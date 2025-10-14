package uk.gov.defra.cdp.trade.demo.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * CDP platform configuration properties.
 * Reads from 'cdp.*' properties in application.yml which are populated from environment variables.
 */
@Configuration
@ConfigurationProperties(prefix = "cdp")
@Validated
public class CdpConfig {

    @NotEmpty(message = "cdp.environment must be set (ENVIRONMENT env var)")
    private String environment = "local";

    @NotEmpty(message = "cdp.service-version must be set (SERVICE_VERSION env var)")
    private String serviceVersion = "0.0.0-local";

    private TracingConfig tracing = new TracingConfig();
    private ProxyConfig proxy = new ProxyConfig();
    private MetricsConfig metrics = new MetricsConfig();

    // Getters and setters
    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public TracingConfig getTracing() {
        return tracing;
    }

    public void setTracing(TracingConfig tracing) {
        this.tracing = tracing;
    }

    public ProxyConfig getProxy() {
        return proxy;
    }

    public void setProxy(ProxyConfig proxy) {
        this.proxy = proxy;
    }

    public MetricsConfig getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricsConfig metrics) {
        this.metrics = metrics;
    }

    /**
     * Request tracing configuration.
     */
    public static class TracingConfig {
        private String headerName = "x-cdp-request-id";

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }
    }

    /**
     * HTTP proxy configuration for outbound requests.
     */
    public static class ProxyConfig {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    /**
     * CloudWatch metrics configuration.
     */
    public static class MetricsConfig {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
