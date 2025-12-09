package uk.gov.defra.cdp.trade.demo.configuration;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.defra.cdp.trade.demo.service.WebIdentityTokenService;

@Slf4j
@Configuration
public class WebClientConfig {
    
    private static final String INS_CONVERSATION_ID = "INS-ConversationId";
    
    @Bean
    public WebClient webClient(WebIdentityTokenService jwtTokenService) {
        return WebClient.builder()
            .filter((request, next) -> {
                String token = jwtTokenService.getWebIdentityToken();
                ClientRequest newRequest = ClientRequest.from(request)
                    .headers(h -> {
                        if (token != null && !token.isEmpty()) {
                            // Set Authorization bearer
                            h.setBearerAuth(token);
                        }
                        h.set(INS_CONVERSATION_ID, UUID.randomUUID().toString());
                    })
                    .build();
                return next.exchange(newRequest);
            })
            .filter(logRequest())
            .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(req -> {
            log.debug("Outgoing " + req.method() + " " + req.url());
            req.headers().forEach((name, values) ->
                values.forEach(v -> log.debug("  " + name + ": " + v)));
            return Mono.just(req);
        });
    }
}
