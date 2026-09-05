package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.BuyerLeadDto;
import com.sarv.exhibitionportal.api.dto.ExportJobDto;
import com.sarv.exhibitionportal.api.dto.StaffMeDto;
import com.sarv.exhibitionportal.api.dto.SupplierReviewDto;
import com.sarv.exhibitionportal.exportjob.ExportService;
import com.sarv.exhibitionportal.review.ReviewService;
import com.sarv.exhibitionportal.staff.StaffUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/staff")
public class StaffController {

    private final ReviewService reviews;
    private final ExportService exports;

    public StaffController(ReviewService reviews, ExportService exports) {
        this.reviews = reviews;
        this.exports = exports;
    }

    @GetMapping("/me")
    public StaffMeDto me(Authentication authentication) {
        StaffUser user = actor(authentication);
        return new StaffMeDto(user.id(), user.getUsername(), user.displayName(), user.roleCodes());
    }

    @GetMapping("/suppliers")
    public List<SupplierReviewDto> suppliers() {
        return reviews.suppliers();
    }

    @GetMapping("/suppliers/{id}")
    public SupplierReviewDto supplier(@PathVariable UUID id) {
        return reviews.supplier(id);
    }

    @PostMapping("/suppliers/{id}/decisions")
    public SupplierReviewDto decide(
            @PathVariable UUID id,
            @RequestBody DecisionRequest body,
            Authentication authentication
    ) {
        return reviews.decide(id, body == null ? null : body.decision(), body == null ? null : body.notes(), actor(authentication));
    }

    @GetMapping("/buyers")
    public List<BuyerLeadDto> buyers() {
        return reviews.buyers();
    }

    @PostMapping("/buyers/{id}/notes")
    public BuyerLeadDto buyerNotes(
            @PathVariable UUID id,
            @RequestBody NotesRequest body,
            Authentication authentication
    ) {
        return reviews.saveBuyerNotes(id, body == null ? null : body.notes(), actor(authentication));
    }

    @PostMapping("/exports")
    public ExportJobDto createExport(Authentication authentication) {
        return exports.createPurchaseLeadExport(actor(authentication));
    }

    @GetMapping("/exports/{id}")
    public ExportJobDto exportJob(@PathVariable UUID id) {
        return exports.get(id);
    }

    @GetMapping("/exports/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        byte[] bytes = exports.download(id);
        ExportJobDto job = exports.get(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                job.mediaType() == null
                        ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        : job.mediaType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(job.originalFilename() == null ? "purchase-leads.xlsx" : job.originalFilename())
                .build());
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    private static StaffUser actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff sign-in required");
    }

    public record DecisionRequest(String decision, String notes) {}

    public record NotesRequest(String notes) {}
}
