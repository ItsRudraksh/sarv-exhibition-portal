package com.sarv.exhibitionportal;

import static org.assertj.core.api.Assertions.assertThat;

import com.sarv.exhibitionportal.api.dto.FinishedGoodDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;

class FinishedGoodsApiTest extends MysqlSpringBootTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void listsActiveFinishedGoodsAndFiltersByQuery() {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                 insert into finished_goods (id, external_id, code, name, is_active, display_order)
                 values (:id, 9001, 'FG-9001', 'Thiocolchicoside API', true, 10)
                 """)
                .param("id", JdbcUuids.mysql(id))
                .update();
        jdbc.sql("""
                 insert into finished_goods (id, external_id, code, name, is_active, display_order)
                 values (:id, 9002, 'FG-9002', 'Hidden Inactive', false, 20)
                 """)
                .param("id", JdbcUuids.mysql(UUID.randomUUID()))
                .update();

        ResponseEntity<FinishedGoodDto[]> all = rest.getForEntity(
                "/api/v1/finished-goods", FinishedGoodDto[].class);
        assertThat(all.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(all.getBody()).isNotNull();
        assertThat(all.getBody())
                .extracting(FinishedGoodDto::name)
                .contains("Thiocolchicoside API")
                .doesNotContain("Hidden Inactive");

        ResponseEntity<FinishedGoodDto[]> filtered = rest.getForEntity(
                "/api/v1/finished-goods?q=thio", FinishedGoodDto[].class);
        assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(filtered.getBody()).isNotNull();
        assertThat(filtered.getBody()).extracting(FinishedGoodDto::code).contains("FG-9001");
    }
}
