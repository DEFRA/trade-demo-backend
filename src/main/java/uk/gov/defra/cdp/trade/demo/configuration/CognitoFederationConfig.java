package uk.gov.defra.cdp.trade.demo.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for AWS Cognito Outbound Identity Federation.
 *
 * <p>This configuration supports the flow where the CDP app:
 * <ol>
 *   <li>Assumes its IAM role and authenticates with AWS STS</li>
 *   <li>Calls STS GetWebIdentityToken to obtain an OIDC token (JWT)</li>
 *   <li>Presents the JWT as "Authorization: Bearer" to IPAFFS Imports Proxy</li>
 *   <li>Proxy validates the token with AWS Cognito's OIDC discovery endpoints</li>
 * </ol>
 *
 * <p>Required environment variables:
 * <ul>
 *   <li>IPAFFS_API_BASE_URL - Base URL of IPAFFS Imports Proxy</li>
 *   <li>IPAFFS_API_ENVIRONMENT - Environment for API path (dev, test, prod)</li>
 *   <li>IPAFFS_TOKEN_AUDIENCE - Expected audience claim in JWT</li>
 *   <li>IPAFFS_SIGNING_ALGORITHM - JWT signing algorithm (RS256 or ES384)</li>
 *   <li>AWS_REGION - AWS region for STS endpoint (from CDP platform)</li>
 * </ul>
 *
 * @see <a href="https://aws.amazon.com/blogs/aws/simplify-access-to-external-services-using-aws-iam-outbound-identity-federation/">AWS Outbound Identity Federation</a>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "ipaffs")
public class CognitoFederationConfig {

    /**
     * Base URL of the IPAFFS Imports Proxy API.
     * Example: https://ipaffs-proxy.example.com
     */
    private String apiBaseUrl;

    /**
     * Environment name used in API path construction.
     * Path format: /notificationapi/{environment}/protected/notifications
     * Example: dev, test, prod
     */
    private String apiEnvironment;

    /**
     * Token audience claim expected by IPAFFS Imports Proxy.
     * This value must match what the proxy is configured to accept.
     * Example: urn:ipaffs:api
     */
    private String tokenAudience;

    /**
     * Token duration in seconds before expiry.
     * Default: 3600 (1 hour).
     * Tokens will be refreshed before this duration expires.
     */
    private int tokenDurationSeconds = 3600;

    /**
     * HTTP connection timeout in milliseconds.
     * Default: 3000ms (3 seconds).
     */
    private int connectionTimeoutMs = 3000;

    /**
     * HTTP read timeout in milliseconds.
     * Default: 10000ms (10 seconds).
     */
    private int readTimeoutMs = 10000;

    /**
     * Token refresh buffer in seconds.
     * Tokens will be refreshed this many seconds before actual expiry.
     * Default: 300 (5 minutes).
     */
    private int tokenRefreshBufferSeconds = 300;

    /**
     * JWT signing algorithm for GetWebIdentityToken API.
     * Valid values: RS256 (RSA with SHA-256) or ES384 (ECDSA with P-384/SHA-384).
     * Default: RS256.
     *
     * <p>RS256 is recommended for most use cases as it's widely supported.
     * ES384 provides smaller signatures and faster verification but may have
     * limited support in some JWT validation libraries.
     */
    private String signingAlgorithm = "RS256";
}