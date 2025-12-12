package uk.gov.defra.cdp.trade.demo.client;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.defra.cdp.trade.demo.domain.ipaffs.IpaffsNotification;

/**
 * Client for submitting notifications to the IPAFFS Notification API.
 */
public interface IpaffsNotificationClient {

    String INS_CONVERSATION_ID_HEADER_KEY = "INS-ConversationId";

    /**
     * Submit a notification to IPAFFS.
     *
     * @param notification the IPAFFS notification to submit
     * @return the CHED reference returned by IPAFFS
     */
    ResponseEntity<String> submitNotification(
        @RequestBody IpaffsNotification notification,
        @RequestHeader(INS_CONVERSATION_ID_HEADER_KEY) String conversationId);
}
