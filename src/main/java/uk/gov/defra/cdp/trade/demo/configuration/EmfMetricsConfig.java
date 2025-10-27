package uk.gov.defra.cdp.trade.demo.configuration;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
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
 * Validates EMF configuration and ensures graceful shutdown to flush buffered metrics.
 * <p>
 * IMPORTANT: Environment variables must be set BEFORE JVM starts (e.g., in ECS task definition).
 * The EMF library reads configuration at static initialization time. Using System.setProperty()
 * in @PostConstruct is too late - the library has already initialized by then.
 * <p>
 * ENVIRONMENT VARIABLES (must be set by CDP platform):
 * - AWS_EMF_ENABLED: Enable/disable EMF (default: false)
 * - AWS_EMF_ENVIRONMENT: EMF output mode (default: Local for dev, auto-detect ECS on CDP)
 * - AWS_EMF_NAMESPACE: CloudWatch namespace (required if enabled, e.g., "trade-demo-backend")
 * - AWS_EMF_SERVICE_NAME: Service identification (optional, default: trade-demo-backend)
 * - AWS_EMF_SERVICE_TYPE: Service type (optional, default: SpringBootApp)
 * <p>
 * ACTIVATION:
 * Only active when AWS_EMF_ENABLED=true via @ConditionalOnProperty.
 * <p>
 * VALIDATION:
 * Validates AWS_EMF_NAMESPACE is set at startup (fail-fast).
 * <p>
 * HOW IT WORKS ON CDP:
 * 1. CDP sets environment variables before JVM starts
 * 2. EMF library reads them at static initialization
 * 3. EMF auto-detects ECS environment → connects to CloudWatch Agent sidecar on port 25888
 * 4. CloudWatch Agent ships metrics to CloudWatch Metrics
 * 5. Grafana queries CloudWatch Metrics
 */
@Slf4j
@Configuration
@ConditionalOnProperty(value = "aws.emf.enabled", havingValue = "true", matchIfMissing = false)
public class EmfMetricsConfig {

    @Value("${aws.emf.namespace:}")
    private String namespace;

    @Value("${aws.emf.environment:Local}")
    private String emfEnvironment;

    @Value("${aws.emf.service.name:trade-demo-backend}")
    private String serviceName;

    @Value("${aws.emf.service.type:SpringBootApp}")
    private String serviceType;

    private Environment environment;

    @PostConstruct
    public void configureEmf() {
        log.info("Validating AWS Embedded Metrics Format configuration");
        log.info("EMF namespace: {}", namespace);
        log.info("EMF environment: {}", emfEnvironment);
        log.info("EMF service: {} ({})", serviceName, serviceType);

        if (namespace == null || namespace.isBlank()) {
            throw new IllegalStateException(
                "AWS_EMF_NAMESPACE must be set when AWS_EMF_ENABLED=true. " +
                "This must be configured as an environment variable before JVM starts."
            );
        }

        // Create environment instance to initialize EMF library
        // Note: EMF library has already read environment variables at static initialization time
        environment = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());

        log.info("AWS Embedded Metrics Format configuration validated successfully");
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
