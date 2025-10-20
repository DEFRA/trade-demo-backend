package uk.gov.defra.cdp.trade.demo.config.tls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * Configures SSL/TLS for the CDP Java Backend Template.
 * <p>
 * Creates a custom SSLContext that combines:
 * 1. Default JVM trust store certificates
 * 2. Custom CDP TRUSTSTORE_* certificates
 * <p>
 * This SSLContext is used by MongoDB client, RestTemplate, WebClient, and any other
 * HTTP/TLS clients in the application.
 * <p>
 * The configuration runs at HIGHEST_PRECEDENCE to ensure the SSLContext is available
 * before other beans (like MongoClient) are created.
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrustStoreConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(TrustStoreConfiguration.class);
    private final CertificateLoader certificateLoader;

    public TrustStoreConfiguration(CertificateLoader certificateLoader) {
        this.certificateLoader = certificateLoader;
    }

    /**
     * Creates a custom SSLContext that includes:
     * 1. Default JVM trust store certificates
     * 2. Custom CDP TRUSTSTORE_* certificates
     *
     * This SSLContext can be used by MongoDB client, RestTemplate, WebClient, etc.
     */
    @Bean
    public SSLContext customSslContext() {
        logger.info("Initializing custom SSL context with CDP certificates");

        try {
            // Load custom certificates
            List<CertificateLoader.CertificateEntry> customCerts =
                certificateLoader.loadCustomCertificates();

            // Create combined trust manager
            X509TrustManager combinedTrustManager = createCombinedTrustManager(customCerts);

            // Initialize SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{combinedTrustManager}, new SecureRandom());

            logger.info("Custom SSL context initialized successfully with {} custom certificate(s)",
                customCerts.size());

            return sslContext;

        } catch (Exception e) {
            logger.error("Failed to initialize custom SSL context: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot initialize SSL context", e);
        }
    }

    /**
     * Creates a trust manager that combines default JVM certificates with custom certificates.
     */
    private X509TrustManager createCombinedTrustManager(
            List<CertificateLoader.CertificateEntry> customCerts)
            throws Exception {

        // Get default trust manager
        TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        defaultTmf.init((KeyStore) null); // null = use default JVM trust store

        X509TrustManager defaultTrustManager = Arrays.stream(defaultTmf.getTrustManagers())
            .filter(tm -> tm instanceof X509TrustManager)
            .map(tm -> (X509TrustManager) tm)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No default X509TrustManager found"));

        // If no custom certificates, just use default
        if (customCerts.isEmpty()) {
            logger.info("No custom certificates found, using default JVM trust store only");
            return defaultTrustManager;
        }

        // Create custom trust manager with CDP certificates
        KeyStore customKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        customKeyStore.load(null, null); // Initialize empty

        for (CertificateLoader.CertificateEntry entry : customCerts) {
            customKeyStore.setCertificateEntry(entry.name(), entry.certificate());
            logger.debug("Added certificate to custom trust store: {}", entry.name());
        }

        TrustManagerFactory customTmf = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        );
        customTmf.init(customKeyStore);

        X509TrustManager customTrustManager = Arrays.stream(customTmf.getTrustManagers())
            .filter(tm -> tm instanceof X509TrustManager)
            .map(tm -> (X509TrustManager) tm)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No custom X509TrustManager found"));

        // Combine both trust managers
        return new CombinedTrustManager(defaultTrustManager, customTrustManager);
    }

    /**
     * Trust manager that delegates to both default and custom trust managers.
     *
     * This implementation tries the default JVM trust manager first (for common CAs like
     * Let's Encrypt, Amazon, etc.), then falls back to the custom CDP trust manager.
     *
     * This approach ensures:
     * 1. Standard HTTPS connections work out of the box
     * 2. CDP internal services with custom CAs are also trusted
     * 3. Security is maintained (only trusted certificates are accepted)
     */
    private static class CombinedTrustManager implements X509TrustManager {

        private final X509TrustManager defaultTrustManager;
        private final X509TrustManager customTrustManager;

        public CombinedTrustManager(X509TrustManager defaultTrustManager,
                                   X509TrustManager customTrustManager) {
            this.defaultTrustManager = defaultTrustManager;
            this.customTrustManager = customTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                defaultTrustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                // If default trust manager rejects, try custom
                customTrustManager.checkClientTrusted(chain, authType);
            }
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                // If default trust manager rejects, try custom
                customTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            // Combine accepted issuers from both trust managers
            X509Certificate[] defaultIssuers = defaultTrustManager.getAcceptedIssuers();
            X509Certificate[] customIssuers = customTrustManager.getAcceptedIssuers();

            X509Certificate[] combined = new X509Certificate[
                defaultIssuers.length + customIssuers.length
            ];
            System.arraycopy(defaultIssuers, 0, combined, 0, defaultIssuers.length);
            System.arraycopy(customIssuers, 0, combined, defaultIssuers.length, customIssuers.length);

            return combined;
        }
    }
}
