package com.sarv.exhibitionportal.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Loads {@code portal.env.ps1} from the process working directory (WinSW {@code %BASE%}).
 * WinSW embeds env into XML at install time; editing the ps1 and {@code net start} otherwise
 * leaves stale JDBC flags, so staging buy-list stays empty.
 */
public class PortalEnvFileEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(PortalEnvFileEnvironmentPostProcessor.class);
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?m)^\\s*\\$env:([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*'([^']*)'");
    static final String PROPERTY_SOURCE_NAME = "portalEnvPs1";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path file = resolveEnvFile(environment);
        if (file == null || !Files.isRegularFile(file)) {
            return;
        }
        Map<String, Object> parsed;
        try {
            parsed = parse(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            log.warn("Could not read {}: {}", file.toAbsolutePath(), ex.getMessage());
            return;
        }
        if (parsed.isEmpty()) {
            return;
        }
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, parsed));
        log.info("Loaded {} env assignment(s) from {} (values not logged)", parsed.size(), file.toAbsolutePath());
    }

    static Path resolveEnvFile(ConfigurableEnvironment environment) {
        String override = environment.getProperty("EXHIBITION_PORTAL_ENV_FILE");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return Path.of("portal.env.ps1");
    }

    static Map<String, Object> parse(String text) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return map;
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            Matcher matcher = ASSIGNMENT.matcher(line);
            if (matcher.find()) {
                map.put(matcher.group(1), matcher.group(2));
            }
        }
        return map;
    }
}
