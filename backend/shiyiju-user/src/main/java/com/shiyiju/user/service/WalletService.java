package com.shiyiju.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.user.entity.Wallet;
import com.shiyiju.user.entity.WalletBill;
import com.shiyiju.user.mapper.WalletMapper;
import com.shiyiju.user.mapper.WalletBillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包核心服务
 * 
 * 所有余额变动必须通过此类，禁止直接 update user_wallet。
 * 每一笔变动自动记录 wallet_bill 流水。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletMapper walletMapper;
    private final WalletBillMapper walletBillMapper;

    /**
     * 入账（增加余额）
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet income(Long userId, BigDecimal amount, String billType,
                         Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        int rows = walletMapper.addBalance(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "钱包入账失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, billType, amount,
                wallet.getBalance().subtract(amount), wallet.getBalance(),
                relatedId, relatedType, remark);
        log.info("钱包入账: userId={}, amount={}, type={}, relatedId={}",
                userId, amount, billType, relatedId);
        return wallet;
    }

    /**
     * 冻结入账：销售款已到账但买家未确认收货时，先进入冻结金额，不增加可用余额。
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet frozenIncome(Long userId, BigDecimal amount, String billType,
                               Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        int rows = walletMapper.addFrozenIncome(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "冻结入账失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, billType, amount,
                wallet.getBalance(), wallet.getBalance(),
                relatedId, relatedType, remark);
        log.info("钱包冻结入账: userId={}, amount={}, type={}, relatedId={}",
                userId, amount, billType, relatedId);
        return wallet;
    }

    /**
     * 销售款解冻：确认收货后，将冻结中的销售款释放为可用余额。
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet releaseFrozenIncome(Long userId, BigDecimal amount, String billType,
                                      Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        int rows = walletMapper.releaseFrozenIncome(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "销售款解冻失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, billType, amount,
                wallet.getBalance().subtract(amount), wallet.getBalance(),
                relatedId, relatedType, remark);
        log.info("钱包销售款解冻: userId={}, amount={}, type={}, relatedId={}",
                userId, amount, billType, relatedId);
        return wallet;
    }

    /**
     * 出账（扣减余额，含余额校验）
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet expense(Long userId, BigDecimal amount, String billType,
                          Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(400, "余额不足");
        }
        int rows = walletMapper.deductBalance(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "钱包出账失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, billType, amount.negate(),
                wallet.getBalance().add(amount), wallet.getBalance(),
                relatedId, relatedType, remark);
        return wallet;
    }

    /**
     * 冻结金额
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet freeze(Long userId, BigDecimal amount,
                         Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(400, "余额不足，无法冻结");
        }
        int rows = walletMapper.freezeBalance(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "冻结失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, "freeze", amount,
                wallet.getBalance().add(amount), wallet.getBalance(),
                relatedId, relatedType, remark);
        return wallet;
    }

    /**
     * 解冻金额
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet unfreeze(Long userId, BigDecimal amount,
                           Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        if (wallet.getFreezeAmount().compareTo(amount) < 0) {
            throw new BusinessException(400, "冻结余额不足");
        }
        int rows = walletMapper.unfreezeBalance(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "解冻失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, "unfreeze", amount,
                wallet.getBalance().subtract(amount), wallet.getBalance(),
                relatedId, relatedType, remark);
        return wallet;
    }

    /**
     * 转账
     */
    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long fromUserId, Long toUserId, BigDecimal amount,
                         String remark) {
        expense(fromUserId, amount, "transfer", null, "transfer",
                "转账给用户" + toUserId + ": " + (remark != null ? remark : ""));
        income(toUserId, amount, "transfer", null, "transfer",
                "收到用户" + fromUserId + "转账: " + (remark != null ? remark : ""));
    }

    /**
     * 退款回滚（扣减余额，不校验余额是否足够）
     * 用于转售退款等场景：从已入账的钱包中扣除之前入账的金额
     * 幂等：调用方保证不重复调用
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet refund(Long userId, BigDecimal amount, String billType,
                         Long relatedId, String relatedType, String remark) {
        Wallet wallet = getOrCreateWallet(userId);
        // 退款使用 expense 逻辑（扣减余额，需要余额校验）
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("退款余额不足，强制扣减至0: userId={}, balance={}, need={}", userId, wallet.getBalance(), amount);
            // 如果余额不足，扣到0为止，不阻塞退款流程
            amount = wallet.getBalance();
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("退款金额为0或负值，跳过: userId={}", userId);
            return wallet;
        }
        int rows = walletMapper.deductBalance(userId, amount, wallet.getVersion());
        if (rows == 0) {
            throw new BusinessException(500, "退款扣款失败，请重试");
        }
        wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        recordBill(userId, billType, amount.negate(),
                wallet.getBalance().add(amount), wallet.getBalance(),
                relatedId, relatedType, remark);
        log.info("退款回滚: userId={}, amount={}, type={}, relatedId={}",
                userId, amount, billType, relatedId);
        return wallet;
    }

    /**
     * 获取钱包（不存在则自动创建）
     */
    @Transactional(rollbackFor = Exception.class)
    public Wallet getOrCreateWallet(Long userId) {
        Wallet wallet = walletMapper.selectOne(
                new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
        if (wallet == null) {
            int created = walletMapper.ensureWallet(userId);
            wallet = walletMapper.selectOne(
                    new LambdaQueryWrapper<Wallet>().eq(Wallet::getUserId, userId));
            if (created > 0) log.info("自动创建钱包: userId={}", userId);
        }
        return wallet;
    }

    public Wallet getWallet(Long userId) { return getOrCreateWallet(userId); }
    public BigDecimal getBalance(Long userId) { return getOrCreateWallet(userId).getBalance(); }

    private void recordBill(Long userId, String billType, BigDecimal amount,
                            BigDecimal beforeBalance, BigDecimal afterBalance,
                            Long relatedId, String relatedType, String remark) {
        WalletBill bill = new WalletBill();
        bill.setUserId(userId);
        bill.setBillType(billType);
        bill.setAmount(amount);
        bill.setBeforeBalance(beforeBalance);
        bill.setAfterBalance(afterBalance);
        bill.setRelatedId(relatedId);
        bill.setRelatedType(relatedType);
        bill.setRemark(remark);
        bill.setCreatedTime(LocalDateTime.now());
        walletBillMapper.insert(bill);
    }
}
