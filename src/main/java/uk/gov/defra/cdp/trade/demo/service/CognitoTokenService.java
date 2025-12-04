package uk.gov.defra.cdp.trade.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenRequest;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenResponse;
import software.amazon.awssdk.services.sts.model.StsException;
import uk.gov.defra.cdp.trade.demo.configuration.CognitoFederationConfig;

import java.time.Instant;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for obtaining and caching AWS OIDC tokens via STS GetWebIdentityToken.
 *
 * <p>This service implements the AWS IAM Outbound Identity Federation flow:
 * <ol>
 *   <li>Calls AWS STS GetWebIdentityToken using the application's IAM credentials</li>
 *   <li>Receives a signed JWT (OIDC token) from AWS</li>
 *   <li>Caches the token with thread-safe access</li>
 *   <li>Automatically refreshes tokens before expiry</li>
 * </ol>
 *
 * <p>The token contains claims including:
 * <ul>
 *   <li>issuer: AWS account-specific issuer URL</li>
 *   <li>audience: Configured audience for IPAFFS API</li>
 *   <li>subject: AWS principal ARN</li>
 *   <li>exp: Expiration timestamp</li>
 * </ul>
 * 
 *
 * <p>Thread Safety: This class uses ReentrantReadWriteLock for concurrent token access
 * and refresh operations.
 *
 * @see <a href="https://docs.aws.amazon.com/STS/latest/APIReference/API_GetWebIdentityToken.html">GetWebIdentityToken API</a>
 */
@Service
@Slf4j
public class CognitoTokenService {

    private final StsClient stsClient;
    private final CognitoFederationConfig config;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    public CognitoTokenService(StsClient stsClient, CognitoFederationConfig config) {
        this.stsClient = stsClient;
        this.config = config;
        validateSigningAlgorithm(config.getSigningAlgorithm());
        log.info("CognitoTokenService initialized with audience: {}, duration: {}s, algorithm: {}",
                config.getTokenAudience(), config.getTokenDurationSeconds(), config.getSigningAlgorithm());
    }

    /**
     * Gets a valid OIDC token, refreshing if necessary.
     *
     * <p>This method is thread-safe and will block concurrent refresh attempts
     * to prevent multiple token requests to AWS STS.
     *
     * @return Valid OIDC JWT token
     * @throws RuntimeException if token acquisition fails
     */
    public String getToken() {
        // Fast path: check if current token is valid (read lock)
        lock.readLock().lock();
        try {
            if (isTokenValid()) {
                log.debug("Returning cached OIDC token (expires: {})", tokenExpiry);
                return cachedToken;
            }
        } finally {
            lock.readLock().unlock();
        }

        // Slow path: refresh token (write lock)
        lock.writeLock().lock();
        try {
            // Double-check: another thread may have refreshed while we waited
            if (isTokenValid()) {
                log.debug("Token was refreshed by another thread, using cached token");
                return cachedToken;
            }

            log.info("Refreshing OIDC token from AWS STS (audience: {})", config.getTokenAudience());
            refreshToken();
            return cachedToken;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if the cached token is still valid.
     * Token is considered invalid if it's null or expires within the refresh buffer window.
     *
     * @return true if token is valid and not near expiry
     */
    private boolean isTokenValid() {
        if (cachedToken == null || tokenExpiry == null) {
            return false;
        }

        // Refresh if token expires within the buffer window
        Instant refreshThreshold = Instant.now()
                .plusSeconds(config.getTokenRefreshBufferSeconds());

        boolean valid = tokenExpiry.isAfter(refreshThreshold);

        if (!valid) {
            log.debug("Token requires refresh (expiry: {}, threshold: {})",
                    tokenExpiry, refreshThreshold);
        }

        return valid;
    }

    /**
     * Calls AWS STS GetWebIdentityToken to obtain a new OIDC token.
     * Updates cached token and expiry time.
     *
     * <p>Caller must hold write lock.
     *
     * @throws RuntimeException if STS call fails
     */
    private void refreshToken() {
        try {
            GetWebIdentityTokenRequest request = GetWebIdentityTokenRequest.builder()
                    .audience(java.util.Collections.singletonList(config.getTokenAudience()))
                    .signingAlgorithm(config.getSigningAlgorithm())
                    .durationSeconds(config.getTokenDurationSeconds())
                    .build();

            log.debug("Calling STS GetWebIdentityToken API (audience: {}, algorithm: {}, duration: {}s)",
                    config.getTokenAudience(), config.getSigningAlgorithm(), config.getTokenDurationSeconds());

            GetWebIdentityTokenResponse response = stsClient.getWebIdentityToken(request);

            this.cachedToken = response.webIdentityToken();
            this.tokenExpiry = Instant.now().plusSeconds(config.getTokenDurationSeconds());

            log.info("Successfully obtained OIDC token from AWS STS (expires: {})", tokenExpiry);

        } catch (StsException e) {
            log.error("Failed to obtain OIDC token from AWS STS: {} - {}",
                    e.awsErrorDetails().errorCode(),
                    e.awsErrorDetails().errorMessage(), e);
            throw new RuntimeException("Failed to obtain OIDC token from AWS STS", e);
        } catch (Exception e) {
            log.error("Unexpected error obtaining OIDC token from AWS STS", e);
            throw new RuntimeException("Unexpected error obtaining OIDC token", e);
        }
    }

    /**
     * Clears the cached token, forcing a refresh on next request.
     * Useful for testing or manual token invalidation.
     */
    public void invalidateToken() {
        lock.writeLock().lock();
        try {
            log.info("Invalidating cached OIDC token");
            this.cachedToken = null;
            this.tokenExpiry = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Validates that the signing algorithm is supported.
     *
     * @param algorithm The algorithm string to validate
     * @throws IllegalArgumentException if the algorithm is not supported
     */
    private void validateSigningAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Signing algorithm cannot be null or blank");
        }
        if (!algorithm.equals("RS256") && !algorithm.equals("ES384")) {
            throw new IllegalArgumentException(
                    "Invalid signing algorithm: " + algorithm + ". Must be RS256 or ES384");
        }
    }

}