package uk.gov.defra.cdp.trade.demo.exceptions;

/**
 * Exception thrown when a requested resource is not found.
 * Will be mapped to 404 Not Found by GlobalExceptionHandler.
 */
public class TradeDemoBackendException extends RuntimeException {

    public TradeDemoBackendException(String message) {
        super(message);
    }
}
