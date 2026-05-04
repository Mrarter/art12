package com.shiyiju.product.controller;

import com.shiyiju.common.result.Result;
import com.shiyiju.product.service.ArtworkPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/artwork/price")
@RequiredArgsConstructor
public class ArtworkPriceController {

    private final ArtworkPriceService artworkPriceService;

    @PostMapping("/daily/{artworkId}")
    public Result<Long> dailyIncrease(@PathVariable Long artworkId) {
        return Result.success(artworkPriceService.dailyIncrease(artworkId));
    }

    @PostMapping("/sale/{artworkId}")
    public Result<Long> saleTriggerIncrease(@PathVariable Long artworkId) {
        return Result.success(artworkPriceService.saleTriggerIncrease(artworkId));
    }

    @PostMapping("/collect/{artworkId}")
    public Result<Long> collectTriggerIncrease(@PathVariable Long artworkId) {
        return Result.success(artworkPriceService.collectTriggerIncrease(artworkId));
    }
}
