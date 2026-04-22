package com.shiyiju.promotion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.constant.PromoterConstant;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;

import com.shiyiju.promotion.entity.CommissionLog;
import com.shiyiju.promotion.entity.WithdrawRecord;
import com.shiyiju.promotion.mapper.CommissionLogMapper;
import com.shiyiju.promotion.mapper.WithdrawRecordMapper;
import com.shiyiju.promotion.vo.EarningsDetailVO;
import com.shiyiju.promotion.vo.EarningsTrendVO;
import com.shiyiju.promotion.vo.RankingVO;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.entity.User;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import com.shiyiju.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/promoter")
@RequiredArgsConstructor
public class PromotionController {

    private final PromoterRecordMapper promoterRecordMapper;
    private final CommissionLogMapper commissionLogMapper;
    private final WithdrawRecordMapper withdrawRecordMapper;
    private final UserMapper userMapper;

    /**
     * 获取推广中心数据 (GET /promoter/center)
     */
    @GetMapping("/center")
    public Result<Map<String, Object>> getPromoterCenter(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        try {
            PromoterRecord promoter = promoterRecordMapper.selectOne(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getUserId, userId)
                            .eq(PromoterRecord::getStatus, 1)
            );
            if (promoter == null) {
                // 返回默认数据，而非抛异常
                Map<String, Object> defaultData = new HashMap<>();
                defaultData.put("level", 0);
                defaultData.put("levelName", "未开通");
                defaultData.put("totalSales", 0);
                defaultData.put("totalOrders", 0);
                defaultData.put("teamSize", 0);
                defaultData.put("inviteCode", null);
                defaultData.put("signTime", null);
                defaultData.put("isPromoter", false);
                return Result.success(defaultData);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("level", promoter.getLevel());
            data.put("levelName", getLevelName(promoter.getLevel()));
            data.put("totalSales", promoter.getTotalSales());
            data.put("totalOrders", promoter.getTotalOrders());
            data.put("teamSize", promoter.getTeamSize());
            data.put("inviteCode", promoter.getInviteCode());
            data.put("signTime", promoter.getSignTime() != null ? 
                    promoter.getSignTime().toString() : null);
            data.put("isPromoter", true);
            
            return Result.success(data);
        } catch (Exception e) {
            log.warn("获取推广中心数据异常: userId={}, error={}", userId, e.getMessage());
            // 返回默认数据
            Map<String, Object> defaultData = new HashMap<>();
            defaultData.put("level", 0);
            defaultData.put("levelName", "未开通");
            defaultData.put("totalSales", 0);
            defaultData.put("totalOrders", 0);
            defaultData.put("teamSize", 0);
            defaultData.put("inviteCode", null);
            defaultData.put("signTime", null);
            defaultData.put("isPromoter", false);
            return Result.success(defaultData);
        }
    }

    /**
     * 获取业绩详情 (GET /promoter/performance)
     */
    @GetMapping("/performance")
    public Result<Map<String, Object>> getPerformance(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(required = false, defaultValue = "30") Integer days
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        try {
            PromoterRecord promoter = promoterRecordMapper.selectOne(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getUserId, userId)
                            .eq(PromoterRecord::getStatus, 1)
            );
            if (promoter == null) {
                // 返回默认数据
                Map<String, Object> data = new HashMap<>();
                data.put("totalSales", 0);
                data.put("totalOrders", 0);
                data.put("recentCommission", 0L);
                data.put("recentOrderCount", 0);
                data.put("teamSize", 0);
                data.put("periodDays", days);
                return Result.success(data);
            }

            Map<String, Object> data = new HashMap<>();
            
            // 获取最近佣金额
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            List<CommissionLog> recentLogs = commissionLogMapper.selectList(
                    new LambdaQueryWrapper<CommissionLog>()
                            .eq(CommissionLog::getPromoterId, promoter.getId())
                            .ge(CommissionLog::getCreateTime, startTime)
            );
            
            long recentCommission = recentLogs.stream()
                    .filter(log -> log.getCommissionAmount() != null && log.getStatus() == PromoterConstant.COMMISSION_SETTLED)
                    .mapToLong(CommissionLog::getCommissionAmount)
                    .sum();
            
            int recentOrderCount = recentLogs.size();
            
            data.put("totalSales", promoter.getTotalSales());
            data.put("totalOrders", promoter.getTotalOrders());
            data.put("recentCommission", recentCommission);
            data.put("recentOrderCount", recentOrderCount);
            data.put("teamSize", promoter.getTeamSize());
            data.put("periodDays", days);
            
            return Result.success(data);
        } catch (Exception e) {
            log.warn("获取业绩详情异常: userId={}, error={}", userId, e.getMessage());
            Map<String, Object> data = new HashMap<>();
            data.put("totalSales", 0);
            data.put("totalOrders", 0);
            data.put("recentCommission", 0L);
            data.put("recentOrderCount", 0);
            data.put("teamSize", 0);
            data.put("periodDays", days);
            return Result.success(data);
        }
    }

    /**
     * 获取佣金明细 (GET /promoter/commission-log)
     */
    @GetMapping("/commission-log")
    public Result<PageResult<CommissionLog>> getCommissionLogs(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        try {
            PromoterRecord promoter = promoterRecordMapper.selectOne(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getUserId, userId)
                            .eq(PromoterRecord::getStatus, 1)
            );
            if (promoter == null) {
                // 返回空列表
                return Result.success(PageResult.of(0L, page, pageSize, List.of()));
            }

            LambdaQueryWrapper<CommissionLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CommissionLog::getPromoterId, promoter.getId())
                   .orderByDesc(CommissionLog::getCreateTime);
            
            // 使用分页查询
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CommissionLog> pageResult = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CommissionLog> result = 
                    commissionLogMapper.selectPage(pageResult, wrapper);

            return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
        } catch (Exception e) {
            log.warn("获取佣金明细异常: userId={}, error={}", userId, e.getMessage());
            return Result.success(PageResult.of(0L, page, pageSize, List.of()));
        }
    }

    /**
     * 获取团队列表 (GET /promoter/team)
     */
    @GetMapping("/team")
    public Result<PageResult<PromoterRecord>> getTeamList(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        try {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<PromoterRecord> pageResult = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<PromoterRecord> result = 
                    promoterRecordMapper.selectPage(pageResult,
                            new LambdaQueryWrapper<PromoterRecord>()
                                    .eq(PromoterRecord::getParentId, userId)
                                    .orderByDesc(PromoterRecord::getCreateTime));

            return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
        } catch (Exception e) {
            log.warn("获取团队列表异常: userId={}, error={}", userId, e.getMessage());
            return Result.success(PageResult.of(0L, page, pageSize, List.of()));
        }
    }

    /**
     * 获取提现账户信息 (GET /promoter/withdraw-account)
     */
    @GetMapping("/withdraw-account")
    public Result<Map<String, Object>> getWithdrawAccount(
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        // 返回用户设置的提现账户信息
        // TODO: 从用户表中获取或单独存储
        Map<String, Object> account = new HashMap<>();
        account.put("hasAccount", false);
        account.put("accountType", null);
        account.put("accountInfo", null);
        return Result.success(account);
    }

    /**
     * 申请提现 (POST /promoter/withdraw)
     */
    @Transactional
    @PostMapping("/withdraw")
    public Result<Void> applyWithdraw(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, userId)
                        .eq(PromoterRecord::getStatus, 1)
        );
        if (promoter == null) {
            return Result.fail(1103, "未开通艺荐官");
        }

        Long amount = Long.valueOf(params.get("amount").toString());
        if (amount <= 0) {
            return Result.fail(400, "提现金额必须大于0");
        }

        // 创建提现记录
        WithdrawRecord record = new WithdrawRecord();
        record.setPromoterId(promoter.getId());
        record.setAmount(amount);
        record.setFeeAmount(0L); // TODO: 计算手续费
        record.setActualAmount(amount);
        record.setAccountType(params.get("accountType") != null ? params.get("accountType").toString() : "wechat");
        record.setAccountInfo(params.get("accountInfo") != null ? params.get("accountInfo").toString() : null);
        record.setAccountName(params.get("accountName") != null ? params.get("accountName").toString() : null);
        record.setStatus(PromoterConstant.WITHDRAW_PENDING);
        record.setCreateTime(LocalDateTime.now());
        withdrawRecordMapper.insert(record);

        return Result.success();
    }

    /**
     * 获取提现记录 (GET /promoter/withdraw-log)
     */
    @GetMapping("/withdraw-log")
    public Result<PageResult<WithdrawRecord>> getWithdrawLogs(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, userId)
        );
        if (promoter == null) {
            return Result.success(PageResult.of(0L, page, pageSize, List.of()));
        }

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WithdrawRecord> pageResult = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<WithdrawRecord> result = 
                withdrawRecordMapper.selectPage(pageResult,
                        new LambdaQueryWrapper<WithdrawRecord>()
                                .eq(WithdrawRecord::getPromoterId, promoter.getId())
                                .orderByDesc(WithdrawRecord::getCreateTime));

        return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
    }

    /**
     * 获取推广素材 (GET /promoter/materials)
     */
    @GetMapping("/materials")
    public Result<Map<String, Object>> getMaterials() {
        // 返回固定素材
        Map<String, Object> materials = new HashMap<>();
        
        List<Map<String, String>> images = List.of(
                Map.of("id", "1", "title", "艺术品分销海报1", "url", "https://cdn.shiyiju.com/materials/poster1.jpg", "type", "image"),
                Map.of("id", "2", "title", "艺术品分销海报2", "url", "https://cdn.shiyiju.com/materials/poster2.jpg", "type", "image")
        );
        
        List<Map<String, String>> texts = List.of(
                Map.of("id", "3", "title", "分销文案模板1", "content", "【拾艺局】高端艺术品平台，专注艺术品交易与推广..."),
                Map.of("id", "4", "title", "分销文案模板2", "content", "发现艺术之美，投资价值之选...")
        );
        
        materials.put("images", images);
        materials.put("texts", texts);
        
        return Result.success(materials);
    }

    private String getLevelName(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 1 -> "普通艺荐官";
            case 2 -> "高级艺荐官";
            case 3 -> "资深艺荐官";
            default -> "未知";
        };
    }

    /**
     * 获取收益趋势 (GET /promoter/earnings/trend)
     */
    @GetMapping("/earnings/trend")
    public Result<List<EarningsTrendVO>> getEarningsTrend(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "week") String period
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        try {
            PromoterRecord promoter = promoterRecordMapper.selectOne(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getUserId, userId)
                            .eq(PromoterRecord::getStatus, 1)
            );
            
            List<EarningsTrendVO> trendData = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            
            if ("week".equals(period)) {
                // 最近7天趋势
                LocalDateTime startOfWeek = now.minusDays(6).withHour(0).withMinute(0).withSecond(0);
                for (int i = 0; i < 7; i++) {
                    LocalDateTime day = startOfWeek.plusDays(i);
                    // 计算当天收益
                    LocalDateTime dayStart = day.toLocalDate().atStartOfDay();
                    LocalDateTime dayEnd = dayStart.plusDays(1);
                    long dayAmount = 0;
                    if (promoter != null) {
                        List<CommissionLog> logs = commissionLogMapper.selectList(
                                new LambdaQueryWrapper<CommissionLog>()
                                        .eq(CommissionLog::getPromoterId, promoter.getId())
                                        .ge(CommissionLog::getCreateTime, dayStart)
                                        .lt(CommissionLog::getCreateTime, dayEnd)
                                        .eq(CommissionLog::getStatus, PromoterConstant.COMMISSION_SETTLED)
                        );
                        dayAmount = logs.stream().mapToLong(log -> log.getCommissionAmount() != null ? log.getCommissionAmount() : 0L).sum();
                    }
                    EarningsTrendVO vo = new EarningsTrendVO();
                    vo.setLabel(getDayLabel(day.getDayOfWeek()));
                    vo.setValue(dayAmount);
                    trendData.add(vo);
                }
            } else if ("month".equals(period)) {
                // 近4周趋势
                for (int i = 3; i >= 0; i--) {
                    LocalDateTime weekStart = now.minusWeeks(i).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                    LocalDateTime weekEnd = weekStart.plusWeeks(1);
                    long weekAmount = 0;
                    if (promoter != null) {
                        List<CommissionLog> logs = commissionLogMapper.selectList(
                                new LambdaQueryWrapper<CommissionLog>()
                                        .eq(CommissionLog::getPromoterId, promoter.getId())
                                        .ge(CommissionLog::getCreateTime, weekStart)
                                        .lt(CommissionLog::getCreateTime, weekEnd)
                                        .eq(CommissionLog::getStatus, PromoterConstant.COMMISSION_SETTLED)
                        );
                        weekAmount = logs.stream().mapToLong(log -> log.getCommissionAmount() != null ? log.getCommissionAmount() : 0L).sum();
                    }
                    EarningsTrendVO vo = new EarningsTrendVO();
                    vo.setLabel("第" + (4 - i) + "周");
                    vo.setValue(weekAmount);
                    trendData.add(vo);
                }
            } else {
                // 近3个月趋势
                for (int i = 2; i >= 0; i--) {
                    LocalDateTime month = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                    LocalDateTime monthEnd = month.plusMonths(1);
                    long monthAmount = 0;
                    if (promoter != null) {
                        List<CommissionLog> logs = commissionLogMapper.selectList(
                                new LambdaQueryWrapper<CommissionLog>()
                                        .eq(CommissionLog::getPromoterId, promoter.getId())
                                        .ge(CommissionLog::getCreateTime, month)
                                        .lt(CommissionLog::getCreateTime, monthEnd)
                                        .eq(CommissionLog::getStatus, PromoterConstant.COMMISSION_SETTLED)
                        );
                        monthAmount = logs.stream().mapToLong(log -> log.getCommissionAmount() != null ? log.getCommissionAmount() : 0L).sum();
                    }
                    EarningsTrendVO vo = new EarningsTrendVO();
                    vo.setLabel((month.getMonthValue()) + "月");
                    vo.setValue(monthAmount);
                    trendData.add(vo);
                }
            }
            
            return Result.success(trendData);
        } catch (Exception e) {
            log.warn("获取收益趋势异常: userId={}, error={}", userId, e.getMessage());
            return Result.success(List.of());
        }
    }
    
    private String getDayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }

    /**
     * 获取收益明细 (GET /promoter/earnings)
     */
    @GetMapping("/earnings")
    public Result<PageResult<EarningsDetailVO>> getEarnings(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String type
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        try {
            PromoterRecord promoter = promoterRecordMapper.selectOne(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getUserId, userId)
                            .eq(PromoterRecord::getStatus, 1)
            );
            if (promoter == null) {
                return Result.success(PageResult.of(0L, page, pageSize, List.of()));
            }

            LambdaQueryWrapper<CommissionLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CommissionLog::getPromoterId, promoter.getId());
            if (type != null && !type.isEmpty()) {
                if ("level1".equals(type)) {
                    wrapper.eq(CommissionLog::getLevel, 1);
                } else if ("level2".equals(type)) {
                    wrapper.eq(CommissionLog::getLevel, 2);
                }
            }
            wrapper.orderByDesc(CommissionLog::getCreateTime);
            
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CommissionLog> pageResult = 
                    new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<CommissionLog> result = 
                    commissionLogMapper.selectPage(pageResult, wrapper);

            List<EarningsDetailVO> voList = result.getRecords().stream().map(log -> {
                EarningsDetailVO vo = new EarningsDetailVO();
                vo.setId(log.getId());
                vo.setTitle("订单收益");
                vo.setAmount(log.getCommissionAmount());
                vo.setStatus(getCommissionStatusText(log.getStatus()));
                vo.setCreateTime(log.getCreateTime() != null ? log.getCreateTime().toString() : null);
                vo.setOrderId(log.getOrderId());
                vo.setLevel(log.getLevel());
                vo.setType("order");
                return vo;
            }).collect(Collectors.toList());

            return Result.success(PageResult.of(result.getTotal(), page, pageSize, voList));
        } catch (Exception e) {
            log.warn("获取收益明细异常: userId={}, error={}", userId, e.getMessage());
            return Result.success(PageResult.of(0L, page, pageSize, List.of()));
        }
    }
    
    private String getCommissionStatusText(Integer status) {
        if (status == null) return "pending";
        if (status.equals(PromoterConstant.COMMISSION_SETTLED)) return "settled";
        if (status.equals(PromoterConstant.COMMISSION_PENDING)) return "pending";
        return "pending";
    }

    /**
     * 获取排行榜 (GET /promoter/ranking)
     */
    @GetMapping("/ranking")
    public Result<List<RankingVO>> getRanking(
            @RequestParam(defaultValue = "week") String period,
            @RequestParam(defaultValue = "10") Integer limit
    ) {
        try {
            // 获取所有艺荐官，按收益排序
            List<PromoterRecord> promoters = promoterRecordMapper.selectList(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getStatus, 1)
                            .orderByDesc(PromoterRecord::getTotalSales)
                            .last("LIMIT " + limit)
            );

            List<RankingVO> rankingList = new ArrayList<>();
            int rank = 1;
            for (PromoterRecord promoter : promoters) {
                RankingVO vo = new RankingVO();
                vo.setUserId(promoter.getUserId());
                vo.setRank(rank++);
                vo.setOrderCount(promoter.getTotalOrders() != null ? promoter.getTotalOrders() : 0);
                vo.setAmount(promoter.getTotalSales() != null ? promoter.getTotalSales().multiply(java.math.BigDecimal.valueOf(100)).longValue() : 0L);
                
                // 获取用户信息
                User user = userMapper.selectById(promoter.getUserId());
                if (user != null) {
                    vo.setNickname(user.getNickname() != null ? user.getNickname() : "用户" + user.getId());
                    vo.setAvatar(user.getAvatar());
                } else {
                    vo.setNickname("用户" + promoter.getUserId());
                }
                
                rankingList.add(vo);
            }

            return Result.success(rankingList);
        } catch (Exception e) {
            log.warn("获取排行榜异常: error={}", e.getMessage());
            return Result.success(List.of());
        }
    }
}
