package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.CampaignDto;
import com.sarv.exhibitionportal.campaign.CampaignService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaigns;

    public CampaignController(CampaignService campaigns) {
        this.campaigns = campaigns;
    }

    @GetMapping("/{code}")
    public CampaignDto get(@PathVariable String code) {
        return campaigns.getActive(code);
    }
}
