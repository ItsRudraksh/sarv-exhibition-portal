package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record FinishedGoodDto(UUID id, Long externalId, String code, String name) {}
