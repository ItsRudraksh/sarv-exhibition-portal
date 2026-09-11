package com.sarv.exhibitionportal.outbox;

/** Outbox attempt failed; worker retries. Inquiry is not deleted. */
public class DeliveryFailedException extends RuntimeException {

    private final String code;

    public DeliveryFailedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
