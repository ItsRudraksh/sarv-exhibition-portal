package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.InquiryDraftDto;
import com.sarv.exhibitionportal.inquiry.InquiryService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries")
public class InquiryController {

    private final InquiryService inquiries;

    public InquiryController(InquiryService inquiries) {
        this.inquiries = inquiries;
    }

    @PostMapping
    public InquiryDraftDto create(@RequestBody(required = false) InquiryDraftDto body) {
        return inquiries.create(body);
    }

    @GetMapping("/{id}")
    public InquiryDraftDto get(@PathVariable UUID id) {
        return inquiries.get(id);
    }

    @PatchMapping("/{id}")
    public InquiryDraftDto save(@PathVariable UUID id, @RequestBody InquiryDraftDto body) {
        return inquiries.save(id, body);
    }

    @PostMapping("/{id}/contact")
    public InquiryDraftDto contact(@PathVariable UUID id, @RequestBody InquiryDraftDto body) {
        return inquiries.confirmContact(id, body);
    }

    @PostMapping("/{id}/submit")
    public InquiryDraftDto submit(@PathVariable UUID id, @RequestBody(required = false) InquiryDraftDto body) {
        return inquiries.submit(id, body);
    }
}
