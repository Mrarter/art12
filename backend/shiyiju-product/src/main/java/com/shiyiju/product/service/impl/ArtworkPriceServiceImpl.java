package com.shiyiju.product.service.impl;

import com.shiyiju.product.entity.ArtistScore;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.entity.ArtworkPriceLog;
import com.shiyiju.product.mapper.ArtistScoreMapper;
import com.shiyiju.product.mapper.ArtworkMapper;
import com.shiyiju.product.mapper.ArtworkPriceLogMapper;
import com.shiyiju.product.service.ArtworkPriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ArtworkPriceServiceImpl implements ArtworkPriceService {

    private final ArtworkMapper artworkMapper;
    private final ArtistScoreMapper artistScoreMapper;
    private final ArtworkPriceLogMapper priceLogMapper;

    @Override
    @Transactional
    public Long dailyIncrease(Long artworkId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) throw new RuntimeException("作品不存在");

        ArtistScore score = null;
        if (artwork.getAuthorId() != null) {
            score = artistScoreMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ArtistScore>()
                    .eq(ArtistScore::getArtistId, artwork.getAuthorId()));
        }
        BigDecimal rate = calculateDailyRate(score);
        return updatePriceAndLog(artwork, rate, "DAILY", "系统每日涨价");
    }

    @Override
    @Transactional
    public Long saleTriggerIncrease(Long artworkId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) throw new RuntimeException("作品不存在");
        return updatePriceAndLog(artwork, new BigDecimal("0.02"), "SALE", "成交触发涨价");
    }

    @Override
    @Transactional
    public Long collectTriggerIncrease(Long artworkId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) throw new RuntimeException("作品不存在");
        return updatePriceAndLog(artwork, new BigDecimal("0.005"), "COLLECT", "收藏触发涨价");
    }

    @Override
    @Transactional
    public Long manualAdjust(Long artworkId, Long newPrice, String reason, Long operatorId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) throw new RuntimeException("作品不存在");

        Long oldPrice = artwork.getPrice();
        BigDecimal rate = newPrice != null && oldPrice != null && oldPrice > 0
                ? BigDecimal.valueOf(newPrice - oldPrice).divide(BigDecimal.valueOf(oldPrice), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        artwork.setPrice(newPrice);
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);

        ArtworkPriceLog log = new ArtworkPriceLog();
        log.setArtworkId(artworkId);
        log.setArtistId(artwork.getAuthorId());
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);
        log.setChangeRate(rate);
        log.setChangeReason("MANUAL");
        log.setRemark(reason);
        log.setOperatorId(operatorId);
        log.setCreatedAt(LocalDateTime.now());
        priceLogMapper.insert(log);

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

    private Long updatePriceAndLog(Artwork artwork, BigDecimal rate, String reason, String remark) {
        Long oldPrice = artwork.getPrice() != null ? artwork.getPrice() : 0L;
        BigDecimal newPriceBD = BigDecimal.valueOf(oldPrice)
                .multiply(BigDecimal.ONE.add(rate))
                .setScale(0, RoundingMode.HALF_UP);
        Long newPrice = newPriceBD.longValue();

        artwork.setPrice(newPrice);
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);

        ArtworkPriceLog log = new ArtworkPriceLog();
        log.setArtworkId(artwork.getId());
        log.setArtistId(artwork.getAuthorId());
        log.setOldPrice(oldPrice);
        log.setNewPrice(newPrice);
        log.setChangeRate(rate);
        log.setChangeReason(reason);
        log.setRemark(remark);
        log.setCreatedAt(LocalDateTime.now());
        priceLogMapper.insert(log);

        return newPrice;
    }
}
