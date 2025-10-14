package uk.gov.defra.cdp.trade.demo.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

/**
 * Configuration for structured logging with ECS (Elastic Common Schema) format.
 * Sets up service version in MDC for inclusion in all log entries.
 */
@Configuration
public class LoggingConfig {

    private static final String MDC_SERVICE_VERSION = "service.version";

    @Value("${SERVICE_VERSION:}")
    private String serviceVersionEnv;

    @Value("${spring.application.version:}")
    private String springApplicationVersion;

    private final Optional<BuildProperties> buildProperties;

    public LoggingConfig(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @PostConstruct
    public void setupServiceVersion() {
        String serviceVersion = determineServiceVersion();

        // Set service version as system property so Logback can access it
        System.setProperty(MDC_SERVICE_VERSION, serviceVersion);
    }

    private String determineServiceVersion() {
        // Priority 1: SERVICE_VERSION environment variable (CDP production)
        if (serviceVersionEnv != null && !serviceVersionEnv.isBlank()) {
            return serviceVersionEnv;
        }

        // Priority 2: spring.application.version property
        if (springApplicationVersion != null && !springApplicationVersion.isBlank()) {
            return springApplicationVersion;
        }

        // Priority 3: Maven build-info.properties
        if (buildProperties.isPresent()) {
            return buildProperties.get().getVersion();
        }

        // Fallback: unknown (for local development)
        return "unknown";
    }

    public String getServiceVersion() {
        return System.getProperty(MDC_SERVICE_VERSION, "unknown");
    }
}
