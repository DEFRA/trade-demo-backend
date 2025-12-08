package uk.gov.defra.cdp.trade.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.service.WebIdentityTokenService;

@Slf4j
@RequestMapping("/ipaffs")
@RestController
public class SubmitController {
    
    private final WebIdentityTokenService webIdentityTokenService;
    
    public SubmitController(WebIdentityTokenService webIdentityTokenService) {
        this.webIdentityTokenService = webIdentityTokenService;
    }

    @GetMapping("/submit")
    public ResponseEntity<String> submit() {
        log.info("SUBMIT /notifications");

        String token = webIdentityTokenService.getWebIdentityToken();
        log.info("Using cached STS token: {}", token.substring(0 ,20));

        return ResponseEntity.ok(token);
    }

}
