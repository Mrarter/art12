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
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 获取拍卖专场列表 (GET /auction/sessions)
     */
    @GetMapping("/sessions")
    public Result<PageResult<Map<String, Object>>> getSessions(
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
        
        Page<AuctionSession> pageResult = new Page<>(page, pageSize);
        Page<AuctionSession> result = sessionMapper.selectPage(pageResult, wrapper);
        log.info("Query auction sessions: total={}, records={}", result.getTotal(), result.getRecords().size());
        
        // 转换为兼容前端的格式
        List<Map<String, Object>> records = result.getRecords().stream().map(session -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", session.getId());
            map.put("name", session.getTitle());
            map.put("coverImage", session.getCoverImage());
            map.put("description", session.getDescription());
            map.put("startTime", session.getStartTime());
            map.put("endTime", session.getEndTime());
            map.put("status", session.getStatus());
            map.put("lotCount", session.getTotalLots());
            return map;
        }).collect(Collectors.toList());
        
        PageResult<Map<String, Object>> pageResultData = new PageResult<>();
        pageResultData.setTotal(result.getTotal());
        pageResultData.setPage(page);
        pageResultData.setPageSize(pageSize);
        pageResultData.setTotalPages((int) Math.ceil((double) result.getTotal() / pageSize));
        pageResultData.setRecords(records);
        
        return Result.success(pageResultData);
    }

    /**
     * 获取专场详情及拍品 (GET /auction/sessions/{sessionId})
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
        
        // 转换专场数据
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("id", session.getId());
        sessionMap.put("name", session.getTitle());
        sessionMap.put("coverImage", session.getCoverImage());
        sessionMap.put("description", session.getDescription());
        sessionMap.put("startTime", session.getStartTime());
        sessionMap.put("endTime", session.getEndTime());
        sessionMap.put("status", session.getStatus());
        sessionMap.put("lotCount", session.getTotalLots());
        
        // 转换拍品数据
        List<Map<String, Object>> lotMaps = lots.stream().map(lot -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", lot.getId());
            map.put("sessionId", lot.getSessionId());
            map.put("lotNo", lot.getLotNo());
            map.put("title", lot.getTitle());
            map.put("coverImage", lot.getCoverImage());
            map.put("artistName", lot.getArtistName());
            map.put("startPrice", lot.getStartPrice());
            map.put("currentPrice", lot.getCurrentPrice());
            map.put("bidCount", lot.getBidCount());
            map.put("status", lot.getStatus());
            return map;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("session", sessionMap);
        result.put("lots", lotMaps);
        
        return Result.success(result);
    }

    /**
     * 获取专场下的拍品 (GET /auction/sessions/{sessionId}/lots)
     */
    @GetMapping("/sessions/{sessionId}/lots")
    public Result<List<Map<String, Object>>> getSessionLots(@PathVariable Long sessionId) {
        List<AuctionLot> lots = lotMapper.selectList(
                new LambdaQueryWrapper<AuctionLot>()
                        .eq(AuctionLot::getSessionId, sessionId)
                        .orderByAsc(AuctionLot::getLotNo)
        );
        
        List<Map<String, Object>> lotMaps = lots.stream().map(lot -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", lot.getId());
            map.put("sessionId", lot.getSessionId());
            map.put("lotNo", lot.getLotNo());
            map.put("title", lot.getTitle());
            map.put("coverImage", lot.getCoverImage());
            map.put("artistName", lot.getArtistName());
            map.put("startPrice", lot.getStartPrice());
            map.put("currentPrice", lot.getCurrentPrice());
            map.put("bidCount", lot.getBidCount());
            map.put("status", lot.getStatus());
            return map;
        }).collect(Collectors.toList());
        
        return Result.success(lotMaps);
    }

    /**
     * 获取拍品详情 (GET /auction/lots/{lotId})
     */
    @GetMapping("/lots/{lotId}")
    public Result<Map<String, Object>> getLotDetail(@PathVariable Long lotId) {
        AuctionLot lot = lotMapper.selectById(lotId);
        if (lot == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", lot.getId());
        vo.put("sessionId", lot.getSessionId());
        vo.put("lotNo", lot.getLotNo());
        vo.put("title", lot.getTitle());
        vo.put("coverImage", lot.getCoverImage());
        vo.put("artistName", lot.getArtistName());
        vo.put("startPrice", lot.getStartPrice());
        vo.put("currentPrice", lot.getCurrentPrice());
        vo.put("bidIncrement", lot.getIncrement());
        vo.put("bidCount", lot.getBidCount());
        vo.put("depositAmount", lot.getDepositAmount());
        vo.put("status", lot.getStatus());
        vo.put("startTime", lot.getStartTime() != null ? lot.getStartTime().toString() : null);
        vo.put("endTime", lot.getEndTime() != null ? lot.getEndTime().toString() : null);
        
        // 获取出价记录
        List<AuctionBid> bids = bidMapper.selectList(
                new LambdaQueryWrapper<AuctionBid>()
                        .eq(AuctionBid::getLotId, lotId)
                        .orderByDesc(AuctionBid::getBidTime)
                        .last("LIMIT 10")
        );
        vo.put("recentBids", bids);
        
        return Result.success(vo);
    }

    /**
     * 缴纳保证金 (POST /auction/sessions/{sessionId}/deposit)
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
            if (existing != null && existing.getPayStatus() == AuctionConstant.DEPOSIT_PAID) {
                continue;
            }

            // TODO: 调用微信支付扣减保证金

            AuctionDeposit deposit = new AuctionDeposit();
            deposit.setLotId(lot.getId());
            deposit.setUserId(userId);
            deposit.setAmount(lot.getDepositAmount());
            deposit.setPayStatus(AuctionConstant.DEPOSIT_PAID);
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
     * 出价 (POST /auction/lots/{lotId}/bid)
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
                        .eq(AuctionDeposit::getPayStatus, AuctionConstant.DEPOSIT_PAID)
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
        long minBid = lot.getCurrentPrice().longValue() + lot.getIncrement().longValue();
        if (bidPrice < minBid) {
            throw new BusinessException(ResultCode.AUCTION_BID_TOO_LOW);
        }

        // 先将之前的最高出价标记为非最高
        List<AuctionBid> previousBids = bidMapper.selectList(
                new LambdaQueryWrapper<AuctionBid>()
                        .eq(AuctionBid::getLotId, lotId)
                        .eq(AuctionBid::getStatus, 1)
        );
        for (AuctionBid prevBid : previousBids) {
            prevBid.setStatus(0);
            bidMapper.updateById(prevBid);
        }

        // 创建新出价
        AuctionBid bid = new AuctionBid();
        bid.setLotId(lotId);
        bid.setUserId(userId);
        bid.setBidPrice(new java.math.BigDecimal(bidPrice));
        bid.setBidTime(LocalDateTime.now());
        bid.setStatus(1);
        bidMapper.insert(bid);

        // 更新拍品
        lot.setCurrentPrice(new java.math.BigDecimal(bidPrice));
        lot.setBidCount(lot.getBidCount() + 1);
        lot.setBuyerId(userId);
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
                        .eq(AuctionDeposit::getPayStatus, AuctionConstant.DEPOSIT_PAID)
                        .orderByDesc(AuctionDeposit::getCreateTime));
        
        List<Long> lotIds = deposits.getRecords().stream()
                .map(AuctionDeposit::getLotId)
                .collect(Collectors.toList());
        
        List<AuctionLot> lots = lotIds.isEmpty() ? List.of() : lotMapper.selectByIds(lotIds);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", lots);
        result.put("total", deposits.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }
}
