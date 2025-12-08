package uk.gov.defra.cdp.trade.demo.service;

import static java.util.Objects.isNull;

import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import java.text.ParseException;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;
import uk.gov.defra.cdp.trade.demo.exceptions.TradeDemoBackendException;

@Slf4j
@Service
public class WebIdentityTokenService {

    @Value("${aws.sts.token.audience}")
    private String audience;

    private Cache tokenCache;

    private final AwsConfig awsConfig;

    private static final String CACHE_NAME = "IDENTITY_TOKEN_CACHE";
    
    private static final String CACHE_KEY = "tradeDemoBackend";

    public WebIdentityTokenService(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing Cache for WebIdentityTokenService: {}", CACHE_NAME);
        this.tokenCache = new ConcurrentMapCacheManager().getCache(CACHE_NAME);
        if (isNull(this.tokenCache)) {
            throw new IllegalStateException("Cache not found: {}" + CACHE_NAME);
        }
        log.info("Successfully initialized cache for WebIdentityTokenService: {}", CACHE_NAME);
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "'tradeDemoBackend'")
    public String getWebIdentityToken() {
        try {
            Cache.ValueWrapper cachedToken = tokenCache.get(CACHE_KEY);

            if (!isNull(cachedToken)) {
                TokenEntry entry = (TokenEntry) cachedToken.get();

                if (isTokenValid(entry.expiry)) {
                    log.info("Cached token found for audience: {}", audience);
                    return entry.token;
                } else {
                    log.info("Cached token not found for audience: {}", audience);
                    evictToken(CACHE_KEY);
                }
            }

            // fetch new token
            return fetchAndCacheToken();
        } catch (Exception e) {
            log.warn("Failed to retrieve web identity token for audience: {}", audience, e);
            throw new TradeDemoBackendException("STS token could not be retrieved");
        }
    }

    @CacheEvict(cacheNames = CACHE_NAME, key = "'tradeDemoBackend'")
    public void evictToken(String audience) {
        log.warn("Evicted web identity token for {}", audience);
    }

    public String fetchAndCacheToken() {

        String token = awsConfig.webIdentityToken();

        if (!isTokenValid(token)) {
            log.warn("Web identity token is invalid or expired");
            throw new TradeDemoBackendException("STS token is invalid or expired...!");
        }

        Instant expiry = getTokenExpiration(token);
        tokenCache.put("cdp-tdb", new TokenEntry(token, expiry));
        log.info("Cached web identity token for audience: {}", CACHE_NAME);
        return token;
    }

    public boolean isTokenValid(String token) {

        Instant expiry = getTokenExpiration(token);
        return expiry != null && !expiry.isBefore(Instant.now());
    }

    public boolean isTokenValid(Instant expiry) {
        return expiry != null && !expiry.isBefore(Instant.now());
    }

    private Instant getTokenExpiration(String token) {

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getExpirationTime().toInstant();
        } catch (ParseException e) {
            throw new TradeDemoBackendException("Error parsing JWT: " + e);
        }
    }

    @AllArgsConstructor
    public static class TokenEntry {

        final String token;
        final Instant expiry;
    }
}
