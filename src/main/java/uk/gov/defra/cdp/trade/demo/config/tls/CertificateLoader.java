package uk.gov.defra.cdp.trade.demo.config.tls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads X509 certificates from TRUSTSTORE_* environment variables.
 *
 * This component handles the decoding and parsing of base64-encoded PEM certificates
 * provided by the CDP platform. Individual certificate failures are logged but do not
 * prevent the application from starting.
 */
@Component
public class CertificateLoader {

    private static final Logger logger = LoggerFactory.getLogger(CertificateLoader.class);
    private final TrustStoreEnvironmentScanner scanner;

    public CertificateLoader(TrustStoreEnvironmentScanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Loads X509 certificates from TRUSTSTORE_* environment variables.
     *
     * @return List of successfully loaded certificates
     */
    public List<CertificateEntry> loadCustomCertificates() {
        List<CertificateEntry> certificates = new ArrayList<>();
        Map<String, byte[]> certData = scanner.scanTrustStoreCertificates();

        if (certData.isEmpty()) {
            logger.info("No custom certificates to load");
            return certificates;
        }

        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            logger.error("Failed to get X.509 CertificateFactory: {}", e.getMessage());
            throw new IllegalStateException("Cannot initialize certificate factory", e);
        }

        for (Map.Entry<String, byte[]> entry : certData.entrySet()) {
            String certName = entry.getKey();
            byte[] data = entry.getValue();

            try {
                ByteArrayInputStream certStream = new ByteArrayInputStream(data);
                X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);

                certificates.add(new CertificateEntry(certName, cert));

                logger.info("Successfully loaded certificate: {} (Subject: {})",
                    certName, cert.getSubjectX500Principal().getName());

            } catch (CertificateException e) {
                logger.error("Failed to parse certificate {}: {}. Skipping.",
                    certName, e.getMessage());
            }
        }

        logger.info("Successfully loaded {} out of {} certificate(s)",
            certificates.size(), certData.size());

        return certificates;
    }

    /**
     * Container for certificate name and X509Certificate.
     *
     * @param name Certificate variable name (e.g., TRUSTSTORE_INTERNAL_CA)
     * @param certificate Parsed X509 certificate
     */
    public record CertificateEntry(String name, X509Certificate certificate) {}
}
