package uk.gov.defra.cdp.trade.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for actuator endpoints in development profile.
 * Verifies that debugging endpoints are properly exposed for local development.
 *
 * <p>Note: Uses TestRestTemplate with RANDOM_PORT to test actual HTTP endpoints
 * including actuator endpoints. MockMvc is not suitable for actuator testing
 * as it only tests the Spring MVC layer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
class HealthCheckConfigDevIT {

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void healthEndpoint_isAccessible() {
    ResponseEntity<String> response = restTemplate.getForEntity("/health", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }

  @Test
  void infoEndpoint_isAccessible() {
    // Development: /info should be exposed for debugging
    ResponseEntity<String> response = restTemplate.getForEntity("/info", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isNotNull();
    assertThat(response.getBody())
        .contains("\"name\":\"trade-demo-backend\"")
        .contains("\"version\"")
        .contains("\"environment\"");
  }

  @Test
  void metricsEndpoint_isAccessible() {
    // Development: /metrics should be exposed for debugging
    ResponseEntity<String> response = restTemplate.getForEntity("/metrics", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isNotNull();
    assertThat(response.getBody()).contains("\"names\"");
  }

  @Test
  void envEndpoint_isAccessible() {
    // Development: /env should be exposed for debugging
    ResponseEntity<String> response = restTemplate.getForEntity("/env", String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType()).isNotNull();
    assertThat(response.getBody()).contains("\"propertySources\"");
  }
}