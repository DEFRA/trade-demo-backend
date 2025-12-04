package uk.gov.defra.cdp.trade.demo.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import uk.gov.defra.cdp.trade.demo.service.CognitoTokenService;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for BearerTokenInterceptor.
 */
@ExtendWith(MockitoExtension.class)
class BearerTokenInterceptorTest {

    @Mock
    private CognitoTokenService tokenService;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private BearerTokenInterceptor interceptor;
    private HttpHeaders headers;

    @BeforeEach
    void setUp() {
        interceptor = new BearerTokenInterceptor(tokenService);
        headers = new HttpHeaders();
    }

    @Test
    void shouldAddAuthorizationHeaderWithBearerToken() throws IOException {
        // Given
        String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI()).thenReturn(URI.create("https://api.example.com/notifications"));
        when(tokenService.getToken()).thenReturn(token);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        // When
        interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + token);
        verify(tokenService).getToken();
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldContinueWithoutTokenWhenTokenServiceThrowsException() throws IOException {
        // Given
        lenient().when(request.getHeaders()).thenReturn(headers);
        lenient().when(request.getURI()).thenReturn(URI.create("https://api.example.com/notifications"));
        when(tokenService.getToken()).thenThrow(new RuntimeException("STS unavailable"));
        when(execution.execute(request, new byte[0])).thenReturn(response);

        // When
        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        assertThat(result).isEqualTo(response);
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldNotAddHeaderWhenTokenIsNull() throws IOException {
        // Given
        lenient().when(request.getHeaders()).thenReturn(headers);
        lenient().when(request.getURI()).thenReturn(URI.create("https://api.example.com/notifications"));
        when(tokenService.getToken()).thenReturn(null);
        when(execution.execute(request, new byte[0])).thenReturn(response);

        // When
        interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    void shouldNotAddHeaderWhenTokenIsBlank() throws IOException {
        // Given
        lenient().when(request.getHeaders()).thenReturn(headers);
        lenient().when(request.getURI()).thenReturn(URI.create("https://api.example.com/notifications"));
        when(tokenService.getToken()).thenReturn("   ");
        when(execution.execute(request, new byte[0])).thenReturn(response);

        // When
        interceptor.intercept(request, new byte[0], execution);

        // Then
        assertThat(headers.getFirst(HttpHeaders.AUTHORIZATION)).isNull();
        verify(execution).execute(request, new byte[0]);
    }
}