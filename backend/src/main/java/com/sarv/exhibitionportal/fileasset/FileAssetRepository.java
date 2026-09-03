package com.sarv.exhibitionportal.fileasset;

import com.sarv.exhibitionportal.api.dto.FileAssetDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
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
                .param("id", JdbcUuids.mysql(row.id()))
                .param("inquiry", JdbcUuids.mysql(row.inquiryId()))
                .param("bundle", JdbcUuids.mysql(row.catalogueBundleId()))
                .param("purpose", JdbcUuids.mysql(row.purpose()))
                .param("name", JdbcUuids.mysql(row.originalFilename()))
                .param("type", JdbcUuids.mysql(row.mediaType()))
                .param("size", JdbcUuids.mysql(row.byteSize()))
                .param("sha", JdbcUuids.mysql(row.sha256()))
                .param("key", JdbcUuids.mysql(row.storageKey()))
                .param("scan", JdbcUuids.mysql(row.securityScanState()))
                .param("proc", JdbcUuids.mysql(row.processingState()))
                .param("retain", JdbcUuids.mysql(java.sql.Timestamp.from(row.retentionUntil())))
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
                .param("scan", JdbcUuids.mysql(scanState))
                .param("proc", JdbcUuids.mysql(processingState))
                .param("id", JdbcUuids.mysql(id))
                .update();
    }

    public Optional<FileAssetRow> find(UUID inquiryId, UUID assetId) {
        return jdbc.sql("""
                        select id, inquiry_id, catalogue_bundle_id, purpose, original_filename, media_type,
                               byte_size, sha256_digest, storage_key, security_scan_state, processing_state
                        from file_assets
                        where inquiry_id = :inquiry and id = :id
                        """)
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("id", JdbcUuids.mysql(assetId))
                .query((rs, n) -> new FileAssetRow(
                        JdbcUuids.get(rs, "id"),
                        JdbcUuids.get(rs, "inquiry_id"),
                        JdbcUuids.get(rs, "catalogue_bundle_id"),
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
                 insert ignore into supplier_inquiries (inquiry_id)
                 values (:id)
                 """)
                .param("id", JdbcUuids.mysql(inquiryId))
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
                .param("id", JdbcUuids.mysql(id))
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("format", JdbcUuids.mysql(format))
                .param("state", JdbcUuids.mysql(processingState))
                .param("completed", JdbcUuids.mysql(complete ? java.sql.Timestamp.from(java.time.Instant.now()) : null))
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
                .param("state", JdbcUuids.mysql(processingState))
                .param("reason", JdbcUuids.mysql(failureReason))
                .param("id", JdbcUuids.mysql(bundleId))
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
                    .param("asset", JdbcUuids.mysql(assetId))
                    .param("name", JdbcUuids.mysql(name))
                    .param("size", JdbcUuids.mysql(size))
                    .param("type", JdbcUuids.mysql(type))
                    .param("id", JdbcUuids.mysql(inquiryId))
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
                .param("asset", JdbcUuids.mysql(assetId))
                .param("name", JdbcUuids.mysql(name))
                .param("size", JdbcUuids.mysql(size))
                .param("type", JdbcUuids.mysql(type))
                .param("id", JdbcUuids.mysql(inquiryId))
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
                .param("bundle", JdbcUuids.mysql(bundleId))
                .param("asset", JdbcUuids.mysql(assetId))
                .param("name", JdbcUuids.mysql(name))
                .param("type", JdbcUuids.mysql(type))
                .param("size", JdbcUuids.mysql(size))
                .param("id", JdbcUuids.mysql(inquiryId))
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
