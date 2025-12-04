package uk.gov.defra.cdp.trade.demo.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

/**
 * Configuration for AWS Security Token Service (STS) client.
 *
 * <p>Provides an STS client configured for AWS IAM Outbound Identity Federation:
 * <ul>
 *   <li>Uses DefaultCredentialsProvider (EC2 instance profile, ECS task role, etc.)</li>
 *   <li>Regional endpoint (not global) as required by GetWebIdentityToken API</li>
 *   <li>Region from AWS_REGION environment variable (provided by CDP platform)</li>
 * </ul>
 *
 * <p>Important: The GetWebIdentityToken API is NOT available on the STS global endpoint.
 * Always use regional endpoints.
 *
 * @see <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_GetWebIdentityToken.html">GetWebIdentityToken API</a>
 */
@Configuration
@Slf4j
public class AwsStsConfig {

    @Value("${aws.region:eu-west-2}")
    private String awsRegion;

    /**
     * Creates an AWS STS client for outbound identity federation.
     *
     * <p>The client uses:
     * <ul>
     *   <li>DefaultCredentialsProvider: Automatically discovers credentials from environment
     *       (IAM role, instance profile, environment variables, etc.)</li>
     *   <li>Regional endpoint: Required for GetWebIdentityToken API</li>
     * </ul>
     *
     * @return Configured STS client
     */
    @Bean
    public StsClient stsClient() {
        log.info("Initializing AWS STS client for region: {}", awsRegion);

        StsClient client = StsClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("AWS STS client initialized successfully");
        return client;
    }
}