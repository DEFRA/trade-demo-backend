package uk.gov.defra.cdp.trade.demo.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Unit tests for MockIpaffsNotificationClient.
 */
class MockIpaffsNotificationClientTest {

    private MockIpaffsNotificationClient client;

    @BeforeEach
    void setUp() {
        client = new MockIpaffsNotificationClient();
    }

    @Test
    void submitNotification_shouldGenerateChedReference_fromNotificationId() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String notificationId = "CDP.2025.12.05.6";

        // When
        String result = client.submitNotification(notification, notificationId);

        // Then
        assertThat(result).isEqualTo("CHEDA.2025.12050600");
    }

    @Test
    void submitNotification_shouldTransformSequenceCorrectly_singleDigit() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String notificationId = "CDP.2025.12.09.1";

        // When
        String result = client.submitNotification(notification, notificationId);

        // Then - sequence 1 becomes 0100 (1 * 100 = 100, padded to 4 digits)
        assertThat(result).isEqualTo("CHEDA.2025.12090100");
    }

    @Test
    void submitNotification_shouldTransformSequenceCorrectly_doubleDigit() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String notificationId = "CDP.2025.11.15.42";

        // When
        String result = client.submitNotification(notification, notificationId);

        // Then - sequence 42 becomes 4200 (42 * 100 = 4200)
        assertThat(result).isEqualTo("CHEDA.2025.11154200");
    }

    @Test
    void submitNotification_shouldTransformSequenceCorrectly_tripleDigit() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String notificationId = "CDP.2025.01.20.999";

        // When
        String result = client.submitNotification(notification, notificationId);

        // Then - sequence 999 becomes 99900 (999 * 100 = 99900)
        assertThat(result).isEqualTo("CHEDA.2025.012099900");
    }

    @Test
    void submitNotification_shouldHandleLeadingZerosInMonth() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String notificationId = "CDP.2025.01.05.3";

        // When
        String result = client.submitNotification(notification, notificationId);

        // Then
        assertThat(result).isEqualTo("CHEDA.2025.01050300");
    }

    @Test
    void submitNotification_shouldHandleLeadingZerosInDay() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String notificationId = "CDP.2025.12.01.7";

        // When
        String result = client.submitNotification(notification, notificationId);

        // Then
        assertThat(result).isEqualTo("CHEDA.2025.12010700");
    }

    @Test
    void submitNotification_shouldThrowException_whenInvalidFormat() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String invalidNotificationId = "CDP.2025.12.05"; // Missing sequence number

        // When/Then
        assertThatThrownBy(() -> client.submitNotification(notification, invalidNotificationId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid notification ID format")
            .hasMessageContaining("Expected: CDP.YYYY.MM.DD.S");
    }

    @Test
    void submitNotification_shouldThrowException_whenTooManyParts() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String invalidNotificationId = "CDP.2025.12.05.6.EXTRA";

        // When/Then
        assertThatThrownBy(() -> client.submitNotification(notification, invalidNotificationId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid notification ID format");
    }

    @Test
    void submitNotification_shouldThrowException_whenEmptyString() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String invalidNotificationId = "";

        // When/Then
        assertThatThrownBy(() -> client.submitNotification(notification, invalidNotificationId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submitNotification_shouldThrowException_whenSequenceNotNumeric() {
        // Given
        IpaffsNotification notification = new IpaffsNotification();
        String invalidNotificationId = "CDP.2025.12.05.ABC";

        // When/Then
        assertThatThrownBy(() -> client.submitNotification(notification, invalidNotificationId))
            .isInstanceOf(NumberFormatException.class);
    }
}
