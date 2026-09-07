package com.sarv.exhibitionportal.finishedgoods;

import com.sarv.exhibitionportal.api.dto.FinishedGoodDto;
import com.sarv.exhibitionportal.api.dto.FinishedGoodsSyncResultDto;
import com.sarv.exhibitionportal.config.ExhibitionProperties;
import com.sarv.exhibitionportal.inquiry.InquiryValidationException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinishedGoodsService {

    private static final Logger log = LoggerFactory.getLogger(FinishedGoodsService.class);

    private final FinishedGoodsRepository repository;
    private final ExhibitionProperties properties;

    public FinishedGoodsService(FinishedGoodsRepository repository, ExhibitionProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<FinishedGoodDto> list(String query) {
        return repository.searchActive(query, 200);
    }

    @Transactional(readOnly = true)
    public int activeCount() {
        return repository.countActive();
    }

    /**
     * Manual (and scheduled) DB sync: read active {@code products} from pharma-erp MySQL and upsert
     * into {@code finished_goods}. Does not invent catalogue rows when pharma-erp is disabled.
     */
    @Transactional
    public FinishedGoodsSyncResultDto syncFromPharmaErp() {
        ExhibitionProperties.PharmaErp cfg = properties.pharmaErp();
        if (!cfg.enabled()) {
            throw new InquiryValidationException(
                    "Pharma-erp sync is disabled. Set exhibition.pharma-erp.enabled=true and JDBC settings.");
        }
        if (cfg.jdbcUrl().isBlank()) {
            throw new InquiryValidationException("exhibition.pharma-erp.jdbc-url is required for sync.");
        }

        UUID runId = UUID.randomUUID();
        Instant started = Instant.now();
        repository.insertSyncRun(runId, started, "RUNNING");

        Set<Long> seen = new HashSet<>();
        int upserted = 0;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection connection = DriverManager.getConnection(
                    cfg.jdbcUrl(), cfg.username(), cfg.password());
                 PreparedStatement ps = connection.prepareStatement("""
                         select p.id, p.product_code, p.name
                         from products p
                         where p.active = 1
                         order by p.name
                         """);
                 ResultSet rs = ps.executeQuery()) {
                int order = 10;
                while (rs.next()) {
                    long externalId = rs.getLong("id");
                    String code = rs.getString("product_code");
                    String name = rs.getString("name");
                    if (name == null || name.isBlank()) {
                        continue;
                    }
                    seen.add(externalId);
                    repository.upsertFromExternal(
                            externalId,
                            code == null || code.isBlank() ? null : code.trim(),
                            name.trim(),
                            order,
                            started);
                    upserted++;
                    order += 10;
                }
            }
            int deactivated = repository.deactivateMissing(seen, started);
            Instant finished = Instant.now();
            String message = "Synced from pharma-erp products (active).";
            repository.finishSyncRun(runId, finished, "SUCCEEDED", upserted, deactivated, message);
            log.info("Finished-goods sync succeeded: upserted={}, deactivated={}", upserted, deactivated);
            return new FinishedGoodsSyncResultDto("SUCCEEDED", upserted, deactivated, message);
        } catch (InquiryValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            repository.finishSyncRun(runId, Instant.now(), "FAILED", upserted, 0, message);
            log.warn("Finished-goods sync failed: {}", message);
            throw new InquiryValidationException("Finished-goods sync failed: " + message);
        }
    }

    @Scheduled(cron = "${exhibition.pharma-erp.sync-cron:0 0 2 * * MON}")
    public void scheduledSync() {
        ExhibitionProperties.PharmaErp cfg = properties.pharmaErp();
        if (!cfg.enabled() || !cfg.scheduleEnabled()) {
            return;
        }
        try {
            syncFromPharmaErp();
        } catch (Exception ex) {
            log.warn("Scheduled finished-goods sync skipped/failed: {}", ex.getMessage());
        }
    }
}
