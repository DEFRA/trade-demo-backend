package uk.gov.defra.cdp.trade.demo.service;

import static java.util.Objects.isNull;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
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

    private static final int EXPIRY_BUFFER_SECONDS = 60;
    
    public WebIdentityTokenService(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
    }
    
    @PostConstruct
    public void init() {
        log.info("Initializing Cache for WebIdentityTokenService: {}", audience);
        this.tokenCache = new ConcurrentMapCacheManager().getCache(audience);
        if (this.tokenCache == null) {
            throw new IllegalStateException("Cache not found: {}" + audience);
        }
        log.info("Successfully initialized cache for WebIdentityTokenService: {}", audience);
    }
    
    @Cacheable(value = "webIdentityToken", key = "#audience", unless = "#result == null")
    public String getWebIdentityToken() {
        try {
            Cache.ValueWrapper cachedToken = tokenCache.get(audience);
            
            if (!isNull(cachedToken)) {
                TokenEntry entry = (TokenEntry) cachedToken.get();
                
                if (isTokenValid(entry.token, entry.expiry)) {
                    log.debug("Cached token found for audience {}", audience);
                    return entry.token;
                } else {
                    log.debug("Cached token not found for audience {}", audience);
                    tokenCache.evict(audience);
                }
            }
            
            // fetch new token
            return fetchAndCacheToken();
            
        } catch (Exception e) {
            log.warn("Failed to retrieve web identity token for audience: {}", audience, e);
            throw new TradeDemoBackendException("STS token could not be retrieved");
        }
    }
    
    @CacheEvict(value = "webIdentityToken", key="#audience")
    public void evictToken(String audience) {
        log.warn("Evicted web identity token for {}", audience);
    }
    
    public String fetchAndCacheToken() {
        
        String token = awsConfig.webIdentityToken();
        
        if (!isTokenValid(token)) {
            log.warn("Web identity token is invalid");
            throw new TradeDemoBackendException("STS token could not be retrieved");
        }
        
        Instant expiry = getTokenExpiration(token);
        tokenCache.put(audience, new TokenEntry(token, expiry));
        log.info("Cached web identity token for audience {}", audience);
        return token;
    }
    
    public boolean isTokenValid(String token) {
        
        try {
            Claims claims = getTokenClaims(token);
            
            Instant expiry = claims.getExpiration().toInstant();
            return Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(expiry);
        } catch (Exception e) {
            log.warn("Failed to validate token", e);
            return false;
        }
    }

    public boolean isTokenValid(String token, Instant expiry) {
        return Instant.now().plusSeconds(EXPIRY_BUFFER_SECONDS).isBefore(expiry);
    }
    
    private Instant getTokenExpiration(String token) {
        
            Claims claims = getTokenClaims(token);

            return claims.getExpiration().toInstant();
    }
    
    private Claims getTokenClaims(String token) {
        return Jwts.parser()
//                .verifyWith(null)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    @AllArgsConstructor
    public static class TokenEntry {
        final String token;
        final Instant expiry;
    }

}
