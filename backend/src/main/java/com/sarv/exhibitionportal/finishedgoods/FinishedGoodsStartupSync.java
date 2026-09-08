package com.sarv.exhibitionportal.finishedgoods;

import com.sarv.exhibitionportal.config.ExhibitionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Staging/prod buy list is a MySQL snapshot. Git does not copy it. When JDBC is enabled,
 * pull pharmadb on boot so Staff click is not the only way to fill an empty server.
 */
@Component
@Order(200)
public class FinishedGoodsStartupSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FinishedGoodsStartupSync.class);

    private final ExhibitionProperties properties;
    private final FinishedGoodsService finishedGoods;

    public FinishedGoodsStartupSync(ExhibitionProperties properties, FinishedGoodsService finishedGoods) {
        this.properties = properties;
        this.finishedGoods = finishedGoods;
    }

    @Override
    public void run(ApplicationArguments args) {
        ExhibitionProperties.PharmaErp cfg = properties.pharmaErp();
        if (cfg == null || !cfg.enabled() || cfg.jdbcUrl().isBlank()) {
            log.info("Startup finished-goods sync skipped (pharma-erp disabled or jdbc-url empty)");
            return;
        }
        try {
            var result = finishedGoods.syncFromPharmaErp();
            log.info(
                    "Startup finished-goods sync {}: upserted={}, deactivated={}",
                    result.state(),
                    result.rowsUpserted(),
                    result.rowsDeactivated());
        } catch (Exception ex) {
            log.warn("Startup finished-goods sync failed (buy list stays empty): {}", ex.getMessage());
        }
    }
}
