package com.yixiangji.artwork.controller;

import com.yixiangji.artwork.service.ArtworkPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/artwork/price")
@RequiredArgsConstructor
public class ArtworkPriceController {
    private final ArtworkPriceService artworkPriceService;

    @PostMapping("/daily/{artworkId}")
    public BigDecimal dailyIncrease(@PathVariable Long artworkId) {
        return artworkPriceService.dailyIncrease(artworkId);
    }

    @PostMapping("/sale/{artworkId}")
    public BigDecimal saleTriggerIncrease(@PathVariable Long artworkId) {
        return artworkPriceService.saleTriggerIncrease(artworkId);
    }

    @PostMapping("/collect/{artworkId}")
    public BigDecimal collectTriggerIncrease(@PathVariable Long artworkId) {
        return artworkPriceService.collectTriggerIncrease(artworkId);
    }
}
