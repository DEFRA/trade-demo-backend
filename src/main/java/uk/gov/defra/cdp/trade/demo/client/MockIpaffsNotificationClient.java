package uk.gov.defra.cdp.trade.demo.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Mock implementation of IPAFFS Notification Client.
 *
 * Generates a CHED reference based on the notification ID without making actual API calls.
 * This will be replaced with a real implementation once IPAFFS integration is ready.
 */
@Component
@Slf4j
@Profile("local | integration-test")
public class MockIpaffsNotificationClient implements IpaffsNotificationClient {

    /**
     * Submit a notification to IPAFFS (mock implementation).
     *
     * Transforms the notification ID into a CHED reference:
     * CDP.2025.12.05.6 -> CHEDA.2025.12050600
     *
     * @param notification   the IPAFFS notification to submit
     * @return the generated CHED reference (e.g., CHEDA.2025.12050600)
     */
    @Override
    public ResponseEntity<String> submitNotification(IpaffsNotification notification) {
        String notificationId = notification.getExternalReferences().getFirst().getReference();
        log.info("Mock IPAFFS submission for notification: {}", notificationId);
        log.debug("INS-ConversationId header would be set to: {}", notificationId);

        String chedReference = generateChedReference(notificationId);
        log.info("Generated CHED reference: {}", chedReference);

        return new ResponseEntity<>(chedReference, HttpStatus.CREATED);
    }

    /**
     * Generate a CHED reference from a CDP notification ID.
     *
     * Transforms: CDP.YYYY.MM.DD.S -> CHEDA.YYYY.MMDDSSSS
     * Example: CDP.2025.12.05.6 -> CHEDA.2025.12050600
     *
     * @param notificationId the CDP notification ID
     * @return the CHED reference
     */
    private String generateChedReference(String notificationId) {
        // Parse notification ID: CDP.2025.12.05.6
        String[] parts = notificationId.split("\\.");

        if (parts.length != 5) {
            throw new IllegalArgumentException(
                "Invalid notification ID format. Expected: CDP.YYYY.MM.DD.S, got: " + notificationId);
        }

        String year = parts[1];      // 2025
        String month = parts[2];     // 12
        String day = parts[3];       // 05
        String sequence = parts[4];  // 6

        // Pad sequence to 4 digits: 6 -> 0600
        String paddedSequence = String.format("%04d", Integer.parseInt(sequence) * 100);

        // Build CHED reference: CHEDA.2025.12050600
        return String.format("CHEDA.%s.%s%s%s", year, month, day, paddedSequence);
    }
}
