package com.sarv.exhibitionportal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SpaRoutingTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    void visitorShellIsPublic() {
        ResponseEntity<String> home = rest.getForEntity("/", String.class);
        assertThat(home.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(home.getBody()).contains("sarv-spa-ok");
    }

    @Test
    void staffRouteForwardsToSpaWithoutAuth() {
        ResponseEntity<String> staff = rest.getForEntity("/staff", String.class);
        assertThat(staff.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(staff.getBody()).contains("sarv-spa-ok");
    }

    @Test
    void staffApiStillRequiresAuth() {
        ResponseEntity<String> me = rest.getForEntity("/api/v1/staff/me", String.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
