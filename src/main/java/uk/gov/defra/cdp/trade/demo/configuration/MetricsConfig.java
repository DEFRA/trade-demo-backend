package uk.gov.defra.cdp.trade.demo.configuration;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
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
@Configuration
public class MetricsConfig {

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        log.debug("Creating TimedAspect for {}", registry.getClass().getSimpleName());
        return new TimedAspect(registry);
    }
 
}
