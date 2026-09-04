package com.sarv.exhibitionportal.campaign;

import com.sarv.exhibitionportal.api.dto.CampaignDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CampaignRepository {

    private final JdbcClient jdbc;

    public CampaignRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CampaignDto> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return jdbc.sql("""
                        select id, code, label, landing_route, exhibition_id, is_active
                        from qr_campaigns
                        where upper(code) = upper(:code)
                        """)
                .param("code", JdbcUuids.mysql(code.trim()))
                .query((rs, n) -> new CampaignDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("code"),
                        rs.getString("label"),
                        rs.getString("landing_route"),
                        JdbcUuids.get(rs, "exhibition_id"),
                        rs.getBoolean("is_active")
                ))
                .optional();
    }

    public Optional<CampaignDto> findById(UUID id) {
        return jdbc.sql("""
                        select id, code, label, landing_route, exhibition_id, is_active
                        from qr_campaigns
                        where id = :id
                        """)
                .param("id", JdbcUuids.mysql(id))
                .query((rs, n) -> new CampaignDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("code"),
                        rs.getString("label"),
                        rs.getString("landing_route"),
                        JdbcUuids.get(rs, "exhibition_id"),
                        rs.getBoolean("is_active")
                ))
                .optional();
    }
}
