package com.shiyiju.auction.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.AuctionConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.auction.entity.*;
import com.shiyiju.auction.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auction")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionSessionMapper sessionMapper;
    private final AuctionLotMapper lotMapper;
    private final AuctionBidMapper bidMapper;
    private final AuctionDepositMapper depositMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 获取拍卖专场列表 (GET /auction/sessions)
     */
    @GetMapping("/sessions")
    public Result<PageResult<AuctionSession>> getSessions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status
    ) {
        LambdaQueryWrapper<AuctionSession> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(AuctionSession::getStatus, status);
        } else {
            wrapper.in(AuctionSession::getStatus, AuctionConstant.SESSION_STATUS_COMING, 
                    AuctionConstant.SESSION_STATUS_ONGOING);
        }
        wrapper.orderByAsc(AuctionSession::getStartTime);
        
        Page<AuctionSession> result = sessionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
    }

    /**
     * 获取专场详情及拍品 (GET /auction/sessions/{session_id})
     */
    @GetMapping("/sessions/{sessionId}")
    public Result<Map<String, Object>> getSessionDetail(@PathVariable Long sessionId) {
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        
        // 获取专场下的拍品
        List<AuctionLot> lots = lotMapper.selectList(
                new LambdaQueryWrapper<AuctionLot>()
                        .eq(AuctionLot::getSessionId, sessionId)
                        .orderByAsc(AuctionLot::getLotNo)
        );
        
        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        result.put("lots", lots);
        
        return Result.success(result);
    }

    /**
     * 获取专场下的拍品 (GET /auction/sessions/{session_id}/lots)
     */
    @GetMapping("/sessions/{sessionId}/lots")
    public Result<List<AuctionLot>> getSessionLots(@PathVariable Long sessionId) {
        return Result.success(lotMapper.selectList(
                new LambdaQueryWrapper<AuctionLot>()
                        .eq(AuctionLot::getSessionId, sessionId)
                        .orderByAsc(AuctionLot::getLotNo)
        ));
    }

    /**
     * 获取拍品详情 (GET /auction/lots/{lot_id})
     */
    @GetMapping("/lots/{lotId}")
    public Result<AuctionLotVO> getLotDetail(@PathVariable Long lotId) {
        AuctionLot lot = lotMapper.selectById(lotId);
        if (lot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        
        AuctionLotVO vo = new AuctionLotVO();
        vo.setId(lot.getId());
        vo.setSessionId(lot.getSessionId());
        vo.setLotNo(lot.getLotNo());
        vo.setStartPrice(lot.getStartPrice());
        vo.setCurrentPrice(lot.getCurrentPrice());
        vo.setBidIncrement(lot.getBidIncrement());
        vo.setBidCount(lot.getBidCount());
        vo.setDepositAmount(lot.getDepositAmount());
        vo.setStatus(lot.getStatus());
        vo.setStartTime(lot.getStartTime() != null ? lot.getStartTime().toString() : null);
        vo.setEndTime(lot.getEndTime() != null ? lot.getEndTime().toString() : null);
        
        // 获取出价记录
        List<AuctionBid> bids = bidMapper.selectList(
                new LambdaQueryWrapper<AuctionBid>()
                        .eq(AuctionBid::getLotId, lotId)
                        .orderByDesc(AuctionBid::getBidTime)
                        .last("LIMIT 10")
        );
        vo.setRecentBids(bids);
        
        return Result.success(vo);
    }

    /**
     * 缴纳保证金 (POST /auction/sessions/{session_id}/deposit)
     */
    @Transactional
    @PostMapping("/sessions/{sessionId}/deposit")
    public Result<Void> payDeposit(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        // 获取该专场下需要缴纳保证金的拍品
        List<AuctionLot> lots = lotMapper.selectList(
                new LambdaQueryWrapper<AuctionLot>()
                        .eq(AuctionLot::getSessionId, sessionId)
                        .eq(AuctionLot::getStatus, AuctionConstant.LOT_STATUS_ONGOING)
        );
        
        for (AuctionLot lot : lots) {
            // 检查是否已缴纳
            AuctionDeposit existing = depositMapper.selectOne(
                    new LambdaQueryWrapper<AuctionDeposit>()
                            .eq(AuctionDeposit::getLotId, lot.getId())
                            .eq(AuctionDeposit::getUserId, userId)
            );
            if (existing != null && existing.getStatus() == AuctionConstant.DEPOSIT_PAID) {
                continue;
            }

            // TODO: 调用微信支付扣减保证金

            AuctionDeposit deposit = new AuctionDeposit();
            deposit.setLotId(lot.getId());
            deposit.setUserId(userId);
            deposit.setAmount(lot.getDepositAmount());
            deposit.setStatus(AuctionConstant.DEPOSIT_PAID);
            deposit.setPayTime(LocalDateTime.now());
            deposit.setCreateTime(LocalDateTime.now());
            
            if (existing != null) {
                deposit.setId(existing.getId());
                depositMapper.updateById(deposit);
            } else {
                depositMapper.insert(deposit);
            }
        }

        return Result.success();
    }

    /**
     * 出价 (POST /auction/lots/{lot_id}/bid)
     */
    @Transactional
    @PostMapping("/lots/{lotId}/bid")
    public Result<Void> placeBid(
            @PathVariable Long lotId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        Long bidPrice = Long.valueOf(params.get("bidPrice").toString());
        
        // 验证保证金
        AuctionDeposit deposit = depositMapper.selectOne(
                new LambdaQueryWrapper<AuctionDeposit>()
                        .eq(AuctionDeposit::getLotId, lotId)
                        .eq(AuctionDeposit::getUserId, userId)
                        .eq(AuctionDeposit::getStatus, AuctionConstant.DEPOSIT_PAID)
        );
        if (deposit == null) {
            throw new BusinessException(ResultCode.AUCTION_DEPOSIT_REQUIRED);
        }

        AuctionLot lot = lotMapper.selectById(lotId);
        if (lot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (lot.getStatus() != AuctionConstant.LOT_STATUS_ONGOING) {
            throw new BusinessException(ResultCode.AUCTION_ENDED);
        }
        
        // 验证出价
        long minBid = lot.getCurrentPrice() + lot.getBidIncrement();
        if (bidPrice < minBid) {
            throw new BusinessException(ResultCode.AUCTION_BID_TOO_LOW);
        }

        // 先将之前的最高出价标记为非最高
        List<AuctionBid> previousBids = bidMapper.selectList(
                new LambdaQueryWrapper<AuctionBid>()
                        .eq(AuctionBid::getLotId, lotId)
                        .eq(AuctionBid::getIsWinning, 1)
        );
        for (AuctionBid prevBid : previousBids) {
            prevBid.setIsWinning(0);
            bidMapper.updateById(prevBid);
        }

        // 创建新出价
        AuctionBid bid = new AuctionBid();
        bid.setLotId(lotId);
        bid.setUserId(userId);
        bid.setBidPrice(bidPrice);
        bid.setBidTime(LocalDateTime.now());
        bid.setIsWinning(1);
        bidMapper.insert(bid);

        // 更新拍品
        lot.setCurrentPrice(bidPrice);
        lot.setBidCount(lot.getBidCount() + 1);
        lot.setHighestBidderId(userId);
        lot.setUpdateTime(LocalDateTime.now());
        lotMapper.updateById(lot);

        // 通过 WebSocket 推送出价消息
        Map<String, Object> message = new HashMap<>();
        message.put("type", "bid");
        message.put("lotId", lotId);
        message.put("bidPrice", bidPrice);
        message.put("userId", userId);
        message.put("bidCount", lot.getBidCount());
        message.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/auction/" + lotId, message);

        return Result.success();
    }

    /**
     * 获取出价记录 (GET /auction/lots/{lot_id}/bids)
     */
    @GetMapping("/lots/{lotId}/bids")
    public Result<PageResult<AuctionBid>> getLotBids(
            @PathVariable Long lotId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Page<AuctionBid> pageResult = new Page<>(page, pageSize);
        Page<AuctionBid> result = bidMapper.selectPage(pageResult,
                new LambdaQueryWrapper<AuctionBid>()
                        .eq(AuctionBid::getLotId, lotId)
                        .orderByDesc(AuctionBid::getBidTime));
        
        return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
    }

    /**
     * 获取我的竞拍 (GET /auction/my-bids)
     */
    @GetMapping("/my-bids")
    public Result<Map<String, Object>> getMyBids(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        // 获取用户缴纳过保证金的拍品
        Page<AuctionDeposit> pageResult = new Page<>(page, pageSize);
        Page<AuctionDeposit> deposits = depositMapper.selectPage(pageResult,
                new LambdaQueryWrapper<AuctionDeposit>()
                        .eq(AuctionDeposit::getUserId, userId)
                        .eq(AuctionDeposit::getStatus, AuctionConstant.DEPOSIT_PAID)
                        .orderByDesc(AuctionDeposit::getCreateTime));
        
        List<Long> lotIds = deposits.getRecords().stream()
                .map(AuctionDeposit::getLotId)
                .collect(Collectors.toList());
        
        List<AuctionLot> lots = lotIds.isEmpty() ? List.of() : lotMapper.selectBatchIds(lotIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", lots);
        result.put("total", deposits.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    // === 内部类 ===
    static class AuctionLotVO {
        public Long id;
        public Long sessionId;
        public String lotNo;
        public Long startPrice;
        public Long currentPrice;
        public Long bidIncrement;
        public Integer bidCount;
        public Long depositAmount;
        public Integer status;
        public String startTime;
        public String endTime;
        public List<AuctionBid> recentBids;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getLotNo() { return lotNo; }
        public void setLotNo(String lotNo) { this.lotNo = lotNo; }
        public Long getStartPrice() { return startPrice; }
        public void setStartPrice(Long startPrice) { this.startPrice = startPrice; }
        public Long getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(Long currentPrice) { this.currentPrice = currentPrice; }
        public Long getBidIncrement() { return bidIncrement; }
        public void setBidIncrement(Long bidIncrement) { this.bidIncrement = bidIncrement; }
        public Integer getBidCount() { return bidCount; }
        public void setBidCount(Integer bidCount) { this.bidCount = bidCount; }
        public Long getDepositAmount() { return depositAmount; }
        public void setDepositAmount(Long depositAmount) { this.depositAmount = depositAmount; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public List<AuctionBid> getRecentBids() { return recentBids; }
        public void setRecentBids(List<AuctionBid> recentBids) { this.recentBids = recentBids; }
    }
}
