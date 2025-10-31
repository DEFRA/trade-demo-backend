package uk.gov.defra.cdp.trade.demo.configuration;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Micrometer metrics.
 * <p>
 * Micrometer provides standard JVM, HTTP, and database metrics via Spring Boot Actuator.
 * Custom business metrics use AWS EMF (see EmfMetricsConfig and MetricsService).
 * <p>
 * ARCHITECTURE:
 * - Standard metrics (JVM, HTTP, DB): Micrometer via Spring Boot Actuator
 * - Custom business metrics: AWS Embedded Metrics Format (EMF)
 * <p>
 * This configuration only provides a fallback SimpleMeterRegistry when metrics are disabled.
 * When enabled, Spring Boot Actuator auto-configures appropriate registries.
 */
@Slf4j
public class MetricsConfig {

    /**
     * Fallback MeterRegistry when metrics are disabled.
     * <p>
     * When management.metrics.enabled=false, Spring Boot Actuator still needs
     * a MeterRegistry bean to avoid errors. SimpleMeterRegistry is an in-memory
     * registry that discards all metrics.
     * <p>
     * This bean is only created when metrics are explicitly disabled.
     */
    @Bean
    @ConditionalOnProperty(value = "management.metrics.enabled", havingValue = "false")
    public MeterRegistry simpleMeterRegistry() {
        log.info("Metrics disabled - using SimpleMeterRegistry");
        return new SimpleMeterRegistry();
    }
}
