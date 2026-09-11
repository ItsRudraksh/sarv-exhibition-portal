package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Admin staff-user row. Password hash is never serialised here. */
public record StaffAccountDto(
        UUID id,
        String email,
        String displayName,
        String status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {}
