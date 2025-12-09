package uk.gov.defra.cdp.trade.demo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenResponse;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;
import uk.gov.defra.cdp.trade.demo.service.WebIdentityTokenService;

@Slf4j
@RequestMapping("/ipaffs")
@RestController
@Profile({"!integration-test & !dev"})
public class SubmitController {
    
    private final WebIdentityTokenService webIdentityTokenService;
    
    private final GetWebIdentityTokenResponse getWebIdentityTokenResponse;
    
    @Autowired
    public SubmitController(
        WebIdentityTokenService webIdentityTokenService, 
        GetWebIdentityTokenResponse getWebIdentityTokenResponse
    ) {
        this.webIdentityTokenService = webIdentityTokenService;
        this.getWebIdentityTokenResponse = getWebIdentityTokenResponse;
    }
    
    @GetMapping("/submit")
    public ResponseEntity<String> submit() {
        log.info("SUBMIT /notifications");

        String token = webIdentityTokenService.getWebIdentityToken();
        return ResponseEntity.ok(token);
    }

    @GetMapping("/token")
    public ResponseEntity<String> token() {

        String token = getWebIdentityTokenResponse.webIdentityToken();
        log.info("New STS token generated...");

        return ResponseEntity.ok(token);
    }

}
