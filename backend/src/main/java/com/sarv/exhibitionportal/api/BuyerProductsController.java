package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.BuyerProductDto;
import com.sarv.exhibitionportal.trading.TradingCatalogueService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/buyer-products")
public class BuyerProductsController {

    private final TradingCatalogueService catalogue;

    public BuyerProductsController(TradingCatalogueService catalogue) {
        this.catalogue = catalogue;
    }

    @GetMapping
    public List<BuyerProductDto> list(@RequestParam(name = "q", required = false) String query) {
        return catalogue.listBuyerProducts(query);
    }
}
