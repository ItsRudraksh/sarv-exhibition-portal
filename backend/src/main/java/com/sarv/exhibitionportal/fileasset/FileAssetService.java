package com.sarv.exhibitionportal.fileasset;

import com.sarv.exhibitionportal.api.dto.FileAssetDto;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.consent.ConsentService;
import com.sarv.exhibitionportal.inquiry.InquiryRepository;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FileAssetService {

    private final FileAssetRepository files;
    private final InquiryRepository inquiries;
    private final LocalObjectStorage storage;
    private final AuditService audits;
    private final ConsentService consents;
    private final ExhibitionProperties properties;

    public FileAssetService(
            FileAssetRepository files,
            InquiryRepository inquiries,
            LocalObjectStorage storage,
            AuditService audits,
            ConsentService consents,
            ExhibitionProperties properties
    ) {
        this.files = files;
        this.inquiries = inquiries;
        this.storage = storage;
        this.audits = audits;
        this.consents = consents;
        this.properties = properties;
    }

    @Transactional(noRollbackFor = InquiryValidationException.class)
    public FileAssetDto upload(UUID inquiryId, String purpose, String side, MultipartFile file) {
        var draft = inquiries.findDraft(inquiryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquiry not found"));
        if ("SUBMITTED".equals(draft.lifecycleState())) {
            throw new InquiryValidationException("Submitted inquiries cannot be changed.");
        }
        if (file == null || file.isEmpty()) {
            throw new InquiryValidationException("Choose a file to upload.");
        }
        String resolvedPurpose = resolvePurpose(purpose);
        if ("BUSINESS_CARD".equals(resolvedPurpose) && side != null && !"front".equals(side) && !"back".equals(side)) {
            throw new InquiryValidationException("Card side must be front or back.");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new InquiryValidationException("Could not read the uploaded file.");
        }
        long max = "BUSINESS_CARD".equals(resolvedPurpose)
                ? properties.cardMaxBytes()
                : properties.catalogueMaxBytes();
        String filename = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String declared = FileContentRules.assertDeclaredAllowlist(
                resolvedPurpose, filename, file.getContentType(), bytes.length, max);

        UUID assetId = UUID.randomUUID();
        String storageKey = LocalObjectStorage.keyFor(
                inquiryId, assetId, LocalObjectStorage.extensionOf(filename, declared));
        Instant retainUntil = Instant.now().plus(properties.fileRetentionDays(), ChronoUnit.DAYS);
        UUID bundleId = null;
        if ("CATALOGUE_ORIGINAL".equals(resolvedPurpose)) {
            files.ensureSupplierInquiry(inquiryId);
            bundleId = files.insertBundle(
                    inquiryId, FileContentRules.catalogueFormat(declared), "PENDING_SCAN");
        }
        Path root = Path.of(properties.storageRoot());
        try {
            storage.write(root, storageKey, bytes);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not store the file", ex);
        }
        files.insert(new FileAssetRepository.FileAssetRow(
                assetId,
                inquiryId,
                bundleId,
                resolvedPurpose,
                filename,
                declared,
                bytes.length,
                LocalObjectStorage.sha256(bytes),
                storageKey,
                "PENDING",
                "UPLOADED",
                retainUntil
        ));
        try {
            FileContentRules.assertContentsMatch(resolvedPurpose, declared, bytes);
        } catch (InquiryValidationException ex) {
            files.updateScan(assetId, "REJECTED", "FAILED");
            if (bundleId != null) {
                files.markBundle(bundleId, "REJECTED", "Content allowlist failed");
            }
            audits.record(inquiryId, "FILE_ASSET", assetId, "FILE_SCAN_REJECTED", Map.of(
                    "purpose", resolvedPurpose,
                    "scanState", "REJECTED"
            ));
            throw ex;
        }
        files.updateScan(assetId, "CLEAN", "READY");
        if ("BUSINESS_CARD".equals(resolvedPurpose)) {
            files.attachCardAsset(
                    inquiryId, side == null ? "front" : side, assetId, filename, bytes.length, declared);
            consents.record(inquiryId, "BUSINESS_CARD_EXTRACTION", "GRANTED");
        } else {
            files.markBundle(bundleId, "READY", null);
            files.attachCatalogue(inquiryId, bundleId, assetId, filename, declared, bytes.length);
        }
        audits.record(inquiryId, "FILE_ASSET", assetId, "FILE_UPLOADED", Map.of(
                "purpose", resolvedPurpose,
                "scanState", "CLEAN"
        ));
        return files.toDto(files.find(inquiryId, assetId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public StoredFile download(UUID inquiryId, UUID assetId) {
        var row = files.find(inquiryId, assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
        if (!"CLEAN".equals(row.securityScanState())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
        try {
            byte[] bytes = storage.read(Path.of(properties.storageRoot()), row.storageKey());
            return new StoredFile(row.originalFilename(), row.mediaType(), bytes);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
        }
    }

    public record StoredFile(String filename, String mediaType, byte[] bytes) {}

    private static String resolvePurpose(String purpose) {
        if (purpose == null || purpose.isBlank() || "BUSINESS_CARD".equals(purpose)) {
            return "BUSINESS_CARD";
        }
        if ("CATALOGUE_ORIGINAL".equals(purpose) || "CATALOGUE".equals(purpose)) {
            return "CATALOGUE_ORIGINAL";
        }
        throw new InquiryValidationException("Unsupported file purpose.");
    }
}
