package uk.gov.defra.cdp.trade.demo.configuration;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO Remove..  This is only for debugging comms with IPAFFS.
 * This only emits the OpenFeign request/response to the logs.
 * NONE - No logging (DEFAULT).
 * BASIC - Log only the request method and URL and the response status code and execution time.
 * HEADERS - Log the basic information along with request and response headers.
 * FULL - Log the headers, body, and metadata for both requests and responses.
 */
@Configuration
public class FeignLoggingConfig {
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
