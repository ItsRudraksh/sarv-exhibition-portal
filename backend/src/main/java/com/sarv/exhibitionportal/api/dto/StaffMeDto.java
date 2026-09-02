package com.sarv.exhibitionportal.api.dto;

import java.util.Set;
import java.util.UUID;

public record StaffMeDto(UUID id, String email, String displayName, Set<String> roles) {}
