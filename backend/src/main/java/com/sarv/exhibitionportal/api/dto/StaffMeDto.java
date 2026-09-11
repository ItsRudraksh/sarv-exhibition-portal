package com.sarv.exhibitionportal.api.dto;

import java.util.Set;
import java.util.UUID;

/** Signed-in staff identity and role codes for the /staff and /admin shells. */
public record StaffMeDto(UUID id, String email, String displayName, Set<String> roles) {}
