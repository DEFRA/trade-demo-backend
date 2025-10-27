package uk.gov.defra.cdp.trade.demo.exceptions;

/**
 * Exception thrown when a resource conflict occurs (e.g., duplicate key).
 * Will be mapped to 409 Conflict by GlobalExceptionHandler.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
