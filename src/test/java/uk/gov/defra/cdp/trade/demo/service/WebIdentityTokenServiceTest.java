package uk.gov.defra.cdp.trade.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.defra.cdp.trade.demo.configuration.AwsConfig;
import uk.gov.defra.cdp.trade.demo.exceptions.TradeDemoBackendException;

@ExtendWith(MockitoExtension.class)
class WebIdentityTokenServiceTest {

    private final String CACHE_NAME = "IDENTITY_TOKEN_CACHE";

    private final String CACHE_KEY = "tradeDemoBackend";

    @Mock
    private Cache cache;
    @Mock
    private Cache.ValueWrapper valueWrapper;
    @Mock
    private AwsConfig mockAwsConfig;
    @Mock
    private CacheManager mockCacheManager;

    private WebIdentityTokenService.TokenEntry tokenEntry;

    private WebIdentityTokenService webIdentityTokenServiceUnderTest;

    @BeforeEach
    void setUp() throws Exception {
        webIdentityTokenServiceUnderTest = new WebIdentityTokenService(mockAwsConfig,
            mockCacheManager);
        ReflectionTestUtils.setField(webIdentityTokenServiceUnderTest, "audience", "audience");
        tokenEntry =
            new WebIdentityTokenService.TokenEntry(getToken(3600), Instant.now().plusSeconds(3600));
    }

    @Test
    void test_GetWebIdentityToken() throws Exception {
        // Setup
        when(mockCacheManager.getCache(CACHE_NAME)).thenReturn(cache);
        when(valueWrapper.get()).thenReturn(tokenEntry);
        when(cache.get(CACHE_KEY)).thenReturn(valueWrapper);

        // Run the test
        final String result = webIdentityTokenServiceUnderTest.getWebIdentityToken();

        // Verify the results
        String expectedToken = tokenEntry.token;
        assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void test_GetWebIdentityToken_when_CacheManager_ReturnsNull() throws Exception {
        // Setup
        when(cache.get(CACHE_KEY)).thenReturn(null);
        when(mockCacheManager.getCache(CACHE_NAME)).thenReturn(cache);
        when(mockAwsConfig.getWebIdentityToken()).thenReturn(tokenEntry.token);

        // Run the test
        final String result = webIdentityTokenServiceUnderTest.getWebIdentityToken();

        // Verify the results
        String expectedToken = tokenEntry.token;
        assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void test_FetchAndCacheToken() throws Exception {
        // Setup
        when(mockAwsConfig.getWebIdentityToken()).thenReturn(tokenEntry.token);
        when(mockCacheManager.getCache(CACHE_NAME)).thenReturn(cache);

        // Run the test
        final String result = webIdentityTokenServiceUnderTest.fetchAndCacheToken();

        // Verify the results
        String expectedToken = tokenEntry.token;
        assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void test_FetchAndCacheToken_CacheManager_ReturnsNull() throws Exception {
        // Setup
        when(mockAwsConfig.getWebIdentityToken()).thenReturn(tokenEntry.token);
        when(mockCacheManager.getCache(CACHE_NAME)).thenReturn(cache);

        // Run the test
        final String result = webIdentityTokenServiceUnderTest.fetchAndCacheToken();

        // Verify the results
        String expectedToken = tokenEntry.token;
        assertThat(result).isEqualTo(expectedToken);
    }

    @Test
    void test_IsTokenNotExpired() throws Exception {
        String token = getToken(3600);
        assertThat(webIdentityTokenServiceUnderTest.isTokenNotExpired(token)).isTrue();
    }

    @Test
    void test_IsTokenHasExpired() throws Exception {
        String token = getToken(0);
        assertThat(webIdentityTokenServiceUnderTest.isTokenNotExpired(token)).isFalse();
    }

    @Test
    void test_getWebIdentityToken_ThrowsException_WhenCacheManagerReturnsNull() {
        // Setup
        when(mockCacheManager.getCache(CACHE_NAME)).thenReturn(null);

        assertThatThrownBy(() -> webIdentityTokenServiceUnderTest.getWebIdentityToken())
            .isInstanceOf(TradeDemoBackendException.class)
            .hasMessageContaining("STS token could not be retrieved");
    }

    @Test
    void test_FetchAndCacheToken_ThrowsException_WhenTokenHasExpired() throws Exception {
        // Setup
        tokenEntry =
            new WebIdentityTokenService.TokenEntry(getToken(0), Instant.now().plusSeconds(0));
        when(mockAwsConfig.getWebIdentityToken()).thenReturn(tokenEntry.token);

        // Run the test
        assertThatThrownBy(() -> webIdentityTokenServiceUnderTest.fetchAndCacheToken())
            .isInstanceOf(TradeDemoBackendException.class)
            .hasMessageContaining("The new Web identity token is invalid or expired...!");
    }

    private String getToken(long seconds) throws Exception {
        String secret = "0123456789ABCDEF0123456789ABCDEF"; // 32+ chars

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .subject("tradeDemoBackend")
            .issuer("test-issuer")
            .expirationTime(Date.from(Instant.now().plusSeconds(seconds)))
            .claim("scope", "read")
            .build();

        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        SignedJWT jwt = new SignedJWT(header, claims);

        jwt.sign(new MACSigner(secret.getBytes()));

        return jwt.serialize();
    }
}
