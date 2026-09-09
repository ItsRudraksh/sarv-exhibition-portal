package com.sarv.exhibitionportal;

import com.sarv.exhibitionportal.api.dto.CreateStaffUserRequest;
import com.sarv.exhibitionportal.api.dto.StaffAccountDto;
import com.sarv.exhibitionportal.api.dto.StaffMeDto;
import com.sarv.exhibitionportal.api.dto.StaffRoleDto;
import com.sarv.exhibitionportal.api.dto.UpdateStaffUserRequest;
import com.sarv.exhibitionportal.audit.AuditService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class StaffAdminApiTest extends MysqlSpringBootTest {

    private static final UUID SEEDED_ADMIN = UUID.fromString("44444444-4444-4444-8444-444444444443");

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private AuditService audits;

    @Test
    void reviewerCannotManageStaffUsers() {
        ResponseEntity<String> forbidden = reviewer().getForEntity("/api/v1/staff/users", String.class);
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanListRolesAndUsers() {
        ResponseEntity<StaffRoleDto[]> roles = admin().getForEntity("/api/v1/staff/roles", StaffRoleDto[].class);
        assertThat(roles.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(roles.getBody()).isNotNull();
        assertThat(roles.getBody()).extracting(StaffRoleDto::code)
                .contains("ADMIN", "SUPPLIER_REVIEWER", "MARKETING", "EXPORTER", "TAXONOMY_MANAGER");

        ResponseEntity<StaffAccountDto[]> users = admin().getForEntity("/api/v1/staff/users", StaffAccountDto[].class);
        assertThat(users.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(users.getBody()).isNotNull();
        assertThat(users.getBody()).extracting(StaffAccountDto::email)
                .contains("admin@sarv.local", "reviewer@sarv.local", "marketing@sarv.local");
    }

    @Test
    void adminCanCreateUpdateAndDeactivateStaffUser() {
        String email = "staff-" + UUID.randomUUID() + "@sarv.local";
        CreateStaffUserRequest create = new CreateStaffUserRequest(
                email, "Stall Operator", "change-me-now", Set.of("MARKETING"), "ACTIVE");
        ResponseEntity<StaffAccountDto> created = admin().postForEntity(
                "/api/v1/staff/users", create, StaffAccountDto.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isNotNull();
        UUID id = created.getBody().id();
        assertThat(created.getBody().email()).isEqualTo(email);
        assertThat(created.getBody().roles()).containsExactly("MARKETING");
        assertThat(audits.countByEntity("APP_USER", id, "STAFF_USER_CREATED")).isEqualTo(1);

        StaffMeDto signedIn = rest.withBasicAuth(email, "change-me-now")
                .getForObject("/api/v1/staff/me", StaffMeDto.class);
        assertThat(signedIn).isNotNull();
        assertThat(signedIn.email()).isEqualTo(email);
        assertThat(signedIn.roles()).contains("MARKETING");

        UpdateStaffUserRequest update = new UpdateStaffUserRequest(
                email, "Stall Lead", "new-pass-ok", Set.of("MARKETING", "EXPORTER"), "ACTIVE");
        ResponseEntity<StaffAccountDto> updated = admin().exchange(
                "/api/v1/staff/users/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(update),
                StaffAccountDto.class);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isNotNull();
        assertThat(updated.getBody().displayName()).isEqualTo("Stall Lead");
        assertThat(updated.getBody().roles()).containsExactly("EXPORTER", "MARKETING");
        assertThat(audits.countByEntity("APP_USER", id, "STAFF_USER_UPDATED")).isEqualTo(1);

        StaffMeDto afterPassword = rest.withBasicAuth(email, "new-pass-ok")
                .getForObject("/api/v1/staff/me", StaffMeDto.class);
        assertThat(afterPassword).isNotNull();
        assertThat(afterPassword.displayName()).isEqualTo("Stall Lead");

        ResponseEntity<StaffAccountDto> deactivated = admin().exchange(
                "/api/v1/staff/users/" + id,
                HttpMethod.DELETE,
                null,
                StaffAccountDto.class);
        assertThat(deactivated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(deactivated.getBody()).isNotNull();
        assertThat(deactivated.getBody().status()).isEqualTo("INACTIVE");
        assertThat(audits.countByEntity("APP_USER", id, "STAFF_USER_DEACTIVATED")).isEqualTo(1);

        ResponseEntity<String> denied = rest.withBasicAuth(email, "new-pass-ok")
                .getForEntity("/api/v1/staff/me", String.class);
        assertThat(denied.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cannotDeactivateLastAdminOrSelf() {
        ResponseEntity<String> self = admin().exchange(
                "/api/v1/staff/users/" + SEEDED_ADMIN,
                HttpMethod.DELETE,
                null,
                String.class);
        assertThat(self.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(self.getBody()).contains("own account");
    }

    @Test
    void cannotStripLastActiveAdminRole() {
        String email = "admin2-" + UUID.randomUUID() + "@sarv.local";
        StaffAccountDto extra = admin().postForObject(
                "/api/v1/staff/users",
                new CreateStaffUserRequest(email, "Second Admin", "change-me-now", Set.of("ADMIN"), "ACTIVE"),
                StaffAccountDto.class);
        assertThat(extra).isNotNull();
        admin().exchange("/api/v1/staff/users/" + extra.id(), HttpMethod.DELETE, null, StaffAccountDto.class);

        ResponseEntity<String> strip = admin().exchange(
                "/api/v1/staff/users/" + SEEDED_ADMIN,
                HttpMethod.PUT,
                new HttpEntity<>(new UpdateStaffUserRequest(
                        "admin@sarv.local", "POC Admin", null, Set.of("MARKETING"), "ACTIVE")),
                String.class);
        assertThat(strip.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(strip.getBody()).contains("at least one active administrator");
    }

    @Test
    void duplicateEmailIsRejected() {
        CreateStaffUserRequest duplicate = new CreateStaffUserRequest(
                "reviewer@sarv.local", "Copy", "another-pass", Set.of("MARKETING"), "ACTIVE");
        ResponseEntity<String> response = admin().postForEntity("/api/v1/staff/users", duplicate, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("already in use");
    }

    @Test
    void unknownRoleIsRejected() {
        CreateStaffUserRequest body = new CreateStaffUserRequest(
                "bad-role-" + UUID.randomUUID() + "@sarv.local",
                "No Role",
                "another-pass",
                Set.of("NOT_A_ROLE"),
                "ACTIVE");
        ResponseEntity<String> response = admin().postForEntity("/api/v1/staff/users", body, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Unknown staff role");
    }

    private TestRestTemplate admin() {
        return rest.withBasicAuth("admin@sarv.local", "poc-staff");
    }

    private TestRestTemplate reviewer() {
        return rest.withBasicAuth("reviewer@sarv.local", "poc-staff");
    }
}
