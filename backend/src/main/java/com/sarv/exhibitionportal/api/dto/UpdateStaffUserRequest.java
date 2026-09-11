package com.sarv.exhibitionportal.api.dto;

import java.util.Set;

/** ADMIN update-staff body. Empty password means leave unchanged. */
public record UpdateStaffUserRequest(
        String email,
        String displayName,
        String password,
        Set<String> roles,
        String status
) {}
