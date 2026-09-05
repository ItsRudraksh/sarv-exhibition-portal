package com.sarv.exhibitionportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarv.exhibitionportal.api.dto.AppMetaDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class MetaApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void metaIsPublicAndReportsDevDefaults() {
        ResponseEntity<AppMetaDto> response = rest.getForEntity("/api/v1/meta", AppMetaDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().poc()).isTrue();
        assertThat(response.getBody().referencePrefix()).isEqualTo("POC-");
        assertThat(response.getBody().stage()).isEqualTo("development");
    }
}
