package com.sarv.exhibitionportal.api.dto;

/** Confirmed visitor identity: name, work email, country code, mobile. */
public record ContactDto(
        String fullName,
        String workEmail,
        String countryCode,
        String mobileNumber
) {}
