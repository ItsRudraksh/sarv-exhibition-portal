package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.dto.StaffMeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffPasswordBootstrapTest {

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> SharedEmbeddedMysql.jdbcUrl("exhibition_bootstrap"));
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("exhibition.staff-bootstrap-password", () -> "deploy-rotate-test-1");
    }

    @Autowired
    private TestRestTemplate rest;

    @Test
    void bootstrapPasswordReplacesSeededNoopHash() {
        ResponseEntity<StaffMeDto> ok = rest.withBasicAuth("reviewer@sarv.local", "deploy-rotate-test-1")
                .getForEntity("/api/v1/staff/me", StaffMeDto.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ok.getBody()).isNotNull();
        assertThat(ok.getBody().email()).isEqualTo("reviewer@sarv.local");

        ResponseEntity<String> old = rest.withBasicAuth("reviewer@sarv.local", "poc-staff")
                .getForEntity("/api/v1/staff/me", String.class);
        assertThat(old.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
