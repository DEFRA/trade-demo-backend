package uk.gov.defra.cdp.trade.demo.interceptor;

import lombok.AllArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import uk.gov.defra.cdp.trade.demo.configuration.CdpConfig;

/**
 * HTTP client interceptor that propagates the x-cdp-request-id trace header to all outbound HTTP
 * requests.
 *
 * <p>This interceptor retrieves the trace ID from the SLF4J MDC context (set by
 * RequestTracingFilter) and adds it as the x-cdp-request-id header to every outbound HTTP call made
 * via RestClient or RestTemplate.
 *
 * <p>CDP Requirement: All outbound HTTP calls must include the x-cdp-request-id header for
 * distributed tracing across service boundaries.
 */
@Component
public class TraceIdPropagationInterceptor implements ClientHttpRequestInterceptor {

  private static final String MDC_TRACE_ID = "trace.id";

  private final CdpConfig cdpConfig;
  
  public TraceIdPropagationInterceptor(CdpConfig cdpConfig) {
      this.cdpConfig = cdpConfig;
  }
  
  @Override
  public ClientHttpResponse intercept(
      
      HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    String traceId = MDC.get(MDC_TRACE_ID);
    if (traceId != null && !traceId.isBlank()) {
      request.getHeaders().set(cdpConfig.getTracingHeaderName(), traceId);
    }
    return execution.execute(request, body);
  }
}
