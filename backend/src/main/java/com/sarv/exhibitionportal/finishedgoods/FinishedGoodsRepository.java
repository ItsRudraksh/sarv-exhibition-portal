package com.sarv.exhibitionportal.finishedgoods;

import com.sarv.exhibitionportal.api.dto.BuyerFinishedGoodDto;
import com.sarv.exhibitionportal.api.dto.FinishedGoodDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
/** Local snapshot of pharmadb products for the buy list. */
public class FinishedGoodsRepository {

    private final JdbcClient jdbc;

    public FinishedGoodsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<FinishedGoodDto> searchActive(String query, int limit) {
        int lim = Math.min(Math.max(limit, 1), 200);
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return jdbc.sql("""
                            select id, external_id, code, name
                            from finished_goods
                            where is_active = true
                            order by display_order, name
                            limit :lim
                            """)
                    .param("lim", lim)
                    .query((rs, n) -> new FinishedGoodDto(
                            JdbcUuids.get(rs, "id"),
                            rs.getLong("external_id"),
                            rs.getString("code"),
                            rs.getString("name")))
                    .list();
        }
        String like = "%" + q.toLowerCase() + "%";
        return jdbc.sql("""
                        select id, external_id, code, name
                        from finished_goods
                        where is_active = true
                          and (lower(name) like :q or lower(coalesce(code, '')) like :q)
                        order by display_order, name
                        limit :lim
                        """)
                .param("q", like)
                .param("lim", lim)
                .query((rs, n) -> new FinishedGoodDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getLong("external_id"),
                        rs.getString("code"),
                        rs.getString("name")))
                .list();
    }

    public int countActive() {
        Integer count = jdbc.sql("select count(*) from finished_goods where is_active = true")
                .query(Integer.class)
                .optional()
                .orElse(0);
        return count;
    }

    public UUID upsertFromExternal(long externalId, String code, String name, int displayOrder, Instant syncedAt) {
        UUID existing = jdbc.sql("select id from finished_goods where external_id = :ext")
                .param("ext", externalId)
                .query((rs, n) -> JdbcUuids.get(rs, "id"))
                .optional()
                .orElse(null);
        if (existing != null) {
            jdbc.sql("""
                     update finished_goods
                     set code = :code, name = :name, is_active = true,
                         display_order = :ord, synced_at = :synced, updated_at = CURRENT_TIMESTAMP(6)
                     where id = :id
                     """)
                    .param("code", code)
                    .param("name", name)
                    .param("ord", displayOrder)
                    .param("synced", java.sql.Timestamp.from(syncedAt))
                    .param("id", JdbcUuids.mysql(existing))
                    .update();
            return existing;
        }
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                 insert into finished_goods (id, external_id, code, name, is_active, display_order, synced_at)
                 values (:id, :ext, :code, :name, true, :ord, :synced)
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("ext", externalId)
                .param("code", code)
                .param("name", name)
                .param("ord", displayOrder)
                .param("synced", java.sql.Timestamp.from(syncedAt))
                .update();
        return id;
    }

    public int deactivateMissing(Set<Long> keepExternalIds, Instant syncedAt) {
        if (keepExternalIds.isEmpty()) {
            return jdbc.sql("""
                            update finished_goods
                            set is_active = false, synced_at = :synced, updated_at = CURRENT_TIMESTAMP(6)
                            where is_active = true
                            """)
                    .param("synced", java.sql.Timestamp.from(syncedAt))
                    .update();
        }
        // Deactivate rows whose external_id is not in the keep set (batched via NOT IN only when small).
        List<FinishedGoodDto> active = searchActive("", 10_000);
        int deactivated = 0;
        for (FinishedGoodDto row : active) {
            if (row.externalId() != null && !keepExternalIds.contains(row.externalId())) {
                jdbc.sql("""
                         update finished_goods
                         set is_active = false, synced_at = :synced, updated_at = CURRENT_TIMESTAMP(6)
                         where id = :id
                         """)
                        .param("synced", java.sql.Timestamp.from(syncedAt))
                        .param("id", JdbcUuids.mysql(row.id()))
                        .update();
                deactivated++;
            }
        }
        return deactivated;
    }

    public void replaceInquirySelections(UUID inquiryId, List<BuyerFinishedGoodDto> selections) {
        jdbc.sql("delete from purchase_inquiry_finished_goods where inquiry_id = :id")
                .param("id", JdbcUuids.mysql(inquiryId))
                .update();
        if (selections == null || selections.isEmpty()) {
            return;
        }
        Set<UUID> seen = new HashSet<>();
        for (BuyerFinishedGoodDto row : selections) {
            if (row == null || row.finishedGoodId() == null || !seen.add(row.finishedGoodId())) {
                continue;
            }
            String qty = row.quantity() == null ? "" : row.quantity().trim();
            jdbc.sql("""
                     insert into purchase_inquiry_finished_goods (inquiry_id, finished_good_id, quantity_text)
                     values (:inq, :fg, :qty)
                     """)
                    .param("inq", JdbcUuids.mysql(inquiryId))
                    .param("fg", JdbcUuids.mysql(row.finishedGoodId()))
                    .param("qty", qty)
                    .update();
        }
    }

    public List<BuyerFinishedGoodDto> selectionsForInquiry(UUID inquiryId) {
        return jdbc.sql("""
                        select finished_good_id, quantity_text
                        from purchase_inquiry_finished_goods
                        where inquiry_id = :id
                        """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .query((rs, n) -> new BuyerFinishedGoodDto(
                        JdbcUuids.get(rs, "finished_good_id"),
                        rs.getString("quantity_text") == null ? "" : rs.getString("quantity_text")
                ))
                .list();
    }

    public void insertSyncRun(UUID id, Instant startedAt, String state) {
        jdbc.sql("""
                 insert into finished_goods_sync_runs (id, started_at, state)
                 values (:id, :started, :state)
                 """)
                .param("id", JdbcUuids.mysql(id))
                .param("started", java.sql.Timestamp.from(startedAt))
                .param("state", state)
                .update();
    }

    public void finishSyncRun(
            UUID id, Instant finishedAt, String state, int upserted, int deactivated, String message
    ) {
        jdbc.sql("""
                 update finished_goods_sync_runs
                 set finished_at = :finished, state = :state,
                     rows_upserted = :up, rows_deactivated = :de, message = :msg
                 where id = :id
                 """)
                .param("finished", java.sql.Timestamp.from(finishedAt))
                .param("state", state)
                .param("up", upserted)
                .param("de", deactivated)
                .param("msg", message == null ? null : message.substring(0, Math.min(message.length(), 500)))
                .param("id", JdbcUuids.mysql(id))
                .update();
    }
}
