package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

public record CardFileDto(String name, Long size, String type, UUID assetId) {
    public CardFileDto(String name, Long size, String type) {
        this(name, size, type, null);
    }
}
