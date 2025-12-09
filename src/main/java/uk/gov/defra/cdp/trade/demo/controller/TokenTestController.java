package uk.gov.defra.cdp.trade.demo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;

@Slf4j
@RequestMapping("/ipaffs")
@RestController
@AllArgsConstructor
public class TokenTestController {
    
    private final AwsConfig awsConfig;
    
    private final WebClient webClient;
    
    
    @GetMapping("/submit")
    public Mono<ResponseEntity<String>> submit() {
        log.info("SUBMIT /notifications");

        return webClient.get()
            .uri("https://httpbin.org/get")
            .retrieve()
            .bodyToMono(String.class)
            .map(ResponseEntity::ok);
    }

    @GetMapping("/token")
    public ResponseEntity<String> token() {
        log.info("New token request /token");

        String token = awsConfig.getWebIdentityToken();

        return ResponseEntity.ok(token);
    }

}
