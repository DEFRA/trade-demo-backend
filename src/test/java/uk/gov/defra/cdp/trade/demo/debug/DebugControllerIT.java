package uk.gov.defra.cdp.trade.demo.debug;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for DebugController.
 *
 * Verifies that debug endpoints are properly wired and functional.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class DebugControllerIT {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.14");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.data.mongodb.database", () -> "test-db");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldRunErrorExperimentsSuccessfully() {
        // When: Running the error experiments
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/debug/run-error-experiments",
            null,
            Map.class
        );

        // Then: Should return 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // And: Response should contain experiment metadata
        Map<String, Object> body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertEquals("completed", body.get("status"));
        assertEquals(4, body.get("experiments_run"));
    }

    @Test
    void shouldRunMetricsExperimentsSuccessfully() {
        // When: Running the metrics experiments
        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/debug/run-metrics-experiments",
            null,
            Map.class
        );

        // Then: Should return 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // And: Response should contain experiment metadata
        Map<String, Object> body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertEquals("completed", body.get("status"));
        assertEquals(7, body.get("metrics_emitted"));
    }

    @Test
    void shouldReturnDebugInfo() {
        // When: Getting debug info
        ResponseEntity<Map> response = restTemplate.getForEntity(
            "/debug/info",
            Map.class
        );

        // Then: Should return 200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // And: Response should contain service info
        Map<String, Object> body = response.getBody();
        assertNotNull(body, "Response body should not be null");
        assertTrue(body.containsKey("service"));
        assertTrue(body.containsKey("emf"));
        assertTrue(body.containsKey("logging"));
    }
}
