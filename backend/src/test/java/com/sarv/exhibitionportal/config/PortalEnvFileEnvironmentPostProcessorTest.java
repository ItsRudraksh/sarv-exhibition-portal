package com.sarv.exhibitionportal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class PortalEnvFileEnvironmentPostProcessorTest {

    @Test
    void parseIgnoresCommentsAndReadsAssignments() {
        Map<String, Object> parsed = PortalEnvFileEnvironmentPostProcessor.parse("""
                # $env:EXHIBITION_PHARMA_ERP_ENABLED = 'false'
                $env:EXHIBITION_PHARMA_ERP_ENABLED = 'true'
                $env:EXHIBITION_PHARMA_ERP_JDBC_URL = 'jdbc:mysql://127.0.0.1:3306/pharmadb'
                $env:EXHIBITION_PHARMA_ERP_PASSWORD = 'secret'
                """);
        assertThat(parsed)
                .containsEntry("EXHIBITION_PHARMA_ERP_ENABLED", "true")
                .containsEntry("EXHIBITION_PHARMA_ERP_JDBC_URL", "jdbc:mysql://127.0.0.1:3306/pharmadb")
                .containsEntry("EXHIBITION_PHARMA_ERP_PASSWORD", "secret")
                .hasSize(3);
    }
}
