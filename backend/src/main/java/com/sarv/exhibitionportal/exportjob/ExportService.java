package com.sarv.exhibitionportal.exportjob;

import com.sarv.exhibitionportal.api.dto.ExportJobDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.config.JdbcUuids;
import com.sarv.exhibitionportal.fileasset.LocalObjectStorage;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import com.sarv.exhibitionportal.review.ReviewRepository;
import com.sarv.exhibitionportal.staff.StaffUser;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
            List<ReviewRepository.BuyerLeadRow> leads = reviews.listBuyers();
            byte[] bytes = toXlsx(leads);
            String key = "exports/" + id + "/purchase-leads.xlsx";
            storage.write(Path.of(properties.storageRoot()), key, bytes);
            jdbc.sql("""
                     update export_jobs
                     set state = 'READY',
                         storage_key = :key,
                         original_filename = 'purchase-leads.xlsx',
                         media_type = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
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
                    "rowCount", leads.size(),
                    "format", "xlsx"
            ));
        } catch (InquiryValidationException ex) {
            markFailed(id);
            throw ex;
        } catch (Exception ex) {
            markFailed(id);
            throw new InquiryValidationException("Could not generate the export file.");
        }
        return get(id);
    }

    private void markFailed(UUID id) {
        jdbc.sql("""
                 update export_jobs
                 set state = 'FAILED', failure_reason = 'generation-failed', updated_at = CURRENT_TIMESTAMP
                 where id = :id
                 """)
                .param("id", JdbcUuids.mysql(id))
                .update();
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

    static byte[] toXlsx(List<ReviewRepository.BuyerLeadRow> rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Purchase leads");
            Row header = sheet.createRow(0);
            String[] columns = {
                    "reference_code", "submitted_at", "person_name", "email",
                    "company_name", "requirement", "lead_state"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }
            int r = 1;
            for (ReviewRepository.BuyerLeadRow row : rows) {
                Row excelRow = sheet.createRow(r++);
                excelRow.createCell(0).setCellValue(nullToEmpty(row.referenceCode()));
                excelRow.createCell(1).setCellValue(
                        row.submittedAt() == null ? "" : row.submittedAt().toString());
                excelRow.createCell(2).setCellValue(nullToEmpty(row.personName()));
                excelRow.createCell(3).setCellValue(nullToEmpty(row.email()));
                excelRow.createCell(4).setCellValue(nullToEmpty(row.companyName()));
                excelRow.createCell(5).setCellValue(nullToEmpty(row.requirement()));
                excelRow.createCell(6).setCellValue(nullToEmpty(row.leadState()));
            }
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
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
