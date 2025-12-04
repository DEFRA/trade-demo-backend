package uk.gov.defra.cdp.trade.demo.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import uk.gov.defra.cdp.trade.demo.service.CognitoTokenService;

import java.io.IOException;

/**
 * HTTP client interceptor that adds Bearer token authentication to outbound requests.
 *
 * <p>This interceptor obtains an OIDC token from the CognitoTokenService and adds it
 * as an Authorization header in the format: "Bearer {token}"
 *
 * <p>The token is obtained via AWS IAM Outbound Identity Federation:
 * <ol>
 *   <li>CognitoTokenService calls AWS STS GetWebIdentityToken</li>
 *   <li>AWS returns a signed JWT with the application's IAM identity</li>
 *   <li>Token is cached and automatically refreshed before expiry</li>
 *   <li>This interceptor adds the token to every outbound request</li>
 * </ol>
 *
 * <p>Use this interceptor for HTTP clients that need to authenticate with external
 * services using AWS Cognito OIDC tokens (e.g., IPAFFS Imports Proxy).
 */
@Component
@Slf4j
public class BearerTokenInterceptor implements ClientHttpRequestInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final CognitoTokenService tokenService;

    public BearerTokenInterceptor(CognitoTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        try {
            String token = tokenService.getToken();

            if (token != null && !token.isBlank()) {
                String authHeaderValue = BEARER_PREFIX + token;
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, authHeaderValue);
                log.debug("Added Authorization header to request: {} {}",
                        request.getMethod(), request.getURI());
            } else {
                log.warn("No token available for request: {} {}", request.getMethod(), request.getURI());
            }

        } catch (Exception e) {
            log.error("Failed to obtain token for request: {} {}",
                    request.getMethod(), request.getURI(), e);
            // Continue without token - let the API reject the request
        }

        return execution.execute(request, body);
    }
}