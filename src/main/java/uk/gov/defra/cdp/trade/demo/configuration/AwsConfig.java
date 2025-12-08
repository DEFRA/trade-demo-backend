package uk.gov.defra.cdp.trade.demo.configuration;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient;
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
    
    
    @Bean
    @Primary
    public StsClient stsClient() {
        
        return StsClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
        
    }
    
    @Bean
    public CognitoIdentityClient cognitoIdentityClient() {
        return CognitoIdentityClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();
    }
    
    @Bean
    @Profile({"!integration-test"})
    public String webIdentityToken() {
        try(StsClient stsClient = stsClient()) {

            GetWebIdentityTokenRequest request = GetWebIdentityTokenRequest.builder()
                .audience("trade-demo-backend")
                .signingAlgorithm("RS256")
                .durationSeconds(180)
                .build();
            GetWebIdentityTokenResponse response = stsClient.getWebIdentityToken(request);
            
            log.info("WebIdentityToken: {}", response.webIdentityToken());
        
            return response.webIdentityToken();
        } catch (StsException ex) {
            throw new NotFoundException("Sts connection error: " +  ex.getMessage());
        }
    }
}
