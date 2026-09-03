package com.sarv.exhibitionportal.audit;

import com.sarv.exhibitionportal.config.JdbcUuids;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {

    private final JdbcClient jdbc;
    private final ObjectMapper mapper;

    public AuditRepository(JdbcClient jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    public void insert(
            UUID inquiryId,
            String entityType,
            UUID entityId,
            String eventType,
            String actorKind,
            UUID actorUserId,
            Map<String, Object> metadata
    ) {
        jdbc.sql("""
                 insert into audit_events (
                     id, inquiry_id, entity_type, entity_id, event_type, actor_kind, actor_user_id, metadata
                 ) values (
                     :id, :inquiry, :etype, :eid, :event, :actor, :user, :meta
                 )
                 """)
                .param("id", JdbcUuids.mysql(UUID.randomUUID()))
                .param("inquiry", JdbcUuids.mysql(inquiryId))
                .param("etype", JdbcUuids.mysql(entityType))
                .param("eid", JdbcUuids.mysql(entityId))
                .param("event", JdbcUuids.mysql(eventType))
                .param("actor", JdbcUuids.mysql(actorKind))
                .param("user", JdbcUuids.mysql(actorUserId))
                .param("meta", JdbcUuids.mysql(json(metadata)))
                .update();
    }

    public void insert(
            UUID inquiryId,
            String entityType,
            UUID entityId,
            String eventType,
            String actorKind,
            Map<String, Object> metadata
    ) {
        insert(inquiryId, entityType, entityId, eventType, actorKind, null, metadata);
    }

    public long countByInquiryAndEvent(UUID inquiryId, String eventType) {
        Long count = jdbc.sql("""
                              select count(*) from audit_events
                              where inquiry_id = :id and event_type = :event
                              """)
                .param("id", JdbcUuids.mysql(inquiryId))
                .param("event", JdbcUuids.mysql(eventType))
                .query(Long.class)
                .single();
        return count == null ? 0 : count;
    }

    private String json(Map<String, Object> metadata) {
        try {
            return mapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }
}
