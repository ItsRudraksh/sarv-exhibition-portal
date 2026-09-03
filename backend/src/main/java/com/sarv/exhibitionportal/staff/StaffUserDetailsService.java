package com.sarv.exhibitionportal.staff;

import com.sarv.exhibitionportal.config.JdbcUuids;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class StaffUserDetailsService implements UserDetailsService {

    private final JdbcClient jdbc;

    public StaffUserDetailsService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String email = username == null ? "" : username.trim().toLowerCase();
        List<StaffRow> rows = jdbc.sql("""
                select u.id, u.email_normalized, u.display_name, u.password_hash, u.status, r.code
                from app_users u
                join user_roles ur on ur.user_id = u.id
                join roles r on r.id = ur.role_id
                where u.email_normalized = :email
                """)
                .param("email", JdbcUuids.mysql(email))
                .query((rs, n) -> new StaffRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("email_normalized"),
                        rs.getString("display_name"),
                        rs.getString("password_hash"),
                        rs.getString("status"),
                        rs.getString("code")
                ))
                .list();
        if (rows.isEmpty()) {
            throw new UsernameNotFoundException("Unknown staff user");
        }
        StaffRow first = rows.get(0);
        Set<String> roles = new HashSet<>();
        for (StaffRow row : rows) {
            roles.add(row.roleCode());
        }
        return new StaffUser(
                first.id(),
                first.email(),
                first.displayName(),
                first.passwordHash(),
                roles,
                "ACTIVE".equals(first.status())
        );
    }

    private record StaffRow(
            UUID id,
            String email,
            String displayName,
            String passwordHash,
            String status,
            String roleCode
    ) {}
}
