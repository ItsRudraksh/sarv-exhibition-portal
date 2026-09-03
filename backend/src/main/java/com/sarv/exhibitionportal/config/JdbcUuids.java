package com.sarv.exhibitionportal.config;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class JdbcUuids {

    private JdbcUuids() {
    }

    public static UUID get(ResultSet rs, String column) throws SQLException {
        return parse(rs.getString(column));
    }

    public static UUID parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }

    public static Optional<UUID> optional(String value) {
        UUID parsed = parse(value);
        return parsed == null ? Optional.empty() : Optional.of(parsed);
    }

    /** MySQL CHAR(36) cannot bind {@link UUID} as a binary Java object. */
    public static Object mysql(Object value) {
        if (value instanceof UUID uuid) {
            return uuid.toString();
        }
        return value;
    }
}
