package com.sarv.exhibitionportal;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Java context-path /exhibit for sarvbiolabs.com/exhibit behind a reverse proxy. */
@TestPropertySource(properties = "server.servlet.context-path=/exhibit")
class PublicPathPrefixTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void visitorApiIsUnderExhibitPrefix() {
        assertThat(rest.getRootUri()).contains("/exhibit");
        ResponseEntity<String> meta = rest.getForEntity("/api/v1/meta", String.class);
        assertThat(meta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void spaStillPublicUnderPrefix() {
        ResponseEntity<String> staff = rest.getForEntity("/staff", String.class);
        assertThat(staff.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(staff.getBody()).contains("sarv-spa-ok");
    }

    @Test
    void unprefixedApiIsNotServedWhenContextPathIsExhibit() {
        String prefixed = rest.getRootUri();
        assertThat(prefixed).endsWith("/exhibit");
        String origin = prefixed.substring(0, prefixed.length() - "/exhibit".length());
        ResponseEntity<String> naked = rest.getForEntity(URI.create(origin + "/api/v1/meta"), String.class);
        assertThat(naked.getStatusCode().is2xxSuccessful()).isFalse();
    }
}
