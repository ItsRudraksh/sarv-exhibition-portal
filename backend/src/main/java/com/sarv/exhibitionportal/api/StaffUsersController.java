package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.CreateStaffUserRequest;
import com.sarv.exhibitionportal.api.dto.StaffAccountDto;
import com.sarv.exhibitionportal.api.dto.StaffRoleDto;
import com.sarv.exhibitionportal.api.dto.UpdateStaffUserRequest;
import com.sarv.exhibitionportal.staff.StaffAdminService;
import com.sarv.exhibitionportal.staff.StaffUser;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** ADMIN-only staff account CRUD. Delete deactivates; last ADMIN cannot be removed. */
@RestController
@RequestMapping("/api/v1/staff")
public class StaffUsersController {

    private final StaffAdminService staffUsers;

    public StaffUsersController(StaffAdminService staffUsers) {
        this.staffUsers = staffUsers;
    }

    @GetMapping("/roles")
    public List<StaffRoleDto> roles() {
        return staffUsers.roles();
    }

    @GetMapping("/users")
    public List<StaffAccountDto> list() {
        return staffUsers.list();
    }

    @GetMapping("/users/{id}")
    public StaffAccountDto get(@PathVariable UUID id) {
        return staffUsers.get(id);
    }

    @PostMapping("/users")
    public ResponseEntity<StaffAccountDto> create(
            @RequestBody CreateStaffUserRequest body,
            Authentication authentication
    ) {
        StaffAccountDto created = staffUsers.create(body, actor(authentication));
        return ResponseEntity.created(URI.create("/api/v1/staff/users/" + created.id())).body(created);
    }

    @PutMapping("/users/{id}")
    public StaffAccountDto update(
            @PathVariable UUID id,
            @RequestBody UpdateStaffUserRequest body,
            Authentication authentication
    ) {
        return staffUsers.update(id, body, actor(authentication));
    }

    @DeleteMapping("/users/{id}")
    public StaffAccountDto deactivate(@PathVariable UUID id, Authentication authentication) {
        return staffUsers.deactivate(id, actor(authentication));
    }

    private static StaffUser actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff sign-in required");
    }
}
