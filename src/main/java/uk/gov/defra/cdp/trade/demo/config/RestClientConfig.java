package uk.gov.defra.cdp.trade.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import uk.gov.defra.cdp.trade.demo.interceptor.TraceIdPropagationInterceptor;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuration for HTTP clients with custom SSL/TLS certificates and trace ID propagation.
 *
 * <p>Provides both RestClient (modern, Spring Boot 3.2+) and RestTemplate (legacy) configured
 * with:
 *
 * <ul>
 *   <li>Custom SSLContext (Default JVM trust store + CDP TRUSTSTORE_* certificates)
 *   <li>TraceIdPropagationInterceptor (propagates x-cdp-request-id to all outbound calls)
 * </ul>
 *
 * <p>This configuration uses Java's built-in HttpClient (JDK 11+) with custom SSL, requiring zero
 * external dependencies (no Apache HttpClient needed).
 *
 * <p>This ensures that outbound HTTP requests to CDP internal services with custom CA certificates
 * are trusted and that distributed tracing works across service boundaries.
 */
@Configuration
@Slf4j
public class RestClientConfig {

  private static final Logger logger = LoggerFactory.getLogger(RestClientConfig.class);
  private final ClientHttpRequestFactory customRequestFactory;
  private final TraceIdPropagationInterceptor traceIdInterceptor;
  
  public RestClientConfig(
      SslBundles sslBundles, TraceIdPropagationInterceptor traceIdInterceptor) {
    logger.info("Configuring HTTP clients with custom SSL context and trace ID propagation");

      // Create Java HttpClient with custom SSL context
      
    HttpClient.Builder builder = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10));
    
    if (sslBundles != null) {
        builder.sslContext(sslBundles.getBundle("client").createSslContext());
        log.info("Creating the SSLContext");
    }
    HttpClient httpClient = builder.build();
    
    // Create request factory using JDK HttpClient
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(30));

    this.customRequestFactory = factory;
    this.traceIdInterceptor = traceIdInterceptor;
    logger.info("HTTP clients configured with custom SSL context and trace ID propagation");
  }

  /**
   * Creates a RestClient.Builder configured with custom SSL context and trace ID propagation.
   * RestClient is the modern, recommended HTTP client for Spring Boot 3.2+.
   *
   * <p>Usage:
   *
   * <pre>
   * {@code
   * @Service
   * public class MyService {
   *     private final RestClient restClient;
   *
   *     public MyService(RestClient.Builder builder) {
   *         this.restClient = builder
   *             .baseUrl("https://my-service.example.com")
   *             .build();
   *     }
   * }
   * }
   * </pre>
   */
  @Bean
  public RestClient.Builder restClientBuilder() {
    logger.debug("Creating RestClient.Builder with custom SSL context and trace ID propagation");
    return RestClient.builder()
        .requestFactory(customRequestFactory)
        .requestInterceptor(traceIdInterceptor);
  }

  /**
   * Default RestClient bean configured with custom SSL context and trace ID propagation.
   * Applications can inject this bean directly.
   */
  @Bean
  public RestClient restClient(RestClient.Builder builder) {
    return builder.build();
  }

  /**
   * Creates a RestTemplateBuilder configured with custom SSL context and trace ID propagation.
   * RestTemplate is the legacy HTTP client, maintained for backward compatibility.
   *
   * <p>Use this builder to create RestTemplate instances that trust CDP certificates and propagate
   * trace IDs.
   */
  @Bean
  public RestTemplateBuilder restTemplateBuilder() {
    logger.debug("Creating RestTemplateBuilder with custom SSL context and trace ID propagation");
    return new RestTemplateBuilder()
        .requestFactory(() -> customRequestFactory)
        .interceptors(traceIdInterceptor);
  }

  /**
   * Default RestTemplate bean configured with custom SSL context and trace ID propagation.
   * Applications can inject this bean or use the RestTemplateBuilder.
   */
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }
}
