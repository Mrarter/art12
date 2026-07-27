package com.shiyiju.user.service.ledger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.user.entity.LedgerAccount;
import com.shiyiju.user.entity.LedgerTransaction;
import com.shiyiju.user.mapper.LedgerAccountMapper;
import com.shiyiju.user.mapper.LedgerTransactionMapper;
import com.shiyiju.user.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 金融级双向记账系统 — 资金操作的唯一真相（Single Source of Truth）
 *
 * 设计原则：
 * 1. 所有资金变动必须先写 ledger_transaction（append-only）
 * 2. 更新 ledger_account 余额
 * 3. 再写 wallet_bill（仅作为审计视图）
 * 4. REFUND = Reversal Transaction（不扣款，反向分录）
 *
 * 调用链路：
 *   Controller/Service → LedgerService.credit()/debit()/reverse()
 *     ├→ 1. 写入 ledger_transaction（幂等: txn_id）
 *     ├→ 2. 更新 ledger_account.balance
 *     └→ 3. 写入 wallet_bill（审计视图）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerTransactionMapper transactionMapper;
    private final LedgerAccountMapper accountMapper;
    private final WalletService walletService;

    // ===================== 核心操作 =====================

    /**
     * 贷方入账（用户收到钱）
     *
     * @param userId    收款用户
     * @param amount    金额
     * @param bizType   业务类型
     * @param relatedId 关联ID
     * @param remark    备注
     * @return txn_id
     */
    @Transactional(rollbackFor = Exception.class)
    public String credit(Long userId, BigDecimal amount, String bizType,
                         Long relatedId, String relatedType, String remark) {
        String txnId = generateTxnId(bizType);
        return executeTransaction(txnId, userId, amount, bizType, "CREDIT",
                relatedId, relatedType, null, remark);
    }

    /**
     * 借方出账（用户支出钱）
     */
    @Transactional(rollbackFor = Exception.class)
    public String debit(Long userId, BigDecimal amount, String bizType,
                        Long relatedId, String relatedType, String remark) {
        String txnId = generateTxnId(bizType);
        return executeTransaction(txnId, userId, amount, bizType, "DEBIT",
                relatedId, relatedType, null, remark);
    }

    /**
     * 反向交易（退款）— 金融级退款核心
     *
     * 不是"再扣一次钱"，而是：
     * 1. 找到原始交易
     * 2. 创建方向相反的新交易
     * 3. reversal_of_txn_id = original txn_id
     *
     * @param originalTxnId 原始交易ID（PAY 时的 txn_id）
     * @param reason        退款原因
     * @return 新的退款 txn_id
     */
    @Transactional(rollbackFor = Exception.class)
    public String reverse(String originalTxnId, String reason) {
        // 1. 查找原始交易
        LedgerTransaction original = transactionMapper.findByTxnId(originalTxnId);
        if (original == null) {
            throw new BusinessException(404, "原始交易不存在: " + originalTxnId);
        }

        // 2. 幂等：已反向的不重复执行
        LedgerTransaction existing = transactionMapper.findReversalByTxnId(originalTxnId);
        if (existing != null) {
            log.warn("交易已反向，幂等返回: txnId={}, reversal={}", originalTxnId, existing.getTxnId());
            return existing.getTxnId();
        }

        // 3. 创建反向交易
        String reverseDirection = "CREDIT".equals(original.getDirection()) ? "DEBIT" : "CREDIT";
        String newTxnId = generateTxnId("REFUND");
        return executeTransaction(newTxnId, original.getUserId(), original.getAmount(),
                "REFUND", reverseDirection,
                original.getRelatedId(), original.getRelatedType(),
                originalTxnId, "退款反向分录: " + reason);
    }

    /**
     * 执行一笔账本交易（写 ledger + 更新 account + 写 wallet_bill）
     */
    private String executeTransaction(String txnId, Long userId, BigDecimal amount,
                                      String bizType, String direction,
                                      Long relatedId, String relatedType,
                                      String reversalOfTxnId, String remark) {
        // 幂等检查
        if (transactionMapper.findByTxnId(txnId) != null) {
            log.warn("交易已存在，幂等返回: txnId={}", txnId);
            return txnId;
        }

        // 1. 写 ledger_transaction
        LedgerTransaction txn = new LedgerTransaction();
        txn.setTxnId(txnId);
        txn.setBizType(bizType);
        txn.setDirection(direction);
        txn.setAmount(amount);
        txn.setUserId(userId);
        txn.setRelatedId(relatedId);
        txn.setRelatedType(relatedType);
        txn.setReversalOfTxnId(reversalOfTxnId);
        txn.setStatus("SUCCESS");
        txn.setRemark(remark);
        txn.setCreatedTime(LocalDateTime.now());
        transactionMapper.insert(txn);

        // 2. 更新 ledger_account 余额
        LedgerAccount account = getOrCreateAccount(userId, resolveAccountType(userId));
        int rows;
        if ("CREDIT".equals(direction)) {
            rows = accountMapper.creditBalance(userId, amount, account.getVersion());
        } else {
            rows = accountMapper.debitBalance(userId, amount, account.getVersion());
        }
        if (rows == 0) {
            throw new BusinessException(500, "账本余额更新失败，请重试: txnId=" + txnId);
        }

        // 3. 写入 wallet_bill（审计视图）
        String billType = "REFUND".equals(bizType) ? "refund_reverse" : bizType.toLowerCase();
        if ("CREDIT".equals(direction)) {
            walletService.income(userId, amount, billType, relatedId, relatedType,
                    "[Ledger] " + (remark != null ? remark : ""));
        } else {
            walletService.expense(userId, amount, billType, relatedId, relatedType,
                    "[Ledger] " + (remark != null ? remark : ""));
        }

        log.info("账本交易完成: txnId={}, userId={}, {}={}, bizType={}",
                txnId, userId, direction, amount, bizType);
        return txnId;
    }

    // ===================== 查询方法 =====================

    public LedgerAccount getAccount(Long userId) {
        return getOrCreateAccount(userId, resolveAccountType(userId));
    }

    public BigDecimal getBalance(Long userId) {
        LedgerAccount account = getAccount(userId);
        return account != null ? account.getBalance() : BigDecimal.ZERO;
    }

    public List<LedgerTransaction> listTransactions(Long userId, int page, int pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<LedgerTransaction> p =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        return transactionMapper.selectPage(p,
                new LambdaQueryWrapper<LedgerTransaction>()
                        .eq(LedgerTransaction::getUserId, userId)
                        .orderByDesc(LedgerTransaction::getCreatedTime))
                .getRecords();
    }

    public LedgerTransaction findByTxnId(String txnId) {
        return transactionMapper.findByTxnId(txnId);
    }

    // ===================== 内部方法 =====================

    private LedgerAccount getOrCreateAccount(Long userId, String accountType) {
        LedgerAccount account = accountMapper.selectOne(
                new LambdaQueryWrapper<LedgerAccount>().eq(LedgerAccount::getUserId, userId));
        if (account == null) {
            account = new LedgerAccount();
            account.setUserId(userId);
            account.setAccountType(accountType);
            account.setBalance(BigDecimal.ZERO);
            account.setFrozenBalance(BigDecimal.ZERO);
            account.setVersion(0);
            accountMapper.insert(account);
        }
        return account;
    }

    private String resolveAccountType(Long userId) {
        return "USER";
    }

    private String generateTxnId(String bizType) {
        return bizType + "-" + UUID.randomUUID().toString().replace("-", "")
                + System.currentTimeMillis();
    }
}
