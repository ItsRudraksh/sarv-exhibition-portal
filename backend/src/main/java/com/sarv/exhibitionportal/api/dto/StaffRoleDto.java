package com.sarv.exhibitionportal.api.dto;

/** Assignable role (ADMIN, SUPPLIER_REVIEWER, MARKETING, EXPORTER, TAXONOMY_MANAGER). */
public record StaffRoleDto(String code, String name, String description) {}
