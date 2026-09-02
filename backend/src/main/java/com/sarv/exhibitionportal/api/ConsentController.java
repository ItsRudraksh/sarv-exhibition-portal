package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.ConsentDto;
import com.sarv.exhibitionportal.consent.ConsentService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inquiries/{inquiryId}/consents")
public class ConsentController {

    private final ConsentService consents;

    public ConsentController(ConsentService consents) {
        this.consents = consents;
    }

    @GetMapping
    public List<ConsentDto> list(@PathVariable UUID inquiryId) {
        return consents.list(inquiryId);
    }

    @PostMapping
    public ConsentDto record(@PathVariable UUID inquiryId, @RequestBody ConsentRequest body) {
        return consents.record(
                inquiryId,
                body == null ? null : body.purpose(),
                body == null ? null : body.decision()
        );
    }

    public record ConsentRequest(String purpose, String decision) {}
}
