package com.sarv.exhibitionportal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.config.ProductionStartupGuard;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionStartupGuardTest {

    @Test
    void prodRejectsMissingStaffPassword() {
        ExhibitionProperties props = props(false, "");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionStartupGuard guard = new ProductionStartupGuard(props, env);
        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EXHIBITION_STAFF_BOOTSTRAP_PASSWORD");
    }

    @Test
    void prodRejectsPocFlag() {
        ExhibitionProperties props = props(true, "unique-prod-password-1");
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        ProductionStartupGuard guard = new ProductionStartupGuard(props, env);
        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exhibition.poc");
    }

    @Test
    void nonProdAllowsEmptyBootstrap() throws Exception {
        ExhibitionProperties props = props(true, "");
        MockEnvironment env = new MockEnvironment();
        ProductionStartupGuard guard = new ProductionStartupGuard(props, env);
        guard.run(new DefaultApplicationArguments());
    }

    private static ExhibitionProperties props(boolean poc, String staffPassword) {
        return new ExhibitionProperties(
                poc,
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                List.of("http://localhost"),
                "./var",
                730,
                1024,
                2048,
                "card-extraction-v1",
                24,
                new ExhibitionProperties.Outbox(true, 5, 30, "local-mailbox", "local-vendor-stub", ""),
                staffPassword,
                poc ? "POC-" : "EP-"
        );
    }
}
