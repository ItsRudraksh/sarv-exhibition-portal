package com.sarv.exhibitionportal.inquiry;

/** Visitor-facing validation failure (HTTP 400 via ApiExceptionHandler). */
public class InquiryValidationException extends RuntimeException {

    public InquiryValidationException(String message) {
        super(message);
    }
}
