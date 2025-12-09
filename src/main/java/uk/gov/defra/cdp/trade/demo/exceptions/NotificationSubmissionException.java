package uk.gov.defra.cdp.trade.demo.exceptions;

/**
 * Exception thrown when notification submission to IPAFFS fails.
 */
public class NotificationSubmissionException extends RuntimeException {

    public NotificationSubmissionException(String message) {
        super(message);
    }

    public NotificationSubmissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
