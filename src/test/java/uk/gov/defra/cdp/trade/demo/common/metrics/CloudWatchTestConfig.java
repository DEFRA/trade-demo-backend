package uk.gov.defra.cdp.trade.demo.common.metrics;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * Test configuration that manually creates CloudWatchMeterRegistry for testing.
 * <p>
 * WHY MANUAL CONFIGURATION:
 * Spring Boot does NOT provide auto-configuration for CloudWatch metrics.
 * CloudWatch auto-configuration was rejected from Spring Boot (PR #11276) because
 * the AWS SDK evolves too quickly. It's available in spring-cloud-aws instead,
 * but we don't use that dependency.
 * <p>
 * CONFIGURATION APPROACH:
 * 1. Creates CloudWatchAsyncClient with dummy AWS credentials
 * 2. Creates CloudWatchConfig with test settings
 * 3. Creates CloudWatchMeterRegistry bean
 * <p>
 * SPRING BOOT BEHAVIOR:
 * Since we create only ONE MeterRegistry bean, Spring Boot injects it directly
 * (not wrapped in CompositeMeterRegistry). This is appropriate for single-backend
 * scenarios and keeps the architecture simple.
 * <p>
 * The dummy credentials allow testing without actual AWS access, but metrics won't
 * actually be published. This verifies the bean wiring is correct.
 * <p>
 * Reference: <a href="https://github.com/micrometer-metrics/micrometer-docs">...</a>
 */
@TestConfiguration
public class CloudWatchTestConfig {

    /**
     * Provides CloudWatchAsyncClient configured with dummy credentials for testing.
     * <p>
     * Uses static credentials provider with fake access key/secret.
     * This allows CloudWatchMeterRegistry to be created without real AWS credentials.
     */
    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient() {
        return CloudWatchAsyncClient.builder()
            .region(Region.EU_WEST_2)
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create("test-access-key", "test-secret-key")
            ))
            .build();
    }

    /**
     * Provides CloudWatchConfig for the meter registry.
     * <p>
     * Configuration tells Micrometer how to format and batch metrics.
     * In tests, we use minimal settings since we're not actually publishing.
     */
    @Bean
    public CloudWatchConfig cloudWatchConfig() {
        return new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Accept defaults
            }

            @Override
            public String namespace() {
                return "trade-demo-backend-test";
            }
        };
    }

    /**
     * Provides CloudWatchMeterRegistry bean.
     * <p>
     * Since this is the ONLY MeterRegistry bean we create, Spring Boot injects it
     * directly without wrapping in CompositeMeterRegistry. This is appropriate for
     * single-backend scenarios.
     * <p>
     * This is the manual equivalent of what spring-cloud-aws auto-configuration does.
     * <p>
     * IMPORTANT: Do NOT use @Primary - not needed for single registry and would
     * interfere if additional registries are added later.
     */
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(
            CloudWatchConfig cloudWatchConfig,
            Clock clock,
            CloudWatchAsyncClient cloudWatchAsyncClient) {
        return new CloudWatchMeterRegistry(cloudWatchConfig, clock, cloudWatchAsyncClient);
    }
}
