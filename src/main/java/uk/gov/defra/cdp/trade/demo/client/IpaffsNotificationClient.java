package uk.gov.defra.cdp.trade.demo.client;

import org.springframework.http.ResponseEntity;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Client for submitting notifications to the IPAFFS Notification API.
 */
public interface IpaffsNotificationClient {

    /**
     * Submit a notification to IPAFFS.
     *
     * @param notification   the IPAFFS notification to submit
     * @return the CHED reference returned by IPAFFS
     */
    ResponseEntity<String> submitNotification(IpaffsNotification notification);
}
