package uk.gov.defra.cdp.trade.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenRequest;
import software.amazon.awssdk.services.sts.model.GetWebIdentityTokenResponse;
import software.amazon.awssdk.services.sts.model.StsException;
import uk.gov.defra.cdp.trade.demo.configuration.CognitoFederationConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CognitoTokenService.
 */
@ExtendWith(MockitoExtension.class)
class CognitoTokenServiceTest {

    @Mock
    private StsClient stsClient;

    private CognitoFederationConfig config;
    private CognitoTokenService tokenService;

    @BeforeEach
    void setUp() {
        config = new CognitoFederationConfig();
        config.setTokenAudience("urn:ipaffs:api");
        config.setTokenDurationSeconds(3600);
        config.setTokenRefreshBufferSeconds(300);
        config.setSigningAlgorithm("RS256");

        tokenService = new CognitoTokenService(stsClient, config);
    }

    @Test
    void shouldObtainTokenFromStsOnFirstCall() {
        // Given
        String expectedToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        GetWebIdentityTokenResponse response = GetWebIdentityTokenResponse.builder()
                .webIdentityToken(expectedToken)
                .build();

        when(stsClient.getWebIdentityToken(any(GetWebIdentityTokenRequest.class)))
                .thenReturn(response);

        // When
        String token = tokenService.getToken();

        // Then
        assertThat(token).isEqualTo(expectedToken);
        verify(stsClient, times(1)).getWebIdentityToken(any(GetWebIdentityTokenRequest.class));
    }

    @Test
    void shouldReturnCachedTokenOnSubsequentCalls() {
        // Given
        String expectedToken = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        GetWebIdentityTokenResponse response = GetWebIdentityTokenResponse.builder()
                .webIdentityToken(expectedToken)
                .build();

        when(stsClient.getWebIdentityToken(any(GetWebIdentityTokenRequest.class)))
                .thenReturn(response);

        // When
        String token1 = tokenService.getToken();
        String token2 = tokenService.getToken();
        String token3 = tokenService.getToken();

        // Then
        assertThat(token1).isEqualTo(expectedToken);
        assertThat(token2).isEqualTo(expectedToken);
        assertThat(token3).isEqualTo(expectedToken);
        // Should only call STS once - subsequent calls use cached token
        verify(stsClient, times(1)).getWebIdentityToken(any(GetWebIdentityTokenRequest.class));
    }

    @Test
    void shouldRefreshTokenAfterInvalidation() {
        // Given
        String token1 = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.token1";
        String token2 = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.token2";

        GetWebIdentityTokenResponse response1 = GetWebIdentityTokenResponse.builder()
                .webIdentityToken(token1)
                .build();

        GetWebIdentityTokenResponse response2 = GetWebIdentityTokenResponse.builder()
                .webIdentityToken(token2)
                .build();

        when(stsClient.getWebIdentityToken(any(GetWebIdentityTokenRequest.class)))
                .thenReturn(response1)
                .thenReturn(response2);

        // When
        String firstToken = tokenService.getToken();
        tokenService.invalidateToken();
        String secondToken = tokenService.getToken();

        // Then
        assertThat(firstToken).isEqualTo(token1);
        assertThat(secondToken).isEqualTo(token2);
        verify(stsClient, times(2)).getWebIdentityToken(any(GetWebIdentityTokenRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenStsCallFails() {
        // Given
        StsException stsException = (StsException) StsException.builder()
                .message("Access denied")
                .awsErrorDetails(software.amazon.awssdk.awscore.exception.AwsErrorDetails.builder()
                        .errorCode("AccessDenied")
                        .errorMessage("Access denied")
                        .build())
                .build();

        when(stsClient.getWebIdentityToken(any(GetWebIdentityTokenRequest.class)))
                .thenThrow(stsException);

        // When/Then
        assertThatThrownBy(() -> tokenService.getToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to obtain OIDC token from AWS STS");

        verify(stsClient, times(1)).getWebIdentityToken(any(GetWebIdentityTokenRequest.class));
    }

    @Test
    void shouldPassCorrectParametersToSts() {
        // Given
        GetWebIdentityTokenResponse response = GetWebIdentityTokenResponse.builder()
                .webIdentityToken("test.token")
                .build();

        when(stsClient.getWebIdentityToken(any(GetWebIdentityTokenRequest.class)))
                .thenReturn(response);

        // When
        tokenService.getToken();

        // Then
        verify(stsClient).getWebIdentityToken(argThat((GetWebIdentityTokenRequest request) ->
                request.audience() != null && request.audience().contains("urn:ipaffs:api") &&
                request.durationSeconds() != null && request.durationSeconds().equals(3600) &&
                request.signingAlgorithm() != null && request.signingAlgorithm().equals("RS256")
        ));
    }

    @Test
    void shouldThrowExceptionForInvalidSigningAlgorithm() {
        // Given
        config.setSigningAlgorithm("INVALID");

        // When/Then
        assertThatThrownBy(() -> new CognitoTokenService(stsClient, config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid signing algorithm: INVALID");
    }
}