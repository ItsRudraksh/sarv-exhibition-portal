package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.AppMetaDto;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.finishedgoods.FinishedGoodsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Build flags for the visitor banner ({@code poc}, receipt prefix, finished-goods count). */
@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final ExhibitionProperties properties;
    private final FinishedGoodsService finishedGoods;

    public MetaController(ExhibitionProperties properties, FinishedGoodsService finishedGoods) {
        this.properties = properties;
        this.finishedGoods = finishedGoods;
    }

    @GetMapping
    public AppMetaDto meta() {
        ExhibitionProperties.PharmaErp pharma = properties.pharmaErp();
        return new AppMetaDto(
                properties.poc(),
                properties.referencePrefix(),
                properties.poc() ? "development" : "production",
                pharma != null && pharma.enabled(),
                finishedGoods.activeCount()
        );
    }
}
