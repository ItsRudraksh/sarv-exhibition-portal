package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.DepartmentDto;
import com.sarv.exhibitionportal.api.dto.ProductTypeDto;
import com.sarv.exhibitionportal.taxonomy.TaxonomyRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/taxonomy")
public class TaxonomyController {

    private final TaxonomyRepository taxonomy;

    public TaxonomyController(TaxonomyRepository taxonomy) {
        this.taxonomy = taxonomy;
    }

    @GetMapping("/departments")
    public List<DepartmentDto> departments() {
        return taxonomy.departments();
    }

    @GetMapping("/product-types")
    public List<ProductTypeDto> productTypes(
            @RequestParam(name = "departmentIds", required = false) List<UUID> departmentIds
    ) {
        List<ProductTypeDto> all = taxonomy.productTypes();
        if (departmentIds == null || departmentIds.isEmpty()) {
            return all;
        }
        return all.stream()
                .filter(pt -> pt.departmentIds().stream().anyMatch(departmentIds::contains))
                .toList();
    }
}
