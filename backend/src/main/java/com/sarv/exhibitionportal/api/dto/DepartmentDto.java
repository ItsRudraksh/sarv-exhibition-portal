package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Supplier category (department) from the V7 taxonomy pack. */
public record DepartmentDto(UUID id, String code, String name) {}
