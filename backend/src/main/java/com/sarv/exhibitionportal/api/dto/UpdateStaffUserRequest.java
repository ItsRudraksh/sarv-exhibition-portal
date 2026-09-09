package com.sarv.exhibitionportal.api.dto;

import java.util.Set;

public record UpdateStaffUserRequest(
        String email,
        String displayName,
        String password,
        Set<String> roles,
        String status
) {}
