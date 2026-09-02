package com.sarv.exhibitionportal.outbox;

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
