package com.shiyiju.auction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.auction.entity.*;
import com.shiyiju.auction.mapper.*;
import com.shiyiju.auction.websocket.AuctionWebSocketHub;
import com.shiyiju.common.constant.AuctionConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuctionBidService {
    private final AuctionLotMapper lotMapper;
    private final AuctionBidMapper bidMapper;
    private final AuctionDepositMapper depositMapper;
    private final AuctionWebSocketHub webSocketHub;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> place(Long lotId, Long userId, BigDecimal price, String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 64) {
            throw new IllegalArgumentException("缺少有效的 requestId");
        }
        AuctionBid repeated = bidMapper.selectOne(new LambdaQueryWrapper<AuctionBid>()
                .eq(AuctionBid::getUserId, userId).eq(AuctionBid::getRequestId, requestId));
        if (repeated != null) return bidResult(repeated, null, true);

        AuctionLot lot = lotMapper.selectByIdForUpdate(lotId);
        if (lot == null) throw new BusinessException(ResultCode.NOT_FOUND);
        LocalDateTime now = LocalDateTime.now();
        if (lot.getStartTime() == null || now.isBefore(lot.getStartTime())
                || lot.getEndTime() == null || !now.isBefore(lot.getEndTime())
                || AuctionConstant.LOT_STATUS_SOLD.equals(lot.getStatus())
                || AuctionConstant.LOT_STATUS_UNSOLD.equals(lot.getStatus())) {
            throw new BusinessException(ResultCode.AUCTION_ENDED);
        }
        AuctionDeposit deposit = depositMapper.selectOne(new LambdaQueryWrapper<AuctionDeposit>()
                .eq(AuctionDeposit::getSessionId, lot.getSessionId())
                .eq(AuctionDeposit::getUserId, userId)
                .eq(AuctionDeposit::getPayStatus, AuctionConstant.DEPOSIT_PAID));
        if (deposit == null) throw new BusinessException(ResultCode.AUCTION_DEPOSIT_REQUIRED);

        BigDecimal current = lot.getCurrentPrice() == null || lot.getCurrentPrice().signum() == 0
                ? lot.getStartPrice() : lot.getCurrentPrice();
        BigDecimal increment = lot.getIncrement() == null ? BigDecimal.ZERO : lot.getIncrement();
        BigDecimal minimum = (lot.getBidCount() == null || lot.getBidCount() == 0) ? current : current.add(increment);
        if (price == null || price.scale() > 2 || price.compareTo(minimum) < 0) {
            throw new BusinessException(ResultCode.AUCTION_BID_TOO_LOW);
        }
        if (increment.signum() > 0 && price.subtract(current).remainder(increment).signum() != 0) {
            throw new IllegalArgumentException("出价必须按加价幅度递增");
        }

        bidMapper.clearLeadingBid(lotId);
        AuctionBid bid = new AuctionBid();
        bid.setLotId(lotId); bid.setUserId(userId); bid.setBidPrice(price.setScale(2));
        bid.setBidTime(now); bid.setStatus(1); bid.setRequestId(requestId);
        try { bidMapper.insert(bid); }
        catch (DuplicateKeyException e) {
            AuctionBid existing = bidMapper.selectOne(new LambdaQueryWrapper<AuctionBid>()
                    .eq(AuctionBid::getUserId, userId).eq(AuctionBid::getRequestId, requestId));
            if (existing != null) return bidResult(existing, lot, true);
            throw e;
        }
        lot.setCurrentPrice(price.setScale(2));
        lot.setBidCount((lot.getBidCount() == null ? 0 : lot.getBidCount()) + 1);
        lot.setBuyerId(userId); lot.setStatus(AuctionConstant.LOT_STATUS_ONGOING); lot.setUpdateTime(now);
        lotMapper.updateById(lot);

        Map<String, Object> result = bidResult(bid, lot, false);
        webSocketHub.broadcast(lotId, Map.of("type", "bid", "data", result));
        return result;
    }

    private Map<String, Object> bidResult(AuctionBid bid, AuctionLot lot, boolean idempotent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bidId", bid.getId()); result.put("lotId", bid.getLotId());
        result.put("price", bid.getBidPrice()); result.put("bidPrice", bid.getBidPrice());
        result.put("userId", bid.getUserId()); result.put("bidTime", bid.getBidTime());
        result.put("bidCount", lot == null ? null : lot.getBidCount()); result.put("idempotent", idempotent);
        return result;
    }
}
