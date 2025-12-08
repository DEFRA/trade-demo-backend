package uk.gov.defra.cdp.trade.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;

@Slf4j
@RequestMapping("/ipaffs")
@RestController
public class SubmitController {
    
    private final AwsConfig awsConfig;
    
    public SubmitController(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
    }

    @GetMapping("/submit")
    public ResponseEntity<String> submit() {
        log.info("SUBMIT /notifications");

        String token = awsConfig.webIdentityToken();

        return ResponseEntity.ok(token);
    }

}
