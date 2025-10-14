package uk.gov.defra.cdp.trade.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CDP configuration management.
 * Verifies that @ConfigurationProperties correctly reads from application.yml and environment variables.
 */
@SpringBootTest
class CdpConfigIT {

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

    /**
     * Test configuration override via TestPropertySource.
     * Verifies that environment variables can override default values.
     */
    @SpringBootTest
    @TestPropertySource(properties = {
            "cdp.environment=test",
            "cdp.service-version=1.2.3",
            "cdp.tracing.header-name=x-custom-trace-id",
            "cdp.proxy.url=http://proxy.example.com:8080",
            "cdp.metrics.enabled=true"
    })
    static class ConfigurationOverrideIT {

        @Autowired
        private CdpConfig cdpConfig;

        @Test
        void cdpConfig_shouldOverrideWithEnvironmentVariables() {
            assertThat(cdpConfig.getEnvironment()).isEqualTo("test");
            assertThat(cdpConfig.getServiceVersion()).isEqualTo("1.2.3");
            assertThat(cdpConfig.getTracing().getHeaderName()).isEqualTo("x-custom-trace-id");
            assertThat(cdpConfig.getProxy().getUrl()).isEqualTo("http://proxy.example.com:8080");
            assertThat(cdpConfig.getMetrics().isEnabled()).isTrue();
        }
    }
}
