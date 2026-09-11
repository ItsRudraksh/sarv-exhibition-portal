package com.sarv.exhibitionportal.api.dto;

import java.util.UUID;

/** Client file handle. assetId is set after upload; name/size/type are display metadata. */
public record CardFileDto(String name, Long size, String type, UUID assetId) {
    public CardFileDto(String name, Long size, String type) {
        this(name, size, type, null);
    }
}
