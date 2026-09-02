package com.sarv.exhibitionportal.consent;

import com.sarv.exhibitionportal.api.dto.ConsentDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ConsentRepository {

    private final JdbcClient jdbc;

    public ConsentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(
            UUID id,
            UUID inquiryId,
            String purpose,
            String policyVersion,
            String decision,
            Instant revokedAt
    ) {
        jdbc.sql("""
                 insert into consent_records (
                     id, inquiry_id, purpose, policy_version, decision, revoked_at
                 ) values (
                     :id, :inquiry, :purpose, :policy, :decision, :revoked
                 )
                 """)
                .param("id", id)
                .param("inquiry", inquiryId)
                .param("purpose", purpose)
                .param("policy", policyVersion)
                .param("decision", decision)
                .param("revoked", revokedAt == null ? null : Timestamp.from(revokedAt))
                .update();
    }

    public Optional<ConsentDto> latest(UUID inquiryId, String purpose) {
        return jdbc.sql("""
                        select id, purpose, policy_version, decision, decided_at
                        from consent_records
                        where inquiry_id = :id and purpose = :purpose
                        order by decided_at desc
                        limit 1
                        """)
                .param("id", inquiryId)
                .param("purpose", purpose)
                .query((rs, n) -> new ConsentDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("purpose"),
                        rs.getString("policy_version"),
                        rs.getString("decision"),
                        rs.getTimestamp("decided_at").toInstant()
                ))
                .optional();
    }

    public List<ConsentDto> list(UUID inquiryId) {
        return jdbc.sql("""
                        select id, purpose, policy_version, decision, decided_at
                        from consent_records
                        where inquiry_id = :id
                        order by decided_at desc
                        """)
                .param("id", inquiryId)
                .query((rs, n) -> new ConsentDto(
                        rs.getObject("id", UUID.class),
                        rs.getString("purpose"),
                        rs.getString("policy_version"),
                        rs.getString("decision"),
                        rs.getTimestamp("decided_at").toInstant()
                ))
                .list();
    }
}
