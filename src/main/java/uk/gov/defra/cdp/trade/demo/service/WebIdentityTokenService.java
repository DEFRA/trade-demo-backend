package uk.gov.defra.cdp.trade.demo.service;

import static java.util.Objects.isNull;

import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;
import uk.gov.defra.cdp.trade.demo.exceptions.TradeDemoBackendException;

@Slf4j
@Service
public class WebIdentityTokenService {

    @Value("${aws.sts.token.audience}")
    private String audience;

    private final AwsConfig awsConfig;

    private final CacheManager cacheManager;

    private static final String CACHE_NAME = "IDENTITY_TOKEN_CACHE";

    private static final String CACHE_KEY = "tradeDemoBackend";

    public WebIdentityTokenService(AwsConfig awsConfig, CacheManager cacheManager) {
        this.awsConfig = awsConfig;
        this.cacheManager = cacheManager;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "'" + CACHE_KEY +  "'")
    public String getWebIdentityToken() {
        try {
            Cache.ValueWrapper cachedToken = cacheManager.getCache(CACHE_NAME).get(CACHE_KEY);

            if (!isNull(cachedToken)) {
                TokenEntry entry = (TokenEntry) cachedToken.get();

                if (isTokenNotExpired(entry.token)) {
                    log.info("Cached token found for audience: {}", audience);
                    return entry.token;
                } else {
                    log.info("Cached token not found for audience: {}", audience);
                    cacheManager.getCache(CACHE_NAME).evict(CACHE_KEY);
                }
            }

            // fetch new token
            log.info("Cache: calling STS getWebIdentityToken");
            return fetchAndCacheToken();
        } catch (Exception e) {
            log.warn("Failed to retrieve web identity token for audience: {}", audience, e);
            throw new TradeDemoBackendException("STS token could not be retrieved");
        }
    }

    public String fetchAndCacheToken() {

        String token = awsConfig.getWebIdentityToken();

        if (!isTokenNotExpired(token)) {
            log.warn("The new Web identity token is invalid or expired");
            throw new TradeDemoBackendException("The new Web identity token is invalid or expired...!");
        }

        Instant expiry = getTokenExpiration(token);
        cacheManager.getCache(CACHE_NAME).put(CACHE_KEY, new TokenEntry(token, expiry));
        log.info("Cached new web identity token for audience: {}", audience);
        return token;
    }

    public boolean isTokenNotExpired(String token) {

        Instant expiry = getTokenExpiration(token);
        Instant now = Instant.now().plusSeconds(60);
        return expiry != null && now.isBefore(expiry);
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
