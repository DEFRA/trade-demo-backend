package uk.gov.defra.cdp.trade.demo.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenRequest;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenResponse;
import software.amazon.awssdk.services.sts.model.StsException;
import uk.gov.defra.cdp.trade.demo.exceptions.NotFoundException;

@Slf4j
@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;
    
    @Value("${aws.sts.token.audience}")
    private String audience;

    @Value("${aws.sts.token.expiration}")
    private Integer expiration; 
    
    
    @Bean
    @Primary
    public StsClient stsClient() {
        
        return StsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
        
    }
    
    @Bean
    @Profile({"!integration-test"})
    public String webIdentityToken() {
        try(StsClient stsClient = stsClient()) {

            GetWebIdentityTokenRequest request = GetWebIdentityTokenRequest.builder()
                .audience(audience)
                .signingAlgorithm("RS256")
                .durationSeconds(expiration)
                .build();
            GetWebIdentityTokenResponse response = stsClient.getWebIdentityToken(request);

            log.info("WebIdentityToken: {}", response.webIdentityToken());

            return response.webIdentityToken();
        } catch (StsException ex) {
            throw new NotFoundException("Sts connection error: " +  ex.getMessage());
        }
        
    }
}
