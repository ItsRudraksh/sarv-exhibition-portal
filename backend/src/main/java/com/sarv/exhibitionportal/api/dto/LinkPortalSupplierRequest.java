package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Admin: tag a submitted portal supplier onto the buy catalogue (not Add to production). */
public record LinkPortalSupplierRequest(UUID inquiryId) {}
