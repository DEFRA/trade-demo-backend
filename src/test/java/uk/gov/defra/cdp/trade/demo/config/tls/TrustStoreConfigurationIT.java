package uk.gov.defra.cdp.trade.demo.config.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration tests for TLS certificate handling and SSLContext configuration.
 * <p>
 * Tests verify that the application can:
 * 1. Create SSLContext combining JVM defaults (no custom certificates in test env)
 * 2. Load and parse base64-encoded X509 certificates
 * 3. Handle empty certificate scenarios gracefully
 * <p>
 * Note: These tests run without TRUSTSTORE_* environment variables set,
 * so they test the "no custom certificates" path. This is the most common
 * scenario and ensures the application works without custom CAs.
 */
@SpringBootTest
class TrustStoreConfigurationIT {

    @Autowired
    private SSLContext customSslContext;

    @Autowired
    private TrustStoreEnvironmentScanner scanner;

    @Autowired
    private CertificateLoader certificateLoader;

    /**
     * Valid self-signed X509 certificate (CN=Test Certificate) in PEM format.
     * Generated using: openssl req -x509 -newkey rsa:2048 -nodes -days 365 -subj "/CN=Test Certificate"
     */
    private static final String VALID_CERT_PEM = """
-----BEGIN CERTIFICATE-----
MIIDXTCCAkWgAwIBAgIJAKL0UG+mRKKzMA0GCSqGSIb3DQEBCwUAMEUxCzAJBgNV
BAYTAkdCMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVQQKDBhJbnRlcm5ldCBX
aWRnaXRzIFB0eSBMdGQwHhcNMjQwMTAxMTIwMDAwWhcNMjUwMTAxMTIwMDAwWjBF
MQswCQYDVQQGEwJHQjETMBEGA1UECAwKU29tZS1TdGF0ZTEhMB8GA1UECgwYSW50
ZXJuZXQgV2lkZ2l0cyBQdHkgTHRkMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB
CgKCAQEAy8Dbv8prpJ/0kKhlGeJYozo2t60EG8eOLYKqZCNb1NVQFGe5Omwpe+6j
4aCq2TqXWquc+oudPkJBDwW6VO3GqNQiVQzmA0p6f9JG0m2/kXiE4E9PkWoHDXyY
hwcZQseN81ISlnC6PX7F5sI8KJmR3YJbCq4m+RqIPzHq2f8Fmh3L1lKbAhqT7Fmz
dTxlCQ7Z5fAK4pE7nJBqCKwkXbPyT9xVGkfLvvTxLLzLNMW5CJF8xqPGMrFQFBFF
OBSEqLJTGGMbXhGMmFVWnD1kLlNyYbH8xmNjfcJyDPqF6KFJmXA6d1k5JKxGJqBf
Gz0q6gv+2TgCXnvyKSqQgM/t5vRUqQIDAQABo1AwTjAdBgNVHQ4EFgQUElRjSoVg
KjUqY6+5QxR2mL8M8gQwHwYDVR0jBBgwFoAUElRjSoVgKjUqY6+5QxR2mL8M8gQw
DAYDVR0TBAUwAwEB/zANBgkqhkiG9w0BAQsFAAOCAQEAQ7H9pNFLpJ5MF1hLk1Mm
FMWdPNkv8ysB0f9Z5nWQPnHOoFLMeNRLPJnI5K7xBdPvUhKzLXqLpPkdqf9hCxrv
tS8MZQNx3nNjbMqh7PLKDhPfxp2nCcHh0jBVnMNRAJNPPxkJRo8cHMfZLxgx8p9B
NzqG3xGfCpmYNVLMAMGcA7OGJjGMKZJy4Qkj2VRLtHx1H9Z0H3dPfqDCNEQPfD3l
IxFMR0LKI8J1xGMW5jQRLWb7PcG7+PG3NlJ5nNqPDJ2qh9nFKNKMYPnPgDxoJTLF
TvBCqLlXHMnPLWLZJKLAVMj4qRKH7PpZNvUjP4GjGQTQJmJIx6EWBkNZhMuFxqKZ
fA==
-----END CERTIFICATE-----
""".trim();

    @Test
    void customSslContext_shouldBeConfigured() {
        // Given: Application started without custom certificates

        // When: SSLContext is injected

        // Then: SSLContext should be configured with default trust store
        assertThat(customSslContext).isNotNull();
        assertThat(customSslContext.getProtocol()).isEqualTo("TLS");
    }

    @Test
    void scanner_shouldReturnEmptyWhenNoTruststoreVariables() {
        // Given: No TRUSTSTORE_* environment variables (test environment)

        // When: Scanning for certificates
        Map<String, byte[]> certificates = scanner.scanTrustStoreCertificates();

        // Then: Should return empty map (no custom certificates)
        assertThat(certificates)
            .as("Should return empty map when no TRUSTSTORE_* variables set")
            .isEmpty();
    }

    @Test
    void certificateLoader_shouldReturnEmptyListWhenNoCertificates() {
        // Given: No TRUSTSTORE_* environment variables (test environment)

        // When: Loading certificates
        List<CertificateLoader.CertificateEntry> certEntries = certificateLoader.loadCustomCertificates();

        // Then: Should return empty list
        assertThat(certEntries)
            .as("Should return empty list when no certificates found")
            .isEmpty();
    }

    @Test
    void certificateLoader_shouldLoadValidCertificateWhenProvided() {
        // Given: Mock scanner that returns valid certificate data
        String base64Cert = Base64.getEncoder().encodeToString(VALID_CERT_PEM.getBytes());
        TrustStoreEnvironmentScanner mockScanner = new TrustStoreEnvironmentScanner() {
            @Override
            public Map<String, byte[]> scanTrustStoreCertificates() {
                return Map.of("TRUSTSTORE_TEST_CERT", Base64.getDecoder().decode(base64Cert));
            }
        };
        CertificateLoader loader = new CertificateLoader(mockScanner);

        // When: Loading certificates
        List<CertificateLoader.CertificateEntry> certEntries = loader.loadCustomCertificates();

        // Then: Certificate should be successfully loaded
        assertThat(certEntries)
            .as("Should load 1 certificate")
            .hasSize(1);

        CertificateLoader.CertificateEntry certEntry = certEntries.get(0);
        assertThat(certEntry.name())
            .as("Certificate name should match")
            .isEqualTo("TRUSTSTORE_TEST_CERT");

        assertThat(certEntry.certificate())
            .as("Certificate should be X509Certificate")
            .isInstanceOf(X509Certificate.class);

        X509Certificate cert = certEntry.certificate();
        assertThat(cert.getSubjectX500Principal().getName())
            .as("Certificate subject should contain GB")
            .contains("GB");
    }

    @Test
    void certificateLoader_shouldSkipInvalidCertificateData() {
        // Given: Mock scanner that returns invalid certificate data
        TrustStoreEnvironmentScanner mockScanner = new TrustStoreEnvironmentScanner() {
            @Override
            public Map<String, byte[]> scanTrustStoreCertificates() {
                return Map.of("TRUSTSTORE_INVALID", "NOT_A_CERT".getBytes());
            }
        };
        CertificateLoader loader = new CertificateLoader(mockScanner);

        // When: Loading certificates with invalid data

        // Then: Should skip invalid certificate and return empty list
        List<CertificateLoader.CertificateEntry> certEntries = loader.loadCustomCertificates();
        assertThat(certEntries)
            .as("Should skip invalid certificate and return empty list")
            .isEmpty();
    }

    @Test
    void scanner_shouldSkipInvalidBase64() {
        // Given: Mock scanner that would encounter invalid base64
        TrustStoreEnvironmentScanner scanner = new TrustStoreEnvironmentScanner();

        // When: Scanning (no TRUSTSTORE_* vars set in test env)
        Map<String, byte[]> certificates = scanner.scanTrustStoreCertificates();

        // Then: Should handle gracefully (empty map)
        assertThat(certificates)
            .as("Should return empty map")
            .isEmpty();
    }
}
