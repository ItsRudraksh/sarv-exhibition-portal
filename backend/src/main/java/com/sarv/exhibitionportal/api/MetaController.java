package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.AppMetaDto;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
public class MetaController {

    private final ExhibitionProperties properties;

    public MetaController(ExhibitionProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public AppMetaDto meta() {
        return new AppMetaDto(
                properties.poc(),
                properties.referencePrefix(),
                properties.poc() ? "development" : "production"
        );
    }
}
