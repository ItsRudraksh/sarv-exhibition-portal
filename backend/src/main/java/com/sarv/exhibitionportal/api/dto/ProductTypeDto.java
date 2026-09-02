package com.sarv.exhibitionportal.api.dto;

import java.util.List;
import java.util.UUID;

public record ProductTypeDto(UUID id, String code, String name, List<UUID> departmentIds) {}
