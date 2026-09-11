package com.sarv.exhibitionportal.staff;

import com.sarv.exhibitionportal.api.dto.CreateStaffUserRequest;
import com.sarv.exhibitionportal.api.dto.StaffAccountDto;
import com.sarv.exhibitionportal.api.dto.StaffRoleDto;
import com.sarv.exhibitionportal.api.dto.UpdateStaffUserRequest;
import com.sarv.exhibitionportal.audit.AuditService;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
/** Create/update/deactivate staff. Last ADMIN and self-deactivation are rejected. */
public class StaffAdminService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE", "SUSPENDED");
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 128;

    private final StaffAdminRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audits;

    public StaffAdminService(
            StaffAdminRepository users,
            PasswordEncoder encoder,
            AuditService audits
    ) {
        this.users = users;
        this.encoder = encoder;
        this.audits = audits;
    }

    @Transactional(readOnly = true)
    public List<StaffAccountDto> list() {
        return users.listUsers().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public StaffAccountDto get(UUID id) {
        return users.findUser(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
    }

    @Transactional(readOnly = true)
    public List<StaffRoleDto> roles() {
        return users.listRoles().stream()
                .map(row -> new StaffRoleDto(row.code(), row.name(), row.description()))
                .toList();
    }

    @Transactional
    public StaffAccountDto create(CreateStaffUserRequest request, StaffUser actor) {
        String email = normalizeEmail(request == null ? null : request.email());
        String displayName = normalizeName(request == null ? null : request.displayName());
        String status = normalizeStatus(request == null ? null : request.status(), "ACTIVE");
        Set<String> roles = normalizeRoles(request == null ? null : request.roles());
        String password = request == null ? null : request.password();
        requirePassword(password);
        if (users.emailTaken(email, null)) {
            throw new InquiryValidationException("That work email is already in use.");
        }
        UUID id = UUID.randomUUID();
        try {
            users.insertUser(id, "staff:" + id, email, displayName, encoder.encode(password), status);
        } catch (DataIntegrityViolationException ex) {
            throw new InquiryValidationException("That work email is already in use.");
        }
        users.replaceRoles(id, roles, actor.id());
        audits.recordUser(
                null,
                "APP_USER",
                id,
                "STAFF_USER_CREATED",
                actor.id(),
                Map.of("status", status, "roles", List.copyOf(roles))
        );
        return get(id);
    }

    @Transactional
    public StaffAccountDto update(UUID id, UpdateStaffUserRequest request, StaffUser actor) {
        StaffAdminRepository.StaffAccountRow current = users.findUser(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
        String email = request == null || request.email() == null || request.email().isBlank()
                ? current.email()
                : normalizeEmail(request.email());
        String displayName = request == null || request.displayName() == null || request.displayName().isBlank()
                ? current.displayName()
                : normalizeName(request.displayName());
        String status = request == null || request.status() == null || request.status().isBlank()
                ? current.status()
                : normalizeStatus(request.status(), current.status());
        Set<String> roles = request == null || request.roles() == null
                ? new LinkedHashSet<>(current.roles())
                : normalizeRoles(request.roles());
        String password = request == null ? null : request.password();
        boolean passwordChanged = password != null && !password.isBlank();
        if (passwordChanged) {
            requirePassword(password);
        }
        if (users.emailTaken(email, id)) {
            throw new InquiryValidationException("That work email is already in use.");
        }
        guardLastAdmin(current, status, roles);
        if (id.equals(actor.id()) && !"ACTIVE".equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate your own account.");
        }
        try {
            users.updateUser(id, email, displayName, passwordChanged ? encoder.encode(password) : null, status);
        } catch (DataIntegrityViolationException ex) {
            throw new InquiryValidationException("That work email is already in use.");
        }
        users.replaceRoles(id, roles, actor.id());
        audits.recordUser(
                null,
                "APP_USER",
                id,
                "STAFF_USER_UPDATED",
                actor.id(),
                Map.of(
                        "status", status,
                        "roles", List.copyOf(roles),
                        "passwordChanged", passwordChanged
                )
        );
        return get(id);
    }

    @Transactional
    public StaffAccountDto deactivate(UUID id, StaffUser actor) {
        StaffAdminRepository.StaffAccountRow current = users.findUser(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff user not found"));
        if (id.equals(actor.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot deactivate your own account.");
        }
        if ("INACTIVE".equals(current.status())) {
            return toDto(current);
        }
        guardLastAdmin(current, "INACTIVE", current.roles());
        users.updateUser(id, current.email(), current.displayName(), null, "INACTIVE");
        audits.recordUser(
                null,
                "APP_USER",
                id,
                "STAFF_USER_DEACTIVATED",
                actor.id(),
                Map.of("status", "INACTIVE", "roles", List.copyOf(current.roles()))
        );
        return get(id);
    }

    private void guardLastAdmin(
            StaffAdminRepository.StaffAccountRow current,
            String nextStatus,
            Set<String> nextRoles
    ) {
        boolean currentlyActiveAdmin = "ACTIVE".equals(current.status()) && current.roles().contains("ADMIN");
        if (!currentlyActiveAdmin) {
            return;
        }
        boolean remainsActiveAdmin = "ACTIVE".equals(nextStatus) && nextRoles.contains("ADMIN");
        if (remainsActiveAdmin) {
            return;
        }
        if (users.countActiveAdmins() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Keep at least one active administrator.");
        }
    }

    private String normalizeEmail(String raw) {
        String email = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || email.length() > 255 || !EMAIL.matcher(email).matches()) {
            throw new InquiryValidationException("Enter a valid work email.");
        }
        return email;
    }

    private String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty() || name.length() > 255) {
            throw new InquiryValidationException("Enter a display name.");
        }
        return name;
    }

    private String normalizeStatus(String raw, String fallback) {
        String status = raw == null || raw.isBlank() ? fallback : raw.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(status)) {
            throw new InquiryValidationException("Unknown staff status.");
        }
        return status;
    }

    private Set<String> normalizeRoles(Set<String> raw) {
        if (raw == null || raw.isEmpty()) {
            throw new InquiryValidationException("Assign at least one role.");
        }
        Set<String> known = users.knownRoleCodes();
        Set<String> roles = new TreeSet<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String code = value.trim().toUpperCase(Locale.ROOT);
            if (!known.contains(code)) {
                throw new InquiryValidationException("Unknown staff role.");
            }
            roles.add(code);
        }
        if (roles.isEmpty()) {
            throw new InquiryValidationException("Assign at least one role.");
        }
        return roles;
    }

    private void requirePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            throw new InquiryValidationException("Password must be between 8 and 128 characters.");
        }
    }

    private StaffAccountDto toDto(StaffAdminRepository.StaffAccountRow row) {
        return new StaffAccountDto(
                row.id(),
                row.email(),
                row.displayName(),
                row.status(),
                new TreeSet<>(row.roles()),
                row.createdAt(),
                row.updatedAt()
        );
    }
}
