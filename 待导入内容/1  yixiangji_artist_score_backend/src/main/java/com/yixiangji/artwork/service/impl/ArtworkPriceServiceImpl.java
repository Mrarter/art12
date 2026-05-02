package com.yixiangji.artwork.service.impl;

import com.yixiangji.artist.entity.ArtistScore;
import com.yixiangji.artist.mapper.ArtistScoreMapper;
import com.yixiangji.artwork.entity.Artwork;
import com.yixiangji.artwork.entity.ArtworkPriceLog;
import com.yixiangji.artwork.mapper.ArtworkMapper;
import com.yixiangji.artwork.mapper.ArtworkPriceLogMapper;
import com.yixiangji.artwork.service.ArtworkPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ArtworkPriceServiceImpl implements ArtworkPriceService {

    private final ArtworkMapper artworkMapper;
    private final ArtistScoreMapper artistScoreMapper;
    private final ArtworkPriceLogMapper priceLogMapper;

    @Override
    public BigDecimal dailyIncrease(Long artworkId) {
        Artwork artwork = artworkMapper.findById(artworkId);
        ArtistScore score = artistScoreMapper.findByArtistId(artwork.getArtistId());
        BigDecimal oldPrice = artwork.getCurrentPrice();
        BigDecimal rate = calculateDailyRate(score);
        BigDecimal newPrice = oldPrice.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
        updatePriceAndLog(artwork, oldPrice, newPrice, rate, "DAILY");
        return newPrice;
    }

    @Override
    public BigDecimal saleTriggerIncrease(Long artworkId) {
        Artwork artwork = artworkMapper.findById(artworkId);
        BigDecimal oldPrice = artwork.getCurrentPrice();
        BigDecimal rate = new BigDecimal("0.02");
        BigDecimal newPrice = oldPrice.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
        updatePriceAndLog(artwork, oldPrice, newPrice, rate, "SALE");
        return newPrice;
    }

    @Override
    public BigDecimal collectTriggerIncrease(Long artworkId) {
        Artwork artwork = artworkMapper.findById(artworkId);
        BigDecimal oldPrice = artwork.getCurrentPrice();
        BigDecimal rate = new BigDecimal("0.005");
        BigDecimal newPrice = oldPrice.multiply(BigDecimal.ONE.add(rate)).setScale(2, RoundingMode.HALF_UP);
        updatePriceAndLog(artwork, oldPrice, newPrice, rate, "COLLECT");
        return newPrice;
    }

    private BigDecimal calculateDailyRate(ArtistScore score) {
        if (score == null || score.getTotalScore() == null) return new BigDecimal("0.001");
        int totalScore = score.getTotalScore();
        BigDecimal baseRate;
        if (totalScore >= 900) baseRate = new BigDecimal("0.005");
        else if (totalScore >= 750) baseRate = new BigDecimal("0.004");
        else if (totalScore >= 500) baseRate = new BigDecimal("0.003");
        else if (totalScore >= 250) baseRate = new BigDecimal("0.002");
        else baseRate = new BigDecimal("0.001");
        return baseRate.min(new BigDecimal("0.005"));
    }

    private void updatePriceAndLog(Artwork artwork, BigDecimal oldPrice, BigDecimal newPrice, BigDecimal rate, String reason) {
        artworkMapper.updateCurrentPrice(artwork.getId(), newPrice);
        ArtworkPriceLog log = new ArtworkPriceLog();
        log.setArtworkId(artwork.getId());
        log.setArtistId(artwork.getArtistId());
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);
        log.setChangeRate(rate);
        log.setChangeReason(reason);
        log.setRemark("系统自动调价");
        priceLogMapper.insert(log);
    }
}
