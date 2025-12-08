package uk.gov.defra.cdp.trade.demo.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;


/**
 * 
 *   Only for testing purpose -- To be deleted
 * 
 * */

@Slf4j
@RequestMapping("/ipaffs")
@RestController
@AllArgsConstructor
public class TokenTestController {
    
    private final AwsConfig awsConfig;
    
    @GetMapping("/token")
    public ResponseEntity<String> token() {
        log.info("New token request /token");

        String token = awsConfig.getWebIdentityToken();

        return ResponseEntity.ok(token);
    }

}
