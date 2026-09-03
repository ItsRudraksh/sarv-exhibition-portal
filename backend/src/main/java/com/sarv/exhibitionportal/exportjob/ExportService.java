package com.sarv.exhibitionportal.exportjob;

import com.sarv.exhibitionportal.api.dto.ExportJobDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.config.JdbcUuids;
import com.sarv.exhibitionportal.fileasset.LocalObjectStorage;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import com.sarv.exhibitionportal.review.ReviewRepository;
import com.sarv.exhibitionportal.staff.StaffUser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExportService {

    private final JdbcClient jdbc;
    private final ReviewRepository reviews;
    private final LocalObjectStorage storage;
    private final ExhibitionProperties properties;
    private final AuditService audits;

    public ExportService(
            JdbcClient jdbc,
            ReviewRepository reviews,
            LocalObjectStorage storage,
            ExhibitionProperties properties,
            AuditService audits
    ) {
        this.jdbc = jdbc;
        this.reviews = reviews;
        this.storage = storage;
        this.properties = properties;
        this.audits = audits;
    }

    @Transactional
    public ExportJobDto createPurchaseLeadExport(StaffUser actor) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        int hours = properties.exportRetentionHours() <= 0 ? 24 : properties.exportRetentionHours();
        Instant expires = now.plus(hours, ChronoUnit.HOURS);
        jdbc.sql("""
                 insert into export_jobs (
                     id, requested_by_user_id, scope, state, expires_at
                 ) values (
                     :id, :actor, 'PURCHASE_LEADS', 'GENERATING', :expires
                 )
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("actor", JdbcUuids.mysql(actor.id()))
                .param("expires", JdbcUuids.mysql(Timestamp.from(expires)))
                .update();
        try {
            String csv = toCsv(reviews.listBuyers());
            byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
            String key = "exports/" + id + "/purchase-leads.csv";
            storage.write(Path.of(properties.storageRoot()), key, bytes);
            jdbc.sql("""
                     update export_jobs
                     set state = 'READY',
                         storage_key = :key,
                         original_filename = 'purchase-leads.csv',
                         media_type = 'text/csv',
                         byte_size = :size,
                         generated_at = CURRENT_TIMESTAMP,
                         updated_at = CURRENT_TIMESTAMP
                     where id = :id
                     """)
                    .param("id", JdbcUuids.mysql(id))
                    .param("key", JdbcUuids.mysql(key))
                    .param("size", JdbcUuids.mysql((long) bytes.length))
                    .update();
            audits.recordUser(null, "EXPORT_JOB", id, "EXPORT_GENERATED", actor.id(), Map.of(
                    "scope", "PURCHASE_LEADS",
                    "rowCount", reviews.listBuyers().size()
            ));
        } catch (Exception ex) {
            jdbc.sql("""
                     update export_jobs
                     set state = 'FAILED', failure_reason = 'generation-failed', updated_at = CURRENT_TIMESTAMP
                     where id = :id
                     """)
                    .param("id", JdbcUuids.mysql(id))
                    .update();
            throw new InquiryValidationException("Could not generate the export file.");
        }
        return get(id);
    }

    @Transactional(readOnly = true)
    public ExportJobDto get(UUID id) {
        return find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export job not found"));
    }

    public byte[] download(UUID id) {
        ExportRow row = jdbc.sql("""
                select id, scope, state, storage_key, original_filename, media_type, byte_size,
                       expires_at, generated_at, failure_reason
                from export_jobs where id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .query((rs, n) -> new ExportRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("scope"),
                        rs.getString("state"),
                        rs.getString("storage_key"),
                        rs.getString("original_filename"),
                        rs.getString("media_type"),
                        longOrNull(rs.getObject("byte_size")),
                        ts(rs.getTimestamp("expires_at")),
                        ts(rs.getTimestamp("generated_at")),
                        rs.getString("failure_reason")
                ))
                .optional()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Export job not found"));
        if (row.expiresAt() != null && row.expiresAt().isBefore(Instant.now())) {
            jdbc.sql("update export_jobs set state = 'EXPIRED', updated_at = CURRENT_TIMESTAMP where id = :id")
                    .param("id", JdbcUuids.mysql(id))
                    .update();
            throw new ResponseStatusException(HttpStatus.GONE, "This export has expired.");
        }
        if (!"READY".equals(row.state()) || row.storageKey() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Export is not ready.");
        }
        try {
            return storage.read(Path.of(properties.storageRoot()), row.storageKey());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export file is missing.");
        }
    }

    private Optional<ExportJobDto> find(UUID id) {
        return jdbc.sql("""
                select id, scope, state, original_filename, media_type, byte_size, expires_at, generated_at, failure_reason
                from export_jobs where id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .query((rs, n) -> new ExportJobDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("scope"),
                        rs.getString("state"),
                        rs.getString("original_filename"),
                        rs.getString("media_type"),
                        longOrNull(rs.getObject("byte_size")),
                        ts(rs.getTimestamp("expires_at")),
                        ts(rs.getTimestamp("generated_at")),
                        rs.getString("failure_reason")
                ))
                .optional();
    }

    private static String toCsv(List<ReviewRepository.BuyerLeadRow> rows) {
        StringBuilder out = new StringBuilder();
        out.append("reference_code,submitted_at,person_name,email,company_name,requirement,lead_state\n");
        for (ReviewRepository.BuyerLeadRow row : rows) {
            out.append(csv(row.referenceCode())).append(',')
                    .append(csv(row.submittedAt() == null ? "" : row.submittedAt().toString())).append(',')
                    .append(csv(row.personName())).append(',')
                    .append(csv(row.email())).append(',')
                    .append(csv(row.companyName())).append(',')
                    .append(csv(row.requirement())).append(',')
                    .append(csv(row.leadState()))
                    .append('\n');
        }
        return out.toString();
    }

    private static String csv(String value) {
        String raw = value == null ? "" : value.replace("\"", "\"\"");
        return '"' + raw + '"';
    }

    private static Instant ts(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    private static Long longOrNull(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private record ExportRow(
            UUID id,
            String scope,
            String state,
            String storageKey,
            String originalFilename,
            String mediaType,
            Long byteSize,
            Instant expiresAt,
            Instant generatedAt,
            String failureReason
    ) {}
}
