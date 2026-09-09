package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record StaffAccountDto(
        UUID id,
        String email,
        String displayName,
        String status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {}
