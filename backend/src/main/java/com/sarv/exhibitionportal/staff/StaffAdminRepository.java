package com.sarv.exhibitionportal.staff;

import com.sarv.exhibitionportal.config.JdbcUuids;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class StaffAdminRepository {

    private final JdbcClient jdbc;

    public StaffAdminRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<StaffAccountRow> listUsers() {
        return jdbc.sql("""
                select u.id, u.email_normalized, u.display_name, u.status, u.created_at, u.updated_at,
                       group_concat(r.code order by r.code separator ',') as roles
                from app_users u
                left join user_roles ur on ur.user_id = u.id
                left join roles r on r.id = ur.role_id
                group by u.id, u.email_normalized, u.display_name, u.status, u.created_at, u.updated_at
                order by u.email_normalized
                """)
                .query((rs, n) -> new StaffAccountRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("email_normalized"),
                        rs.getString("display_name"),
                        rs.getString("status"),
                        splitRoles(rs.getString("roles")),
                        ts(rs.getTimestamp("created_at")),
                        ts(rs.getTimestamp("updated_at"))
                ))
                .list();
    }

    public Optional<StaffAccountRow> findUser(UUID id) {
        return jdbc.sql("""
                select u.id, u.email_normalized, u.display_name, u.status, u.created_at, u.updated_at,
                       group_concat(r.code order by r.code separator ',') as roles
                from app_users u
                left join user_roles ur on ur.user_id = u.id
                left join roles r on r.id = ur.role_id
                where u.id = :id
                group by u.id, u.email_normalized, u.display_name, u.status, u.created_at, u.updated_at
                """)
                .param("id", JdbcUuids.mysql(id))
                .query((rs, n) -> new StaffAccountRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("email_normalized"),
                        rs.getString("display_name"),
                        rs.getString("status"),
                        splitRoles(rs.getString("roles")),
                        ts(rs.getTimestamp("created_at")),
                        ts(rs.getTimestamp("updated_at"))
                ))
                .optional();
    }

    public List<RoleRow> listRoles() {
        return jdbc.sql("select code, name, description from roles order by code")
                .query((rs, n) -> new RoleRow(
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("description")
                ))
                .list();
    }

    public Set<String> knownRoleCodes() {
        Set<String> codes = new LinkedHashSet<>();
        for (RoleRow row : listRoles()) {
            codes.add(row.code());
        }
        return codes;
    }

    public boolean emailTaken(String email, UUID excludingId) {
        Long count = jdbc.sql("""
                select count(*) from app_users
                where email_normalized = :email
                  and (:exclude is null or id <> :exclude)
                """)
                .param("email", JdbcUuids.mysql(email))
                .param("exclude", JdbcUuids.mysql(excludingId))
                .query(Long.class)
                .single();
        return count != null && count > 0;
    }

    public int countActiveAdmins() {
        Long count = jdbc.sql("""
                select count(distinct u.id)
                from app_users u
                join user_roles ur on ur.user_id = u.id
                join roles r on r.id = ur.role_id
                where u.status = 'ACTIVE' and r.code = 'ADMIN'
                """)
                .query(Long.class)
                .single();
        return count == null ? 0 : count.intValue();
    }

    public void insertUser(
            UUID id,
            String externalSubject,
            String email,
            String displayName,
            String passwordHash,
            String status
    ) {
        jdbc.sql("""
                insert into app_users (
                    id, external_subject, email_normalized, display_name, password_hash, status
                ) values (
                    :id, :subject, :email, :name, :hash, :status
                )
                """)
                .param("id", JdbcUuids.mysql(id))
                .param("subject", JdbcUuids.mysql(externalSubject))
                .param("email", JdbcUuids.mysql(email))
                .param("name", JdbcUuids.mysql(displayName))
                .param("hash", JdbcUuids.mysql(passwordHash))
                .param("status", JdbcUuids.mysql(status))
                .update();
    }

    public void updateUser(
            UUID id,
            String email,
            String displayName,
            String passwordHash,
            String status
    ) {
        if (passwordHash == null) {
            jdbc.sql("""
                    update app_users
                    set email_normalized = :email,
                        display_name = :name,
                        status = :status,
                        updated_at = current_timestamp
                    where id = :id
                    """)
                    .param("id", JdbcUuids.mysql(id))
                    .param("email", JdbcUuids.mysql(email))
                    .param("name", JdbcUuids.mysql(displayName))
                    .param("status", JdbcUuids.mysql(status))
                    .update();
            return;
        }
        jdbc.sql("""
                update app_users
                set email_normalized = :email,
                    display_name = :name,
                    password_hash = :hash,
                    status = :status,
                    updated_at = current_timestamp
                where id = :id
                """)
                .param("id", JdbcUuids.mysql(id))
                .param("email", JdbcUuids.mysql(email))
                .param("name", JdbcUuids.mysql(displayName))
                .param("hash", JdbcUuids.mysql(passwordHash))
                .param("status", JdbcUuids.mysql(status))
                .update();
    }

    public void replaceRoles(UUID userId, Set<String> roleCodes, UUID assignedByUserId) {
        jdbc.sql("delete from user_roles where user_id = :id")
                .param("id", JdbcUuids.mysql(userId))
                .update();
        for (String code : roleCodes) {
            jdbc.sql("""
                    insert into user_roles (user_id, role_id, assigned_by_user_id)
                    select :userId, r.id, :actor
                    from roles r
                    where r.code = :code
                    """)
                    .param("userId", JdbcUuids.mysql(userId))
                    .param("actor", JdbcUuids.mysql(assignedByUserId))
                    .param("code", JdbcUuids.mysql(code))
                    .update();
        }
    }

    private static Set<String> splitRoles(String csv) {
        Set<String> roles = new LinkedHashSet<>();
        if (csv == null || csv.isBlank()) {
            return roles;
        }
        for (String part : csv.split(",")) {
            String code = part.trim();
            if (!code.isEmpty()) {
                roles.add(code);
            }
        }
        return roles;
    }

    private static Instant ts(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    public record StaffAccountRow(
            UUID id,
            String email,
            String displayName,
            String status,
            Set<String> roles,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record RoleRow(String code, String name, String description) {}
}
