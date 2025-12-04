package uk.gov.defra.cdp.trade.demo.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;
import uk.gov.defra.cdp.trade.demo.configuration.CognitoFederationConfig;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.interceptor.BearerTokenInterceptor;
import uk.gov.defra.cdp.trade.demo.interceptor.TraceIdPropagationInterceptor;

import java.time.Duration;

/**
 * HTTP client for IPAFFS Imports Proxy API.
 *
 * <p>This client communicates with the IPAFFS Imports Proxy using AWS Cognito OIDC authentication:
 * <ol>
 *   <li>BearerTokenInterceptor obtains OIDC token from AWS STS via CognitoTokenService</li>
 *   <li>Token is added as "Authorization: Bearer {token}" header</li>
 *   <li>TraceIdPropagationInterceptor adds x-cdp-request-id for distributed tracing</li>
 *   <li>Proxy validates token with AWS Cognito OIDC discovery endpoints</li>
 *   <li>Proxy maps token claims to internal role and forwards to Notification Backend</li>
 * </ol>
 *
 * <p>API Endpoints:
 * <ul>
 *   <li>POST /notificationapi/{env}/protected/notifications - Create notification in IPAFFS</li>
 * </ul>
 *
 * @see uk.gov.defra.cdp.trade.demo.service.CognitoTokenService
 * @see uk.gov.defra.cdp.trade.demo.interceptor.BearerTokenInterceptor
 */
@Component
@Slf4j
public class IpaffsNotificationClient {

    private final RestClient restClient;
    private final String environment;

    public IpaffsNotificationClient(
            CognitoFederationConfig config,
            BearerTokenInterceptor bearerTokenInterceptor,
            TraceIdPropagationInterceptor traceIdInterceptor) {

        this.environment = config.getApiEnvironment();

        log.info("Initializing IPAFFS Notification Client - Base URL: {}, Environment: {}",
                config.getApiBaseUrl(), environment);

        this.restClient = RestClient.builder()
                .baseUrl(config.getApiBaseUrl())
                .requestInterceptor(bearerTokenInterceptor)
                .requestInterceptor(traceIdInterceptor)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("IPAFFS Notification Client initialized successfully");
    }

    /**
     * Creates a notification (CHED) in IPAFFS via the Imports Proxy.
     *
     * <p>Path: POST /notificationapi/{env}/protected/notifications
     *
     * <p>Authentication: Bearer token (OIDC JWT from AWS STS) added by BearerTokenInterceptor
     *
     * <p>This method separates the concerns of request construction from execution:
     * <ol>
     *   <li>Accepts a CreateNotificationRequest (API contract)</li>
     *   <li>Converts to NotificationDto for HTTP serialization</li>
     *   <li>Executes the HTTP POST request</li>
     *   <li>Returns NotificationDto with server-assigned ID</li>
     * </ol>
     *
     * @param request The notification creation request
     * @return The created notification with server-assigned ID
     * @throws IpaffsApiException if the API returns an error response
     * @throws RuntimeException if network or other errors occur
     */
    public NotificationDto createNotification(CreateNotificationRequest request) {
        String path = String.format("/notificationapi/%s/protected/notifications", environment);

        log.info("Creating notification in IPAFFS: chedReference={}", request.getChedReference());
        log.debug("POST {} - Request: {}", path, request);

        try {
            NotificationDto response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request.toDto())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, handleApiError())
                    .onStatus(HttpStatusCode::is5xxServerError, handleApiError())
                    .body(NotificationDto.class);

            String id = "n/a";
            if (response != null) {
                id = response.getId();
            }

            String chedReference = "n/a";
            if (response != null) {
                chedReference = response.getChedReference();
            }
            
            log.info("Successfully created notification in IPAFFS: id={}, chedReference={}", id, chedReference);

            return response;

        } catch (IpaffsApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error calling IPAFFS API", e);
            throw new RuntimeException("Failed to create notification in IPAFFS", e);
        }
    }

    private static ErrorHandler handleApiError() {
        return (httpRequest, httpResponse) -> {
            String errorBody = new String(httpResponse.getBody().readAllBytes());
            log.error("IPAFFS API client error ({}): {}", httpResponse.getStatusCode(), errorBody);
            throw new IpaffsApiException(
                "IPAFFS API rejected request",
                httpResponse.getStatusCode().value(),
                errorBody);
        };
    }

    /**
     * Exception thrown when IPAFFS API returns an error response.
     */
    public static class IpaffsApiException extends RuntimeException {
        private final int statusCode;
        private final String responseBody;

        public IpaffsApiException(String message, int statusCode, String responseBody) {
            super(message + " (HTTP " + statusCode + ")");
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getResponseBody() {
            return responseBody;
        }
    }
}