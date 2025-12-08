package uk.gov.defra.cdp.trade.demo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;
import uk.gov.defra.cdp.trade.demo.service.WebIdentityTokenService;

@Slf4j
@RequestMapping("/ipaffs")
@RestController
@AllArgsConstructor
public class SubmitController {
    
    private final WebIdentityTokenService webIdentityTokenService;
    
    private final AwsConfig awsConfig;
    
    @GetMapping("/submit")
    public ResponseEntity<String> submit() {
        log.info("SUBMIT /notifications");

        String token = webIdentityTokenService.getWebIdentityToken();
        return ResponseEntity.ok(token);
    }

    @GetMapping("/token")
    public ResponseEntity<String> cachedToken() {
        log.info("Cognito token...");

        String token = awsConfig.webIdentityToken();
        log.info("Nw STS token generated...");

        return ResponseEntity.ok(token);
    }

}
