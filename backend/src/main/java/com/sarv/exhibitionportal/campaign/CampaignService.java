package com.sarv.exhibitionportal.campaign;

import com.sarv.exhibitionportal.api.dto.CampaignDto;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CampaignService {

    private final CampaignRepository campaigns;

    public CampaignService(CampaignRepository campaigns) {
        this.campaigns = campaigns;
    }

    @Transactional(readOnly = true)
    public CampaignDto requireActive(String code) {
        CampaignDto campaign = campaigns.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        if (!campaign.active()) {
            throw new InquiryValidationException("That campaign QR is not active.");
        }
        return campaign;
    }

    @Transactional(readOnly = true)
    public CampaignDto getActive(String code) {
        return requireActive(code);
    }
}
