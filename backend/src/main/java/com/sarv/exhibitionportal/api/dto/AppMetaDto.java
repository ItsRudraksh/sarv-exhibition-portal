package com.sarv.exhibitionportal.api.dto;

/** Visitor GET /meta payload (poc banner, receipt prefix, catalogue counts). */
public record AppMetaDto(
        boolean poc,
        String referencePrefix,
        String stage,
        boolean pharmaErpEnabled,
        int finishedGoodsActive
) {}
