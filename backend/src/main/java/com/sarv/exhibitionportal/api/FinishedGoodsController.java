package com.sarv.exhibitionportal.api;

import com.sarv.exhibitionportal.api.dto.FinishedGoodDto;
import com.sarv.exhibitionportal.finishedgoods.FinishedGoodsService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public list of active finished-goods snapshot rows (also folded into buyer-products). */
@RestController
@RequestMapping("/api/v1/finished-goods")
public class FinishedGoodsController {

    private final FinishedGoodsService finishedGoods;

    public FinishedGoodsController(FinishedGoodsService finishedGoods) {
        this.finishedGoods = finishedGoods;
    }

    @GetMapping
    public List<FinishedGoodDto> list(@RequestParam(name = "q", required = false) String query) {
        return finishedGoods.list(query);
    }
}
