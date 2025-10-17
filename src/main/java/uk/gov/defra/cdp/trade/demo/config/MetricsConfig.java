package uk.gov.defra.cdp.trade.demo.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

/**
 * Production configuration for CloudWatch metrics export.
 * <p>
 * WHY MANUAL CONFIGURATION IS REQUIRED:
 * Spring Boot does NOT provide auto-configuration for CloudWatch metrics.
 * CloudWatch auto-configuration was rejected from Spring Boot (PR #11276) because
 * the AWS SDK evolves too quickly and maintaining compatibility was challenging.
 * <p>
 * CONFIGURATION APPROACH:
 * 1. Creates CloudWatchAsyncClient using AWS DefaultCredentialsProvider
 * 2. Creates CloudWatchConfig reading from application.yml properties
 * 3. Creates CloudWatchMeterRegistry bean that exports metrics to AWS CloudWatch
 * <p>
 * CONDITIONAL ACTIVATION:
 * This configuration is only active when cdp.metrics.enabled=true (ENABLE_METRICS=true).
 * When disabled, Spring Boot uses SimpleMeterRegistry which keeps metrics in memory only.
 * <p>
 * AWS CREDENTIALS:
 * Uses DefaultCredentialsProvider which checks (in order):
 * 1. Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
 * 2. System properties
 * 3. AWS credentials file (~/.aws/credentials)
 * 4. EC2 instance profile / ECS task role (production on CDP)
 * <p>
 * SPRING BOOT BEHAVIOR:
 * Since we create only ONE MeterRegistry bean, Spring Boot injects it directly
 * (not wrapped in CompositeMeterRegistry). This is appropriate for single-backend
 * scenarios and keeps the architecture simple.
 * <p>
 * Reference: <a href="https://github.com/micrometer-metrics/micrometer-docs">Micrometer CloudWatch2</a>
 */
@Configuration
@ConditionalOnProperty(name = "cdp.metrics.enabled", havingValue = "true")
public class MetricsConfig {

    private static final Logger log = LoggerFactory.getLogger(MetricsConfig.class);

    /**
     * Properties for CloudWatch metrics export, bound from application.yml.
     * <p>
     * Maps to: management.metrics.export.cloudwatch.*
     * <p>
     * Validation ensures invalid configuration fails fast at startup rather than runtime.
     */
    @ConfigurationProperties(prefix = "management.metrics.export.cloudwatch")
    @Validated
    public static class CloudWatchProperties {
        @NotBlank(message = "CloudWatch namespace must not be blank")
        private String namespace = "trade-demo-backend";

        @NotBlank(message = "CloudWatch region must not be blank")
        private String region = "eu-west-2";

        @Min(value = 1, message = "Batch size must be at least 1")
        private int batchSize = 20;

        private Duration step = Duration.ofMinutes(1);

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public Duration getStep() {
            return step;
        }

        public void setStep(Duration step) {
            this.step = step;
        }
    }

    /**
     * Provides CloudWatchProperties bean bound from application.yml.
     * <p>
     * Properties are bound via @ConfigurationProperties on the class definition.
     */
    @Bean
    public CloudWatchProperties cloudWatchProperties() {
        return new CloudWatchProperties();
    }

    /**
     * Provides CloudWatchAsyncClient configured with production AWS credentials.
     * <p>
     * Uses DefaultCredentialsProvider which automatically discovers credentials from:
     * - Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)
     * - AWS credentials file (~/.aws/credentials)
     * - EC2 instance profile / ECS task role (production on CDP)
     * <p>
     * The region is configured from application.yml (management.metrics.export.cloudwatch.region).
     * <p>
     * IMPORTANT: The destroyMethod="close" ensures the client is properly closed on application
     * shutdown, preventing resource leaks as required by AWS SDK v2 documentation.
     */
    @Bean(destroyMethod = "close")
    public CloudWatchAsyncClient cloudWatchAsyncClient(CloudWatchProperties properties) {
        Region awsRegion = Region.of(properties.getRegion());
        log.info("Creating CloudWatchAsyncClient for region: {}", awsRegion);

        return CloudWatchAsyncClient.builder()
            .region(awsRegion)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    /**
     * Provides CloudWatchConfig for the meter registry.
     * <p>
     * Configuration tells Micrometer:
     * - namespace: CloudWatch namespace for metrics (appears in AWS console)
     * - batchSize: Number of metrics to send in one request
     * - step: How often to publish metrics (default: 1 minute)
     * <p>
     * Values are read from application.yml via CloudWatchProperties.
     */
    @Bean
    public CloudWatchConfig cloudWatchConfig(CloudWatchProperties properties) {
        return new CloudWatchConfig() {
            @Override
            public String get(String key) {
                return null; // Use defaults for all other settings
            }

            @Override
            public String namespace() {
                return properties.getNamespace();
            }

            @Override
            public int batchSize() {
                return properties.getBatchSize();
            }

            @Override
            public Duration step() {
                return properties.getStep();
            }
        };
    }

    /**
     * Provides CloudWatchMeterRegistry bean that exports metrics to AWS CloudWatch.
     * <p>
     * This is the core bean that enables metrics export. Since this is the ONLY
     * MeterRegistry bean we create, Spring Boot injects it directly without wrapping
     * in CompositeMeterRegistry. This is appropriate for single-backend scenarios.
     * <p>
     * CloudWatchMeterRegistry is a StepMeterRegistry that buffers metrics and publishes
     * them in batches at regular intervals (configured by step, default 1 minute).
     * <p>
     * IMPORTANT: Do NOT use @Primary - not needed for single registry and would
     * interfere if additional registries are added later.
     *
     * @param cloudWatchConfig Configuration for CloudWatch metrics
     * @param clock Clock for timing (provided by Spring Boot auto-configuration)
     * @param cloudWatchAsyncClient AWS CloudWatch client for publishing metrics
     * @return CloudWatchMeterRegistry configured for production use
     */
    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(
            CloudWatchConfig cloudWatchConfig,
            Clock clock,
            CloudWatchAsyncClient cloudWatchAsyncClient) {

        log.info("Creating CloudWatchMeterRegistry with namespace: {}, step: {}",
            cloudWatchConfig.namespace(), cloudWatchConfig.step());

        return new CloudWatchMeterRegistry(cloudWatchConfig, clock, cloudWatchAsyncClient);
    }
}
