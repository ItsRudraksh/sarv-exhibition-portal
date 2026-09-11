package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.CreateOfflineSupplierRequest;
import com.sarv.exhibitionportal.api.dto.LinkPortalSupplierRequest;
import com.sarv.exhibitionportal.api.dto.PortalSupplierCandidateDto;
import com.sarv.exhibitionportal.api.dto.TradingProductDto;
import com.sarv.exhibitionportal.api.dto.TradingSupplierDto;
import com.sarv.exhibitionportal.api.dto.UpdateTradingSupplierRequest;
import com.sarv.exhibitionportal.api.dto.UpsertTradingProductRequest;
import com.sarv.exhibitionportal.staff.StaffUser;
import com.sarv.exhibitionportal.trading.TradingCatalogueService;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin buyer-catalogue: offline suppliers, link portal suppliers, tag products
 * {@code listed_for_buyers}. Linking is not Add to production.
 */
@RestController
@RequestMapping("/api/v1/staff/trading-suppliers")
public class TradingSuppliersController {

    private final TradingCatalogueService trading;

    public TradingSuppliersController(TradingCatalogueService trading) {
        this.trading = trading;
    }

    @GetMapping
    public List<TradingSupplierDto> list() {
        return trading.listSuppliers();
    }

    @GetMapping("/portal-candidates")
    public List<PortalSupplierCandidateDto> portalCandidates() {
        return trading.portalCandidates();
    }

    @GetMapping("/{id}")
    public TradingSupplierDto get(@PathVariable UUID id) {
        return trading.getSupplier(id);
    }

    @PostMapping
    public ResponseEntity<TradingSupplierDto> createOffline(
            @RequestBody CreateOfflineSupplierRequest body,
            Authentication authentication
    ) {
        TradingSupplierDto created = trading.createOffline(body, actor(authentication));
        return ResponseEntity.created(URI.create("/api/v1/staff/trading-suppliers/" + created.id())).body(created);
    }

    @PostMapping("/from-portal")
    public TradingSupplierDto linkPortal(
            @RequestBody LinkPortalSupplierRequest body,
            Authentication authentication
    ) {
        return trading.linkPortal(body, actor(authentication));
    }

    @PutMapping("/{id}")
    public TradingSupplierDto update(
            @PathVariable UUID id,
            @RequestBody UpdateTradingSupplierRequest body,
            Authentication authentication
    ) {
        return trading.updateSupplier(id, body, actor(authentication));
    }

    @DeleteMapping("/{id}")
    public TradingSupplierDto deactivate(@PathVariable UUID id, Authentication authentication) {
        return trading.deactivateSupplier(id, actor(authentication));
    }

    @PostMapping("/{id}/products")
    public ResponseEntity<TradingProductDto> addProduct(
            @PathVariable UUID id,
            @RequestBody UpsertTradingProductRequest body,
            Authentication authentication
    ) {
        TradingProductDto created = trading.addProduct(id, body, actor(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}/products/{productId}")
    public TradingProductDto updateProduct(
            @PathVariable UUID id,
            @PathVariable UUID productId,
            @RequestBody UpsertTradingProductRequest body,
            Authentication authentication
    ) {
        return trading.updateProduct(id, productId, body, actor(authentication));
    }

    @DeleteMapping("/{id}/products/{productId}")
    public TradingProductDto deactivateProduct(
            @PathVariable UUID id,
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        return trading.deactivateProduct(id, productId, actor(authentication));
    }

    private static StaffUser actor(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof StaffUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff sign-in required");
    }
}
