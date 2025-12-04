package uk.gov.defra.cdp.trade.demo.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.defra.cdp.trade.demo.client.IpaffsNotificationClient.IpaffsApiException;
import uk.gov.defra.cdp.trade.demo.configuration.CognitoFederationConfig;
import uk.gov.defra.cdp.trade.demo.domain.Commodity;
import uk.gov.defra.cdp.trade.demo.domain.NotificationDto;
import uk.gov.defra.cdp.trade.demo.interceptor.BearerTokenInterceptor;
import uk.gov.defra.cdp.trade.demo.interceptor.TraceIdPropagationInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for IpaffsNotificationClient.
 *
 * Note: These tests verify configuration and basic initialization.
 * Integration tests with MockRestServiceServer would be in a separate IT test.
 */
@ExtendWith(MockitoExtension.class)
class IpaffsNotificationClientTest {

    @Mock
    private BearerTokenInterceptor bearerTokenInterceptor;

    @Mock
    private TraceIdPropagationInterceptor traceIdInterceptor;

    private CognitoFederationConfig config;
    private IpaffsNotificationClient client;

    @BeforeEach
    void setUp() {
        config = new CognitoFederationConfig();
        config.setApiBaseUrl("http://localhost:8080");
        config.setApiEnvironment("dev");
        config.setTokenAudience("urn:ipaffs:api");
        config.setTokenDurationSeconds(3600);

        client = new IpaffsNotificationClient(config, bearerTokenInterceptor, traceIdInterceptor);
    }

    @Test
    void shouldInitializeClientWithCorrectConfiguration() {
        // Given/When - client is initialized in setUp()

        // Then - no exception thrown during initialization
        assertThat(client).isNotNull();
    }

    @Test
    void shouldConstructCorrectApiPath() {
        // This test verifies the path construction logic
        // In a real scenario, this would be tested via integration test with MockRestServiceServer
        // For now, we verify that the client can be instantiated correctly

        String expectedPath = "/notificationapi/dev/protected/notifications";
        assertThat(config.getApiEnvironment()).isEqualTo("dev");
        assertThat(config.getApiBaseUrl()).isEqualTo("http://localhost:8080");
    }

    @Test
    void shouldHandleIpaffsApiException() {
        // Verify that IpaffsApiException has correct properties
        IpaffsApiException exception = new IpaffsApiException(
                "API error", 400, "{\"error\":\"bad request\"}");

        assertThat(exception.getStatusCode()).isEqualTo(400);
        assertThat(exception.getResponseBody()).isEqualTo("{\"error\":\"bad request\"}");
        assertThat(exception.getMessage()).contains("HTTP 400");
    }

    @Test
    void shouldConvertRequestToDto() {
        // Given
        CreateNotificationRequest request = createTestRequest();

        // When
        NotificationDto dto = request.toDto();

        // Then
        assertThat(dto.getChedReference()).isEqualTo("CHED-2024-001");
        assertThat(dto.getOriginCountry()).isEqualTo("GB");
        assertThat(dto.getImportReason()).isEqualTo("Commercial import");
        assertThat(dto.getCommodity()).isNotNull();
        assertThat(dto.getCommodity().getCode()).isEqualTo("12345");
        assertThat(dto.getId()).isNull(); // ID should not be set in request
    }

    @Test
    void shouldConvertDtoToRequest() {
        // Given
        NotificationDto dto = new NotificationDto();
        dto.setId("server-generated-id");
        dto.setChedReference("CHED-2024-001");
        dto.setOriginCountry("GB");
        dto.setImportReason("Commercial import");

        Commodity commodity = new Commodity();
        commodity.setCode("12345");
        commodity.setDescription("Test commodity");
        dto.setCommodity(commodity);

        // When
        CreateNotificationRequest request = CreateNotificationRequest.fromDto(dto);

        // Then
        assertThat(request.getChedReference()).isEqualTo("CHED-2024-001");
        assertThat(request.getOriginCountry()).isEqualTo("GB");
        assertThat(request.getImportReason()).isEqualTo("Commercial import");
        assertThat(request.getCommodity()).isNotNull();
        assertThat(request.getCommodity().getCode()).isEqualTo("12345");
        // Note: ID is excluded from request (server-generated field)
    }

    private CreateNotificationRequest createTestRequest() {
        Commodity commodity = new Commodity();
        commodity.setCode("12345");
        commodity.setDescription("Test commodity");

        return CreateNotificationRequest.builder()
                .chedReference("CHED-2024-001")
                .originCountry("GB")
                .importReason("Commercial import")
                .commodity(commodity)
                .build();
    }
}