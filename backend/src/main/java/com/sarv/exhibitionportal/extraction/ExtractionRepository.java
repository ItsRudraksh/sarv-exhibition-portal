package com.sarv.exhibitionportal.extraction;

import com.sarv.exhibitionportal.api.dto.ExtractionDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ExtractionRepository {

    private final JdbcClient jdbc;

    public ExtractionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insertSession(
            UUID id,
            UUID inquiryId,
            UUID consentId,
            String feature,
            String languageCode,
            String state,
            String providerRef,
            Instant completedAt
    ) {
        jdbc.sql("""
                 insert into ai_assistance_sessions (
                     id, inquiry_id, consent_id, feature, language_code, state,
                     provider_request_reference, completed_at
                 ) values (
                     :id, :inquiry, :consent, :feature, :lang, :state, :provider, :completed
                 )
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("consent", JdbcUuids.mysql(consentId))
                .param("feature", JdbcUuids.mysql(feature))
                .param("lang", JdbcUuids.mysql(languageCode))
                .param("state", JdbcUuids.mysql(state))
                .param("provider", JdbcUuids.mysql(providerRef))
                .param("completed", JdbcUuids.mysql(completedAt == null ? null : Timestamp.from(completedAt)))
                .update();
    }

    public void insertExtraction(
            UUID id,
            UUID sessionId,
            UUID assetId,
            String state,
            String providerModel,
            Instant completedAt
    ) {
        jdbc.sql("""
                 insert into ai_extractions (
                     id, session_id, input_asset_id, state, provider_model_reference, completed_at
                 ) values (
                     :id, :session, :asset, :state, :model, :completed
                 )
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("session", JdbcUuids.mysql(sessionId))
                .param("asset", JdbcUuids.mysql(assetId))
                .param("state", JdbcUuids.mysql(state))
                .param("model", JdbcUuids.mysql(providerModel))
                .param("completed", JdbcUuids.mysql(completedAt == null ? null : Timestamp.from(completedAt)))
                .update();
    }

    public void insertField(
            UUID id,
            UUID extractionId,
            String fieldKey,
            String proposedValue,
            BigDecimal confidence
    ) {
        jdbc.sql("""
                 insert into ai_extracted_fields (
                     id, extraction_id, field_key, proposed_value_text, confidence_score, review_state
                 ) values (
                     :id, :extraction, :key, :value, :confidence, 'PENDING'
                 )
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("extraction", JdbcUuids.mysql(extractionId))
                .param("key", JdbcUuids.mysql(fieldKey))
                .param("value", JdbcUuids.mysql(proposedValue))
                .param("confidence", confidence)
                .update();
    }

    public void markFieldReviewed(UUID fieldId, String reviewState, Instant reviewedAt) {
        jdbc.sql("""
                 update ai_extracted_fields
                 set review_state = :state, reviewed_at = :reviewed, updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("state", JdbcUuids.mysql(reviewState))
                .param("reviewed", Timestamp.from(reviewedAt))
                .param("id", JdbcUuids.mysql(fieldId))
                .update();
    }

    public Optional<ExtractionDto> latestForInquiry(UUID inquiryId) {
        Optional<ExtractionRow> row = jdbc.sql("""
                select e.id as extraction_id, e.session_id, s.inquiry_id, s.feature, e.state,
                       e.input_asset_id, e.provider_model_reference, e.completed_at,
                       (u.card_qr_payload_internal is not null
                            and char_length(u.card_qr_payload_internal) > 0) as qr_detected
                from ai_extractions e
                inner join ai_assistance_sessions s on s.id = e.session_id
                left join inquiry_ui_state u on u.inquiry_id = s.inquiry_id
                where s.inquiry_id = :id
                order by e.created_at desc
                limit 1
                """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> new ExtractionRow(
                        JdbcUuids.get(rs, "extraction_id"),
                        JdbcUuids.get(rs, "session_id"),
                        JdbcUuids.get(rs, "inquiry_id"),
                        rs.getString("feature"),
                        rs.getString("state"),
                        JdbcUuids.get(rs, "input_asset_id"),
                        rs.getString("provider_model_reference"),
                        rs.getBoolean("qr_detected"),
                        rs.getTimestamp("completed_at") == null
                                ? null
                                : rs.getTimestamp("completed_at").toInstant()
                ))
                .optional();
        return row.map(this::toDto);
    }

    public Optional<ExtractionDto> find(UUID inquiryId, UUID extractionId) {
        Optional<ExtractionRow> row = jdbc.sql("""
                select e.id as extraction_id, e.session_id, s.inquiry_id, s.feature, e.state,
                       e.input_asset_id, e.provider_model_reference, e.completed_at,
                       (u.card_qr_payload_internal is not null
                            and char_length(u.card_qr_payload_internal) > 0) as qr_detected
                from ai_extractions e
                inner join ai_assistance_sessions s on s.id = e.session_id
                left join inquiry_ui_state u on u.inquiry_id = s.inquiry_id
                where s.inquiry_id = :inquiry and e.id = :extraction
                """)
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("extraction", JdbcUuids.mysql(extractionId))
                .query((rs, n) -> new ExtractionRow(
                        JdbcUuids.get(rs, "extraction_id"),
                        JdbcUuids.get(rs, "session_id"),
                        JdbcUuids.get(rs, "inquiry_id"),
                        rs.getString("feature"),
                        rs.getString("state"),
                        JdbcUuids.get(rs, "input_asset_id"),
                        rs.getString("provider_model_reference"),
                        rs.getBoolean("qr_detected"),
                        rs.getTimestamp("completed_at") == null
                                ? null
                                : rs.getTimestamp("completed_at").toInstant()
                ))
                .optional();
        return row.map(this::toDto);
    }

    public List<FieldRow> fieldsForExtraction(UUID extractionId) {
        return jdbc.sql("""
                        select id, field_key, proposed_value_text, confidence_score, review_state
                        from ai_extracted_fields
                        where extraction_id = :id
                        order by field_key
                        """)
                .param("id", JdbcUuids.mysql(extractionId))
                .query((rs, n) -> new FieldRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("field_key"),
                        rs.getString("proposed_value_text"),
                        rs.getBigDecimal("confidence_score"),
                        rs.getString("review_state")
                ))
                .list();
    }

    public List<FieldRow> pendingFieldsForInquiry(UUID inquiryId) {
        return jdbc.sql("""
                        select f.id, f.field_key, f.proposed_value_text, f.confidence_score, f.review_state
                        from ai_extracted_fields f
                        inner join ai_extractions e on e.id = f.extraction_id
                        inner join ai_assistance_sessions s on s.id = e.session_id
                        where s.inquiry_id = :id and f.review_state = 'PENDING'
                        order by e.created_at desc, f.field_key
                        """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> new FieldRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("field_key"),
                        rs.getString("proposed_value_text"),
                        rs.getBigDecimal("confidence_score"),
                        rs.getString("review_state")
                ))
                .list();
    }

    private ExtractionDto toDto(ExtractionRow row) {
        List<ExtractionDto.ExtractedFieldDto> fields = fieldsForExtraction(row.extractionId()).stream()
                .map(f -> new ExtractionDto.ExtractedFieldDto(
                        f.id(),
                        f.fieldKey(),
                        f.proposedValueText(),
                        f.confidenceScore(),
                        f.reviewState()
                ))
                .toList();
        return new ExtractionDto(
                row.extractionId(),
                row.sessionId(),
                row.inquiryId(),
                row.feature(),
                row.state(),
                row.inputAssetId(),
                row.providerModelReference(),
                row.qrDetected(),
                row.completedAt(),
                fields
        );
    }

    public record FieldRow(
            UUID id,
            String fieldKey,
            String proposedValueText,
            BigDecimal confidenceScore,
            String reviewState
    ) {}

    private record ExtractionRow(
            UUID extractionId,
            UUID sessionId,
            UUID inquiryId,
            String feature,
            String state,
            UUID inputAssetId,
            String providerModelReference,
            boolean qrDetected,
            Instant completedAt
    ) {}
}
