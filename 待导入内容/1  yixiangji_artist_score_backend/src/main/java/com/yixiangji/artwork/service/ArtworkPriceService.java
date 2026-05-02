package com.yixiangji.artwork.service;

import java.math.BigDecimal;

public interface ArtworkPriceService {
    BigDecimal dailyIncrease(Long artworkId);
    BigDecimal saleTriggerIncrease(Long artworkId);
    BigDecimal collectTriggerIncrease(Long artworkId);
}
