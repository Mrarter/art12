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
import com.shiyiju.auction.service.AuctionBidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AuctionBidService auctionBidService;

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
            Integer displayStatus = resolveSessionStatus(session.getStartTime(), session.getEndTime(), session.getStatus());
            Map<String, Object> map = new HashMap<>();
            map.put("id", session.getId());
            map.put("name", session.getTitle());
            map.put("coverImage", session.getCoverImage());
            map.put("description", session.getDescription());
            map.put("rules", session.getRules());
            map.put("startTime", session.getStartTime());
            map.put("endTime", session.getEndTime());
            map.put("status", displayStatus);
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
        sessionMap.put("rules", session.getRules());
        sessionMap.put("startTime", session.getStartTime());
        sessionMap.put("endTime", session.getEndTime());
        sessionMap.put("status", resolveSessionStatus(session.getStartTime(), session.getEndTime(), session.getStatus()));
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
            map.put("depositAmount", lot.getDepositAmount());
            map.put("startTime", lot.getStartTime());
            map.put("endTime", lot.getEndTime());
            map.put("status", lot.getStatus());
            return map;
        }).collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("session", sessionMap);
        result.put("lots", lotMaps);
        
        return Result.success(result);
    }

    private Integer resolveSessionStatus(LocalDateTime startTime, LocalDateTime endTime, Integer status) {
        LocalDateTime now = LocalDateTime.now();
        if (startTime != null && now.isBefore(startTime)) {
            return AuctionConstant.SESSION_STATUS_COMING;
        }
        if (endTime != null && now.isAfter(endTime)) {
            return AuctionConstant.SESSION_STATUS_ENDED;
        }
        if (startTime != null || endTime != null) {
            return AuctionConstant.SESSION_STATUS_ONGOING;
        }
        return status;
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
            map.put("depositAmount", lot.getDepositAmount());
            map.put("startTime", lot.getStartTime());
            map.put("endTime", lot.getEndTime());
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
        vo.put("startTime", lot.getStartTime());
        vo.put("endTime", lot.getEndTime());
        
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
    @PostMapping("/sessions/{sessionId}/deposit")
    public Result<Void> payDeposit(
            @PathVariable Long sessionId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        return Result.fail(410, "接口已升级，请创建保证金支付订单");
    }

    /**
     * 出价 (POST /auction/lots/{lotId}/bid)
     */
    @PostMapping("/lots/{lotId}/bid")
    public Result<Map<String, Object>> placeBid(
            @PathVariable Long lotId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        if (params.get("bidPrice") == null) return Result.fail(400, "请输入出价金额");
        String requestId = params.get("requestId") == null ? null : params.get("requestId").toString();
        return Result.success(auctionBidService.place(lotId, userId,
                new java.math.BigDecimal(params.get("bidPrice").toString()), requestId));
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
        
        List<Long> sessionIds = deposits.getRecords().stream()
                .map(AuctionDeposit::getSessionId).filter(java.util.Objects::nonNull).distinct()
                .collect(Collectors.toList());
        List<AuctionLot> lots = sessionIds.isEmpty() ? List.of() : lotMapper.selectList(
                new LambdaQueryWrapper<AuctionLot>().in(AuctionLot::getSessionId, sessionIds)
                        .orderByDesc(AuctionLot::getEndTime));
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", lots);
        result.put("total", deposits.getTotal());
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    // ===================== 管理后台 API =====================

    /**
     * 创建拍卖专场 (POST /auction/admin/session/create)
     */
    @PostMapping("/admin/session/create")
    public Result<Void> createSession(@RequestBody Map<String, Object> params) {
        AuctionSession session = new AuctionSession();
        session.setTitle((String) params.getOrDefault("title", ""));
        session.setCoverImage((String) params.getOrDefault("coverImage", ""));
        session.setDescription((String) params.getOrDefault("description", ""));
        session.setStartTime(params.get("startTime") != null ? LocalDateTime.parse((String) params.get("startTime")) : LocalDateTime.now());
        session.setEndTime(params.get("endTime") != null ? LocalDateTime.parse((String) params.get("endTime")) : LocalDateTime.now().plusDays(7));
        session.setStatus(AuctionConstant.SESSION_STATUS_COMING);
        session.setTotalLots(0);
        session.setTotalBids(0);
        sessionMapper.insert(session);
        log.info("创建拍卖专场: id={}, title={}", session.getId(), session.getTitle());
        return Result.success();
    }

    /**
     * 更新拍卖专场 (POST /auction/admin/session/update)
     */
    @PostMapping("/admin/session/update")
    public Result<Void> updateSession(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        AuctionSession session = sessionMapper.selectById(id);
        if (session == null) {
            return Result.fail(404, "专场不存在");
        }
        if (params.containsKey("title")) session.setTitle((String) params.get("title"));
        if (params.containsKey("coverImage")) session.setCoverImage((String) params.get("coverImage"));
        if (params.containsKey("description")) session.setDescription((String) params.get("description"));
        if (params.containsKey("startTime")) session.setStartTime(LocalDateTime.parse((String) params.get("startTime")));
        if (params.containsKey("endTime")) session.setEndTime(LocalDateTime.parse((String) params.get("endTime")));
        sessionMapper.updateById(session);
        log.info("更新拍卖专场: id={}", id);
        return Result.success();
    }

    /**
     * 删除拍卖专场 (POST /auction/admin/session/delete)
     */
    @PostMapping("/admin/session/delete")
    public Result<Void> deleteSession(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        sessionMapper.deleteById(id);
        log.info("删除拍卖专场: id={}", id);
        return Result.success();
    }

    /**
     * 管理后台 - 拍品列表 (POST /auction/admin/lot/list)
     */
    @PostMapping("/admin/lot/list")
    public Result<PageResult<Map<String, Object>>> getAdminLotList(@RequestBody Map<String, Object> params) {
        int page = params.getOrDefault("page", 1) instanceof Number n ? n.intValue() : 1;
        int size = params.getOrDefault("size", 20) instanceof Number n ? n.intValue() : 20;
        Long sessionId = params.get("sessionId") instanceof Number n ? n.longValue() : null;

        Page<AuctionLot> pageResult = new Page<>(page, size);
        LambdaQueryWrapper<AuctionLot> wrapper = new LambdaQueryWrapper<AuctionLot>()
                .orderByAsc(AuctionLot::getLotNo);
        if (sessionId != null) {
            wrapper.eq(AuctionLot::getSessionId, sessionId);
        }
        Page<AuctionLot> resultPage = lotMapper.selectPage(pageResult, wrapper);

        List<Map<String, Object>> rows = resultPage.getRecords().stream().map(lot -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", lot.getId());
            row.put("lotNo", lot.getLotNo());
            row.put("title", lot.getTitle());
            row.put("coverImage", lot.getCoverImage());
            row.put("artistName", lot.getArtistName());
            row.put("startPrice", lot.getStartPrice());
            row.put("currentPrice", lot.getCurrentPrice());
            row.put("reservePrice", lot.getReservePrice());
            row.put("increment", lot.getIncrement());
            row.put("depositAmount", lot.getDepositAmount());
            row.put("bidCount", lot.getBidCount());
            row.put("status", lot.getStatus());
            row.put("startTime", lot.getStartTime());
            row.put("endTime", lot.getEndTime());
            row.put("sessionId", lot.getSessionId());
            row.put("artworkId", lot.getArtworkId());
            return row;
        }).collect(Collectors.toList());

        return Result.success(PageResult.of(resultPage.getTotal(), page, size, rows));
    }

    /**
     * 管理后台 - 创建拍品 (POST /auction/admin/lot/create)
     */
    @PostMapping("/admin/lot/create")
    public Result<Void> createLot(@RequestBody Map<String, Object> params) {
        AuctionLot lot = new AuctionLot();
        lot.setSessionId(((Number) params.get("sessionId")).longValue());
        lot.setTitle((String) params.getOrDefault("title", ""));
        lot.setCoverImage((String) params.getOrDefault("coverImage", ""));
        lot.setArtistName((String) params.getOrDefault("artistName", ""));
        lot.setLotNo(String.valueOf(params.getOrDefault("lotNo", "")));
        lot.setStartPrice(params.get("startPrice") != null ? new java.math.BigDecimal(params.get("startPrice").toString()) : java.math.BigDecimal.ZERO);
        lot.setCurrentPrice(params.get("currentPrice") != null ? new java.math.BigDecimal(params.get("currentPrice").toString()) : java.math.BigDecimal.ZERO);
        lot.setReservePrice(params.get("reservePrice") != null ? new java.math.BigDecimal(params.get("reservePrice").toString()) : java.math.BigDecimal.ZERO);
        lot.setIncrement(params.get("increment") != null ? new java.math.BigDecimal(params.get("increment").toString()) : new java.math.BigDecimal("500"));
        lot.setDepositAmount(params.get("depositAmount") != null ? new java.math.BigDecimal(params.get("depositAmount").toString()) : new java.math.BigDecimal("1000"));
        lot.setBidCount(0);
        lot.setStatus(AuctionConstant.LOT_STATUS_PENDING);
        lot.setStartTime(params.get("startTime") != null ? LocalDateTime.parse((String) params.get("startTime")) : LocalDateTime.now());
        lot.setEndTime(params.get("endTime") != null ? LocalDateTime.parse((String) params.get("endTime")) : LocalDateTime.now().plusDays(7));
        lotMapper.insert(lot);
        // 更新 session 的 lot 计数
        AuctionSession session = sessionMapper.selectById(lot.getSessionId());
        if (session != null) {
            session.setTotalLots(session.getTotalLots() + 1);
            sessionMapper.updateById(session);
        }
        return Result.success();
    }

    /**
     * 管理后台 - 删除拍品 (POST /auction/admin/lot/delete)
     */
    @PostMapping("/admin/lot/delete")
    public Result<Void> deleteLot(@RequestBody Map<String, Object> params) {
        Long id = ((Number) params.get("id")).longValue();
        AuctionLot lot = lotMapper.selectById(id);
        if (lot != null && lot.getSessionId() != null) {
            AuctionSession session = sessionMapper.selectById(lot.getSessionId());
            if (session != null && session.getTotalLots() > 0) {
                session.setTotalLots(session.getTotalLots() - 1);
                sessionMapper.updateById(session);
            }
        }
        lotMapper.deleteById(id);
        return Result.success();
    }

    /**
     * 管理后台 - 更新拍品 (POST /auction/admin/lot/update)
     */
    @PostMapping("/admin/lot/update")
    public Result<Void> updateLot(@RequestBody Map<String, Object> params) {
        try {
            Long id = ((Number) params.get("id")).longValue();
            AuctionLot lot = lotMapper.selectById(id);
            if (lot == null) {
                return Result.fail(404, "拍品不存在");
            }
            if (params.containsKey("title")) lot.setTitle((String) params.get("title"));
            if (params.containsKey("coverImage")) lot.setCoverImage((String) params.get("coverImage"));
            if (params.containsKey("artistName")) lot.setArtistName((String) params.get("artistName"));
            if (params.containsKey("sessionId")) lot.setSessionId(((Number) params.get("sessionId")).longValue());
            if (params.containsKey("lotNo")) lot.setLotNo(String.valueOf(params.get("lotNo")));
            if (params.containsKey("startPrice")) lot.setStartPrice(new java.math.BigDecimal(params.get("startPrice").toString()));
            if (params.containsKey("reservePrice")) lot.setReservePrice(new java.math.BigDecimal(params.get("reservePrice").toString()));
            if (params.containsKey("increment")) lot.setIncrement(new java.math.BigDecimal(params.get("increment").toString()));
            if (params.containsKey("depositAmount")) lot.setDepositAmount(new java.math.BigDecimal(params.get("depositAmount").toString()));
            if (params.containsKey("startTime")) lot.setStartTime(LocalDateTime.parse((String) params.get("startTime")));
            if (params.containsKey("endTime")) lot.setEndTime(LocalDateTime.parse((String) params.get("endTime")));
            lotMapper.updateById(lot);
            return Result.success();
        } catch (Exception e) {
            log.error("更新拍品失败", e);
            return Result.fail(500, e.getMessage());
        }
    }
}
