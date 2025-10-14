package uk.gov.defra.cdp.trade.demo.config.tls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Scans environment variables for TRUSTSTORE_* entries containing base64-encoded certificates.
 *
 * CDP platform pattern: Custom CA certificates are provided as base64-encoded PEM certificates
 * in environment variables prefixed with TRUSTSTORE_ (e.g., TRUSTSTORE_INTERNAL_CA).
 */
@Component
public class TrustStoreEnvironmentScanner {

    private static final Logger logger = LoggerFactory.getLogger(TrustStoreEnvironmentScanner.class);
    private static final String TRUSTSTORE_PREFIX = "TRUSTSTORE_";

    /**
     * Scans environment variables for TRUSTSTORE_* entries.
     *
     * @return Map of certificate names to base64-decoded certificate data
     */
    public Map<String, byte[]> scanTrustStoreCertificates() {
        Map<String, byte[]> certificates = new HashMap<>();
        Map<String, String> env = System.getenv();

        logger.info("Scanning environment for custom certificates with prefix: {}", TRUSTSTORE_PREFIX);

        for (Map.Entry<String, String> entry : env.entrySet()) {
            String varName = entry.getKey();

            if (!varName.startsWith(TRUSTSTORE_PREFIX)) {
                continue;
            }

            String base64Value = entry.getValue();

            if (base64Value == null || base64Value.isBlank()) {
                logger.warn("Certificate variable {} is empty. Skipping.", varName);
                continue;
            }

            try {
                byte[] certData = Base64.getDecoder().decode(base64Value);
                certificates.put(varName, certData);
                logger.info("Found certificate: {} (size: {} bytes)", varName, certData.length);
            } catch (IllegalArgumentException e) {
                logger.error("Failed to decode base64 for {}: {}. Skipping certificate.",
                    varName, e.getMessage());
            }
        }

        logger.info("Found {} custom certificate(s)", certificates.size());
        return certificates;
    }
}
