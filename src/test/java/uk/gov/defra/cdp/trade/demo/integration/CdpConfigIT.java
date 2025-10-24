package uk.gov.defra.cdp.trade.demo.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import uk.gov.defra.cdp.trade.demo.config.CdpConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CDP configuration management.
 * Verifies that @ConfigurationProperties correctly reads from application.yml and environment variables.
 */

class CdpConfigIT extends IntegrationBase {

    @Autowired
    private CdpConfig cdpConfig;

    @Test
    void cdpConfig_shouldLoadDefaultValues() {
        // Verify default values from application.yml
        assertThat(cdpConfig.getEnvironment()).isEqualTo("local");
        assertThat(cdpConfig.getServiceVersion()).isEqualTo("0.0.0-local");
        assertThat(cdpConfig.getTracing().getHeaderName()).isEqualTo("x-cdp-request-id");
        assertThat(cdpConfig.getMetrics().isEnabled()).isFalse();
    }

    @Test
    void cdpConfig_shouldHaveTracingConfig() {
        assertThat(cdpConfig.getTracing()).isNotNull();
        assertThat(cdpConfig.getTracing().getHeaderName()).isNotEmpty();
    }

    @Test
    void cdpConfig_shouldHaveProxyConfig() {
        assertThat(cdpConfig.getProxy()).isNotNull();
    }

    @Test
    void cdpConfig_shouldHaveMetricsConfig() {
        assertThat(cdpConfig.getMetrics()).isNotNull();
        assertThat(cdpConfig.getMetrics().isEnabled()).isFalse();
    }
}
