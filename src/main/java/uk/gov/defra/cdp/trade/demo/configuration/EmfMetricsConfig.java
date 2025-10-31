package uk.gov.defra.cdp.trade.demo.configuration;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.core.client.config.ClientAsyncConfiguration;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.DefaultEnvironment;
import software.amazon.cloudwatchlogs.emf.environment.Environment;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for AWS Embedded Metrics Format (EMF).
 *
 * <p>Validates EMF configuration and ensures graceful shutdown to flush buffered metrics.
 *
 * <p>IMPORTANT: Environment variables must be set BEFORE JVM starts (e.g., in ECS task definition).
 * The EMF library reads configuration at static initialization time. Using System.setProperty()
 * in @PostConstruct is too late - the library has already initialized by then.
 *
 * <p>ENVIRONMENT VARIABLES (must be set by CDP platform): - AWS_EMF_ENABLED: Enable/disable EMF
 * (default: false) - AWS_EMF_ENVIRONMENT: EMF output mode (default: Local for dev, auto-detect ECS
 * on CDP) - AWS_EMF_NAMESPACE: CloudWatch namespace (required if enabled, e.g.,
 * "trade-demo-backend") - AWS_EMF_SERVICE_NAME: Service identification (optional, default:
 * trade-demo-backend) - AWS_EMF_SERVICE_TYPE: Service type (optional, default: SpringBootApp)
 *
 * <p>ACTIVATION: Only active when AWS_EMF_ENABLED=true via @ConditionalOnProperty.
 *
 * <p>VALIDATION: Validates AWS_EMF_NAMESPACE is set at startup (fail-fast).
 *
 * <p>HOW IT WORKS ON CDP: 1. CDP sets environment variables before JVM starts 2. EMF library reads
 * them at static initialization 3. EMF auto-detects ECS environment → connects to CloudWatch Agent
 * sidecar on port 25888 4. CloudWatch Agent ships metrics to CloudWatch Metrics 5. Grafana queries
 * CloudWatch Metrics
 */
@Configuration
@Slf4j
@ConditionalOnProperty(value = "aws.emf.enabled", havingValue = "true", matchIfMissing = false)
public class EmfMetricsConfig {

  @Value("${aws.emf.namespace}")
  private String namespace;

  @Value("${management.metrics.export.cloudwatch.step}")
  private Duration step;

  @Value("${management.metrics.export.cloudwatch.enabled}")
  private boolean enabled;

  @Value("${management.metrics.export.cloudwatch.numThreads}")
  private int numThreads;

  @Value("${management.metrics.export.cloudwatch.connectTimeout}")
  private Duration connectTimeout;

  @Value("${management.metrics.export.cloudwatch.readTimeout}")
  private Duration readTimeout;

  @Value("${management.metrics.export.cloudwatch.batchSize}")
  private int batchSize;

  private Environment environment;

    /**
     * This class has been constructed specifically a the micrometer instance appears to be having 
     * difficulty in pulling the values form the configuration.
     * There are some of the entries set in here that are deprecated (step, batchSize, numThreads, 
     * connectTimeout, readTimeout) and no longer read from config but need to be set manually.
     * 
     * @return CloudWatchConfig
     */
  @Bean
  public CloudWatchConfig cloudWatchConfig() {

    return new CloudWatchConfig() {
      @Override
      public String get(String key) {
        return "";
      }

      @Override
      public Duration step() {
        return step;
      }

      @Override
      public boolean enabled() {
        return enabled;
      }

      @Override
      public int numThreads() {
        return numThreads;
      }

      @Override
      public Duration connectTimeout() {
        return connectTimeout;
      }

      @Override
      public Duration readTimeout() {
        return readTimeout;
      }

      @Override
      public String namespace() {
        return namespace;
      }

      @Override
      public int batchSize() {
        return batchSize;
      }
    };
  }

  @Bean
  MeterRegistry meterRegistry(CloudWatchConfig cloudWatchConfig) {
    return new CloudWatchMeterRegistry(
        cloudWatchConfig, Clock.SYSTEM, CloudWatchAsyncClient.create());
  }
}
