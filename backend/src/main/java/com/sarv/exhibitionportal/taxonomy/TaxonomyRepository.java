package com.sarv.exhibitionportal.taxonomy;

import com.sarv.exhibitionportal.api.dto.DepartmentDto;
import com.sarv.exhibitionportal.api.dto.ProductTypeDto;
import com.sarv.exhibitionportal.config.JdbcUuids;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TaxonomyRepository {

    private final JdbcClient jdbc;

    public TaxonomyRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<DepartmentDto> departments() {
        return jdbc.sql("""
                        select id, code, name
                        from departments
                        where is_active = true
                        order by display_order, name
                        """)
                .query((rs, n) -> new DepartmentDto(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("code"),
                        rs.getString("name")))
                .list();
    }

    public List<ProductTypeDto> productTypes() {
        List<TypeRow> rows = jdbc.sql("""
                select pt.id, pt.code, pt.name, dpt.department_id
                from product_types pt
                join department_product_types dpt
                  on dpt.product_type_id = pt.id and dpt.is_active = true
                where pt.is_active = true
                order by pt.display_order, pt.name
                """)
                .query((rs, n) -> new TypeRow(
                        JdbcUuids.get(rs, "id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        JdbcUuids.get(rs, "department_id")))
                .list();
        Map<UUID, ProductTypeDto> byId = new LinkedHashMap<>();
        for (TypeRow row : rows) {
            ProductTypeDto existing = byId.get(row.id());
            if (existing == null) {
                List<UUID> depts = new ArrayList<>();
                depts.add(row.departmentId());
                byId.put(row.id(), new ProductTypeDto(row.id(), row.code(), row.name(), depts));
            } else if (!existing.departmentIds().contains(row.departmentId())) {
                existing.departmentIds().add(row.departmentId());
            }
        }
        return new ArrayList<>(byId.values());
    }

    private record TypeRow(UUID id, String code, String name, UUID departmentId) {}
}
