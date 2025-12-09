package uk.gov.defra.cdp.trade.demo.client;

import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Client for submitting notifications to the IPAFFS Notification API.
 */
public interface IpaffsNotificationClient {

    /**
     * Submit a notification to IPAFFS.
     *
     * @param notification   the IPAFFS notification to submit
     * @param notificationId the CDP notification ID (used for INS-ConversationId header)
     * @return the CHED reference returned by IPAFFS
     */
    String submitNotification(IpaffsNotification notification, String notificationId);
}
