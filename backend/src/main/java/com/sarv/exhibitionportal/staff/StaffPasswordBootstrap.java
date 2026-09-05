package com.sarv.exhibitionportal.staff;

import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(100)
public class StaffPasswordBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaffPasswordBootstrap.class);

    private final ExhibitionProperties properties;
    private final JdbcClient jdbc;
    private final PasswordEncoder encoder;
    private final Environment environment;

    public StaffPasswordBootstrap(
            ExhibitionProperties properties,
            JdbcClient jdbc,
            PasswordEncoder encoder,
            Environment environment
    ) {
        this.properties = properties;
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        String password = properties.staffBootstrapPassword();
        if (password == null || password.isBlank()) {
            if (prod) {
                // ProductionStartupGuard should have failed already; keep as belt-and-braces.
                throw new IllegalStateException(
                        "EXHIBITION_STAFF_BOOTSTRAP_PASSWORD is required in prod.");
            }
            return;
        }
        String hash = encoder.encode(password);
        int updated = jdbc.sql("""
                update app_users
                set password_hash = :hash, updated_at = current_timestamp
                where status = 'ACTIVE'
                """)
                .param("hash", JdbcUuids.mysql(hash))
                .update();
        log.info("Rotated password_hash for {} active staff users", updated);
    }
}
