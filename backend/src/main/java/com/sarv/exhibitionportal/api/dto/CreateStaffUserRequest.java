package com.sarv.exhibitionportal.api.dto;

import java.util.Set;

/** ADMIN create-staff body. Password is hashed; never returned later. */
public record CreateStaffUserRequest(
        String email,
        String displayName,
        String password,
        Set<String> roles,
        String status
) {}
