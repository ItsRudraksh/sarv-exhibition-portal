package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.FileAssetDto;
import com.sarv.exhibitionportal.fileasset.FileAssetService;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/inquiries/{inquiryId}/files")
public class FileController {

    private final FileAssetService files;

    public FileController(FileAssetService files) {
        this.files = files;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public FileAssetDto upload(
            @PathVariable UUID inquiryId,
            @RequestParam String purpose,
            @RequestParam(required = false) String side,
            @RequestParam("file") MultipartFile file
    ) {
        return files.upload(inquiryId, purpose, side, file);
    }

    @GetMapping("/{assetId}")
    public ResponseEntity<byte[]> download(@PathVariable UUID inquiryId, @PathVariable UUID assetId) {
        FileAssetService.StoredFile stored = files.download(inquiryId, assetId);
        MediaType type;
        try {
            type = MediaType.parseMediaType(stored.mediaType());
        } catch (Exception ex) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(stored.filename())
                        .build()
                        .toString())
                .body(stored.bytes());
    }
}
