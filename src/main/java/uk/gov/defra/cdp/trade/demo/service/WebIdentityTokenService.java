package uk.gov.defra.cdp.trade.demo.service;

import static java.util.Objects.isNull;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
    
    private static final String CACHE_NAME = "webIdentityToken";
    
    public WebIdentityTokenService(AwsConfig awsConfig) {
        this.awsConfig = awsConfig;
    }
    
    @PostConstruct
    public void init() {
        log.info("Initializing Cache for WebIdentityTokenService: {}", CACHE_NAME);
        this.tokenCache = new ConcurrentMapCacheManager().getCache(CACHE_NAME);
        if (this.tokenCache == null) {
            throw new IllegalStateException("Cache not found: {}" + CACHE_NAME);
        }
        log.info("Successfully initialized cache for WebIdentityTokenService: {}", CACHE_NAME);
    }
    
    @Cacheable(cacheNames = CACHE_NAME, key = "'default'")
    public String getWebIdentityToken() {
        try {
            Cache.ValueWrapper cachedToken = tokenCache.get("default");
            
            if (cachedToken != null) {
                TokenEntry entry = (TokenEntry) cachedToken.get();
                
                if (isTokenValid(entry.token, entry.expiry)) {
                    log.debug("Cached token found for audience {}", audience);
                    return entry.token;
                } else {
                    log.debug("Cached token not found for audience {}", audience);
                    tokenCache.evict("default");
                }
            }
            
            // fetch new token
            return fetchAndCacheToken();
            
        } catch (Exception e) {
            log.warn("Failed to retrieve web identity token for audience: {}", audience, e);
            throw new TradeDemoBackendException("STS token could not be retrieved");
        }
    }
    
    @CacheEvict(cacheNames = CACHE_NAME, key="'default'")
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
        tokenCache.put("default", new TokenEntry(token, expiry));
        log.info("Cached web identity token for audience {}", CACHE_NAME);
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
