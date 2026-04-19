package com.shiyiju.promotion.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.constant.PromoterConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.promotion.entity.CommissionLog;
import com.shiyiju.promotion.entity.WithdrawRecord;
import com.shiyiju.promotion.mapper.CommissionLogMapper;
import com.shiyiju.promotion.mapper.WithdrawRecordMapper;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/promoter")
@RequiredArgsConstructor
public class PromotionController {

    private final PromoterRecordMapper promoterRecordMapper;
    private final CommissionLogMapper commissionLogMapper;
    private final WithdrawRecordMapper withdrawRecordMapper;

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
        
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, userId)
                        .eq(PromoterRecord::getAgreementStatus, 1)
        );
        if (promoter == null) {
            throw new BusinessException(ResultCode.PROMOTER_NOT_OPENED);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("level", promoter.getLevel());
        data.put("levelName", getLevelName(promoter.getLevel()));
        data.put("totalCommission", promoter.getTotalCommission());
        data.put("withdrawableCommission", promoter.getWithdrawableCommission());
        data.put("withdrawnCommission", promoter.getWithdrawnCommission());
        data.put("subordinateCount", promoter.getSubordinateCount());
        data.put("inviteCode", promoter.getInviteCode());
        data.put("agreementTime", promoter.getAgreementTime() != null ? 
                promoter.getAgreementTime().toString() : null);
        
        return Result.success(data);
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
        
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, userId)
                        .eq(PromoterRecord::getAgreementStatus, 1)
        );
        if (promoter == null) {
            throw new BusinessException(ResultCode.PROMOTER_NOT_OPENED);
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
        
        data.put("totalCommission", promoter.getTotalCommission());
        data.put("recentCommission", recentCommission);
        data.put("recentOrderCount", recentOrderCount);
        data.put("subordinateCount", promoter.getSubordinateCount());
        data.put("periodDays", days);
        
        return Result.success(data);
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
        
        PromoterRecord promoter = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, userId)
                        .eq(PromoterRecord::getAgreementStatus, 1)
        );
        if (promoter == null) {
            throw new BusinessException(ResultCode.PROMOTER_NOT_OPENED);
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
        
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PromoterRecord> pageResult = 
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<PromoterRecord> result = 
                promoterRecordMapper.selectPage(pageResult,
                        new LambdaQueryWrapper<PromoterRecord>()
                                .eq(PromoterRecord::getInviterId, userId)
                                .orderByDesc(PromoterRecord::getCreateTime));

        return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
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
                        .eq(PromoterRecord::getAgreementStatus, 1)
        );
        if (promoter == null) {
            throw new BusinessException(ResultCode.PROMOTER_NOT_OPENED);
        }

        Long amount = Long.valueOf(params.get("amount").toString());
        if (amount <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "提现金额必须大于0");
        }
        if (amount > promoter.getWithdrawableCommission()) {
            throw new BusinessException(ResultCode.COMMISSION_WITHDRAW_INSUFFICIENT);
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

        // 扣减可提现佣金
        promoter.setWithdrawableCommission(promoter.getWithdrawableCommission() - amount);
        promoterRecordMapper.updateById(promoter);

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
}
