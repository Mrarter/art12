package com.shiyiju.product.service;

import com.shiyiju.product.entity.Artwork;

public interface ArtworkPriceService {
    Long dailyIncrease(Long artworkId);
    Long saleTriggerIncrease(Long artworkId);
    Long collectTriggerIncrease(Long artworkId);
    Long manualAdjust(Long artworkId, Long newPrice, String reason, Long operatorId);
}
