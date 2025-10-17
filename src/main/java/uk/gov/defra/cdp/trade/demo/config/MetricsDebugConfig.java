package uk.gov.defra.cdp.trade.demo.config;

import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for debugging metrics setup.
 * <p>
 * Provides detailed logging of MeterRegistry configuration to help diagnose
 * metrics export issues. Only enabled when cdp.metrics.debug=true.
 * <p>
 * Enable in application.yml or environment variables:
 * <pre>
 * cdp:
 *   metrics:
 *     debug: true
 * </pre>
 * Or via environment variable:
 * <pre>
 * CDP_METRICS_DEBUG=true
 * </pre>
 */
@Configuration
public class MetricsDebugConfig {

    private static final Logger logger = LoggerFactory.getLogger(MetricsDebugConfig.class);

    /**
     * CommandLineRunner that logs detailed MeterRegistry configuration on startup.
     * <p>
     * This helps diagnose:
     * - Which MeterRegistry implementations are configured
     * - Whether CloudWatchMeterRegistry is present
     * - Whether metrics are being recorded
     * <p>
     * Only runs when cdp.metrics.debug=true
     */
    @Bean
    @ConditionalOnProperty(name = "cdp.metrics.debug", havingValue = "true", matchIfMissing = false)
    public CommandLineRunner metricsDebugLogger(MeterRegistry meterRegistry) {
        return args -> {
            logger.info("=== MeterRegistry Debug Information ===");
            logger.info("MeterRegistry class: {}", meterRegistry.getClass().getName());

            if (meterRegistry instanceof CompositeMeterRegistry) {
                CompositeMeterRegistry composite = (CompositeMeterRegistry) meterRegistry;
                logger.info("CompositeMeterRegistry contains {} registries:", composite.getRegistries().size());

                composite.getRegistries().forEach(registry -> {
                    logger.info("  - Registry: {}", registry.getClass().getName());

                    if (registry instanceof CloudWatchMeterRegistry) {
                        CloudWatchMeterRegistry cloudWatch = (CloudWatchMeterRegistry) registry;
                        logger.info("    ✓ CloudWatchMeterRegistry detected");
                        logger.info("    Initial metrics count: {}", cloudWatch.getMeters().size());
                    }
                });

                // Check if CloudWatch registry is present
                boolean hasCloudWatch = composite.getRegistries().stream()
                    .anyMatch(r -> r instanceof CloudWatchMeterRegistry);

                if (!hasCloudWatch) {
                    logger.warn("⚠ CloudWatchMeterRegistry NOT found in CompositeMeterRegistry");
                    logger.warn("  Check: management.metrics.export.cloudwatch.enabled=true");
                    logger.warn("  Check: ENABLE_METRICS environment variable");
                }
            } else {
                logger.info("MeterRegistry is not a CompositeMeterRegistry");
            }

            logger.info("Total metrics in registry: {}", meterRegistry.getMeters().size());
            logger.info("=== End MeterRegistry Debug ===");

            // Log AWS configuration
            logger.info("=== AWS Configuration ===");
            logger.info("AWS_REGION env var: {}", System.getenv("AWS_REGION"));
            logger.info("AWS_DEFAULT_REGION env var: {}", System.getenv("AWS_DEFAULT_REGION"));
            logger.info("=== End AWS Configuration ===");
        };
    }
}
