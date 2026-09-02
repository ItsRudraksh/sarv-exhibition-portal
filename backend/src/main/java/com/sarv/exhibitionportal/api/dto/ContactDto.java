package com.sarv.exhibitionportal.api.dto;

public record ContactDto(
        String fullName,
        String workEmail,
        String countryCode,
        String mobileNumber
) {}
