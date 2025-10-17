package uk.gov.defra.cdp.trade.demo.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.DefaultEnvironment;
import software.amazon.cloudwatchlogs.emf.environment.Environment;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for AWS Embedded Metrics Format (EMF).
 * <p>
 * Configures EMF library with namespace and service identification.
 * Ensures graceful shutdown to flush buffered metrics.
 * <p>
 * ENVIRONMENT VARIABLES:
 * - AWS_EMF_ENABLED: Enable/disable EMF (default: false)
 * - AWS_EMF_NAMESPACE: CloudWatch namespace (required if enabled)
 * - AWS_EMF_SERVICE_NAME: Service identification (optional, default: trade-demo-backend)
 * - AWS_EMF_SERVICE_TYPE: Service type (optional, default: SpringBootApp)
 * <p>
 * ACTIVATION:
 * Only active when AWS_EMF_ENABLED=true via @ConditionalOnProperty.
 * <p>
 * VALIDATION:
 * Validates AWS_EMF_NAMESPACE is set at startup (fail-fast).
 */
@Configuration
@ConditionalOnProperty(value = "aws.emf.enabled", havingValue = "true", matchIfMissing = false)
public class EmfMetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(EmfMetricsConfig.class);

    @Value("${aws.emf.namespace:}")
    private String namespace;

    @Value("${aws.emf.service.name:trade-demo-backend}")
    private String serviceName;

    @Value("${aws.emf.service.type:SpringBootApp}")
    private String serviceType;

    private Environment environment;

    @PostConstruct
    public void configureEmf() {
        log.info("Initializing AWS Embedded Metrics Format");
        log.info("EMF namespace: {}", namespace);
        log.info("EMF service: {} ({})", serviceName, serviceType);

        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                "AWS_EMF_NAMESPACE must be set when AWS_EMF_ENABLED=true"
            );
        }

        // Set environment variables that EMF library reads
        // This is the proper way to configure EMF - it reads from environment
        System.setProperty("AWS_EMF_NAMESPACE", namespace);
        System.setProperty("AWS_EMF_SERVICE_NAME", serviceName);
        System.setProperty("AWS_EMF_SERVICE_TYPE", serviceType);

        environment = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());

        log.info("AWS Embedded Metrics Format initialized successfully");
    }

    @PreDestroy
    public void shutdownEmf() {
        if (environment != null) {
            try {
                log.info("Shutting down AWS Embedded Metrics Format");
                environment.getSink().shutdown()
                    .orTimeout(10_000L, TimeUnit.MILLISECONDS);
                log.info("AWS Embedded Metrics Format shutdown complete");
            } catch (Exception e) {
                log.warn("Error shutting down AWS Embedded Metrics Format", e);
            }
        }
    }
}
