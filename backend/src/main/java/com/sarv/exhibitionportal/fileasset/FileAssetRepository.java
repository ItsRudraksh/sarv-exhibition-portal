package com.sarv.exhibitionportal.fileasset;

import com.sarv.exhibitionportal.api.dto.FileAssetDto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class FileAssetRepository {

    private final JdbcClient jdbc;

    public FileAssetRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(FileAssetRow row) {
        jdbc.sql("""
                 insert into file_assets (
                     id, inquiry_id, catalogue_bundle_id, purpose, original_filename, media_type,
                     byte_size, sha256_digest, storage_key, security_scan_state, processing_state,
                     retention_until
                 ) values (
                     :id, :inquiry, :bundle, :purpose, :name, :type, :size, :sha, :key,
                     :scan, :proc, :retain
                 )
                 """)
                .param("id", row.id())
                .param("inquiry", row.inquiryId())
                .param("bundle", row.catalogueBundleId())
                .param("purpose", row.purpose())
                .param("name", row.originalFilename())
                .param("type", row.mediaType())
                .param("size", row.byteSize())
                .param("sha", row.sha256())
                .param("key", row.storageKey())
                .param("scan", row.securityScanState())
                .param("proc", row.processingState())
                .param("retain", java.sql.Timestamp.from(row.retentionUntil()))
                .update();
    }

    public void updateScan(UUID id, String scanState, String processingState) {
        jdbc.sql("""
                 update file_assets
                 set security_scan_state = :scan,
                     processing_state = :proc,
                     updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("scan", scanState)
                .param("proc", processingState)
                .param("id", id)
                .update();
    }

    public Optional<FileAssetRow> find(UUID inquiryId, UUID assetId) {
        return jdbc.sql("""
                        select id, inquiry_id, catalogue_bundle_id, purpose, original_filename, media_type,
                               byte_size, sha256_digest, storage_key, security_scan_state, processing_state
                        from file_assets
                        where inquiry_id = :inquiry and id = :id
                        """)
                .param("inquiry", inquiryId)
                .param("id", assetId)
                .query((rs, n) -> new FileAssetRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("inquiry_id", UUID.class),
                        rs.getObject("catalogue_bundle_id", UUID.class),
                        rs.getString("purpose"),
                        rs.getString("original_filename"),
                        rs.getString("media_type"),
                        rs.getLong("byte_size"),
                        rs.getString("sha256_digest"),
                        rs.getString("storage_key"),
                        rs.getString("security_scan_state"),
                        rs.getString("processing_state"),
                        null
                ))
                .optional();
    }

    public void ensureSupplierInquiry(UUID inquiryId) {
        jdbc.sql("""
                 insert into supplier_inquiries (inquiry_id)
                 values (:id)
                 on conflict (inquiry_id) do nothing
                 """)
                .param("id", inquiryId)
                .update();
    }

    public UUID insertBundle(UUID inquiryId, String format, String processingState) {
        UUID id = UUID.randomUUID();
        boolean complete = "READY".equals(processingState)
                || "FAILED".equals(processingState)
                || "REJECTED".equals(processingState);
        jdbc.sql("""
                 insert into catalogue_bundles (
                     id, supplier_inquiry_id, submission_format, processing_state, completed_at
                 ) values (
                     :id, :inquiry, :format, :state, :completed
                 )
                 """)
                .param("id", id)
                .param("inquiry", inquiryId)
                .param("format", format)
                .param("state", processingState)
                .param("completed", complete ? java.sql.Timestamp.from(java.time.Instant.now()) : null)
                .update();
        return id;
    }

    public void markBundle(UUID bundleId, String processingState, String failureReason) {
        jdbc.sql("""
                 update catalogue_bundles
                 set processing_state = :state,
                     failure_reason = :reason,
                     completed_at = CURRENT_TIMESTAMP,
                     updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("state", processingState)
                .param("reason", failureReason)
                .param("id", bundleId)
                .update();
    }

    public void attachCardAsset(UUID inquiryId, String side, UUID assetId, String name, long size, String type) {
        if ("back".equals(side)) {
            jdbc.sql("""
                     update inquiry_ui_state
                     set card_back_asset_id = :asset,
                         card_back_name = :name,
                         card_back_size = :size,
                         card_back_type = :type,
                         updated_at = CURRENT_TIMESTAMP
                     where inquiry_id = :id
                     """)
                    .param("asset", assetId)
                    .param("name", name)
                    .param("size", size)
                    .param("type", type)
                    .param("id", inquiryId)
                    .update();
            return;
        }
        jdbc.sql("""
                 update inquiry_ui_state
                 set card_front_asset_id = :asset,
                     card_front_name = :name,
                     card_front_size = :size,
                     card_front_type = :type,
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("asset", assetId)
                .param("name", name)
                .param("size", size)
                .param("type", type)
                .param("id", inquiryId)
                .update();
    }

    public void attachCatalogue(UUID inquiryId, UUID bundleId, UUID assetId, String name, String type, long size) {
        jdbc.sql("""
                 update supplier_inquiries
                 set catalogue_bundle_id = :bundle,
                     catalogue_asset_id = :asset,
                     catalogue_filename = :name,
                     catalogue_media_type = :type,
                     catalogue_byte_size = :size,
                     updated_at = CURRENT_TIMESTAMP
                 where inquiry_id = :id
                 """)
                .param("bundle", bundleId)
                .param("asset", assetId)
                .param("name", name)
                .param("type", type)
                .param("size", size)
                .param("id", inquiryId)
                .update();
    }

    public FileAssetDto toDto(FileAssetRow row) {
        return new FileAssetDto(
                row.id(),
                row.inquiryId(),
                row.purpose(),
                row.originalFilename(),
                row.mediaType(),
                row.byteSize(),
                row.securityScanState(),
                row.processingState()
        );
    }

    public record FileAssetRow(
            UUID id,
            UUID inquiryId,
            UUID catalogueBundleId,
            String purpose,
            String originalFilename,
            String mediaType,
            long byteSize,
            String sha256,
            String storageKey,
            String securityScanState,
            String processingState,
            java.time.Instant retentionUntil
    ) {}
}
