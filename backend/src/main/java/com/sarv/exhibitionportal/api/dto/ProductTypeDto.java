package com.sarv.exhibitionportal.api.dto;

import java.util.List;
import java.util.UUID;

/** Supplier product type, valid only via department_product_types mappings. */
public record ProductTypeDto(UUID id, String code, String name, List<UUID> departmentIds) {}
