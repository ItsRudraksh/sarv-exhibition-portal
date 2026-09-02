package com.sarv.exhibitionportal.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record InquiryDraftDto(
        UUID id,
        String lifecycleState,
        String currentStep,
        String route,
        String entryChannel,
        CardFileDto cardFront,
        CardFileDto cardBack,
        String cardQrPayloadInternal,
        ContactDto contact,
        SupplierDto supplier,
        List<UUID> departmentIds,
        List<UUID> productTypeIds,
        BuyerDto buyer,
        boolean contactConfirmed,
        Instant submittedAt,
        String referenceCode
) {}
