package com.shiyiju.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.util.AESUtil;
import com.shiyiju.user.dto.PayAccountAddDTO;
import com.shiyiju.user.entity.PayAccount;
import com.shiyiju.user.entity.RealnameCertification;
import com.shiyiju.user.entity.User;
import com.shiyiju.user.mapper.PayAccountMapper;
import com.shiyiju.user.mapper.RealnameCertificationMapper;
import com.shiyiju.user.mapper.UserMapper;
import com.shiyiju.user.vo.PayAccountVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收款账户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayAccountService {

    private final PayAccountMapper payAccountMapper;
    private final RealnameCertificationMapper realnameCertMapper;
    private final UserMapper userMapper;

    /** 账户类型常量 */
    private static final int TYPE_WECHAT = 1;
    private static final int TYPE_ALIPAY = 2;
    private static final int TYPE_BANK = 3;

    /** 账户类型名称映射 */
    private static String getTypeText(int type) {
        return switch (type) {
            case TYPE_WECHAT -> "微信收款";
            case TYPE_ALIPAY -> "支付宝收款";
            case TYPE_BANK -> "银行卡收款";
            default -> "未知";
        };
    }

    /** 账户图标映射 */
    private static String getTypeIcon(int type) {
        return switch (type) {
            case TYPE_WECHAT -> "wechat";
            case TYPE_ALIPAY -> "alipay";
            case TYPE_BANK -> "bank";
            default -> "default";
        };
    }

    /**
     * 添加收款账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void addAccount(Long userId, PayAccountAddDTO dto) {
        // 参数校验
        validateAccount(dto);

        // 防重复提交：同一用户同一类型同一账号不允许重复
        boolean exists = checkDuplicate(userId, dto);
        if (exists) {
            throw new BusinessException(400, "该账户已存在");
        }

        // 实名认证联动：检查用户是否已实名
        RealnameCertification realname = realnameCertMapper.selectOne(
                new LambdaQueryWrapper<RealnameCertification>()
                        .eq(RealnameCertification::getUserId, userId)
                        .eq(RealnameCertification::getStatus, 1));
        if ((dto.getAccountType() == TYPE_ALIPAY || dto.getAccountType() == TYPE_BANK) && realname == null) {
            throw new BusinessException(400, "请先完成实名认证，再绑定支付宝或银行卡收款账户");
        }
        Integer verifyStatus = (realname != null) ? 1 : 0;

        // 如果已实名，校验姓名一致性
        if (realname != null && !Objects.equals(realname.getRealName(), dto.getRealName().trim())) {
            throw new BusinessException(400, "收款人姓名与实名认证信息不一致");
        }

        // 如果没有默认账户，自动设为默认
        long count = payAccountMapper.selectCount(
                new LambdaQueryWrapper<PayAccount>()
                        .eq(PayAccount::getUserId, userId)
                        .eq(PayAccount::getStatus, 1));
        boolean autoDefault = (count == 0);

        // 构造实体
        PayAccount account = new PayAccount();
        account.setUserId(userId);
        account.setAccountType(dto.getAccountType());
        account.setRealName(dto.getRealName().trim());
        account.setIdCard(dto.getIdCard() != null ? AESUtil.maskIdCard(dto.getIdCard().trim()) : null);
        account.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        account.setBankName(dto.getBankName());
        account.setWechatOpenid(dto.getWechatOpenid());
        account.setAlipayAccount(dto.getAlipayAccount());

        // 银行卡号 AES 加密存储
        if (dto.getBankCard() != null && !dto.getBankCard().isEmpty()) {
            account.setBankCard(AESUtil.encrypt(dto.getBankCard().trim()));
        }

        // 如果用户要设为默认，先将其他默认取消
        if (Boolean.TRUE.equals(dto.getSetDefault()) || autoDefault) {
            clearDefault(userId);
            account.setIsDefault(1);
        } else {
            account.setIsDefault(0);
        }

        account.setVerifyStatus(verifyStatus);
        account.setStatus(1);
        payAccountMapper.insert(account);

        log.info("用户 {} 添加收款账户: type={}, id={}", userId, dto.getAccountType(), account.getId());
    }

    /**
     * 绑定当前登录账号的微信 OpenID 为收款账户。
     */
    @Transactional(rollbackFor = Exception.class)
    public PayAccountVO bindCurrentWechat(Long userId, Boolean setDefault) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        String openid = user.getOpenid();
        if (openid == null || openid.isBlank()) {
            throw new BusinessException(400, "当前账号未完成微信授权，请先使用微信登录或授权后再绑定");
        }

        PayAccount existing = payAccountMapper.selectOne(
                new LambdaQueryWrapper<PayAccount>()
                        .eq(PayAccount::getUserId, userId)
                        .eq(PayAccount::getAccountType, TYPE_WECHAT)
                        .eq(PayAccount::getWechatOpenid, openid)
                        .eq(PayAccount::getStatus, 1)
                        .last("LIMIT 1"));
        if (existing != null) {
            if (Boolean.TRUE.equals(setDefault) && existing.getIsDefault() != 1) {
                setDefaultAccount(userId, existing.getId());
                existing = payAccountMapper.selectById(existing.getId());
            }
            return toVO(existing);
        }

        RealnameCertification realname = getVerifiedRealname(userId);
        long count = payAccountMapper.selectCount(
                new LambdaQueryWrapper<PayAccount>()
                        .eq(PayAccount::getUserId, userId)
                        .eq(PayAccount::getStatus, 1));
        boolean shouldDefault = Boolean.TRUE.equals(setDefault) || count == 0;
        if (shouldDefault) {
            clearDefault(userId);
        }

        PayAccount account = new PayAccount();
        account.setUserId(userId);
        account.setAccountType(TYPE_WECHAT);
        account.setRealName(resolveWechatRealName(user, realname));
        account.setWechatOpenid(openid);
        account.setIsDefault(shouldDefault ? 1 : 0);
        account.setVerifyStatus(realname != null ? 1 : 0);
        account.setStatus(1);
        payAccountMapper.insert(account);

        log.info("用户 {} 绑定微信收款账户: id={}", userId, account.getId());
        return toVO(account);
    }

    /**
     * 获取用户账户列表
     */
    public List<PayAccountVO> getAccountList(Long userId) {
        List<PayAccount> list = payAccountMapper.selectList(
                new LambdaQueryWrapper<PayAccount>()
                        .eq(PayAccount::getUserId, userId)
                        .eq(PayAccount::getStatus, 1)
                        .orderByDesc(PayAccount::getIsDefault)
                        .orderByDesc(PayAccount::getCreatedTime));

        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 删除账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteAccount(Long userId, Long accountId) {
        PayAccount account = payAccountMapper.selectById(accountId);
        if (account == null || !account.getUserId().equals(userId) || !Integer.valueOf(1).equals(account.getStatus())) {
            throw new BusinessException(400, "账户不存在");
        }
        boolean wasDefault = (account.getIsDefault() == 1);
        account.setStatus(0);
        account.setIsDefault(0);
        payAccountMapper.updateById(account);
        // 如果删除的是默认账户，自动将最新一个设为默认
        if (wasDefault) {
            PayAccount latest = payAccountMapper.selectOne(
                    new LambdaQueryWrapper<PayAccount>()
                            .eq(PayAccount::getUserId, userId)
                            .eq(PayAccount::getStatus, 1)
                            .orderByDesc(PayAccount::getCreatedTime)
                            .last("LIMIT 1"));
            if (latest != null) {
                latest.setIsDefault(1);
                payAccountMapper.updateById(latest);
                log.info("自动设置默认账户: userId={}, accountId={}", userId, latest.getId());
            }
        }
        log.info("用户 {} 删除收款账户: id={}, wasDefault={}", userId, accountId, wasDefault);
    }

    /**
     * 设置默认账户
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultAccount(Long userId, Long accountId) {
        PayAccount account = payAccountMapper.selectById(accountId);
        if (account == null || !account.getUserId().equals(userId) || !Integer.valueOf(1).equals(account.getStatus())) {
            throw new BusinessException(400, "账户不存在");
        }
        // 取消所有默认
        clearDefault(userId);
        // 设置新默认
        account.setIsDefault(1);
        payAccountMapper.updateById(account);
        log.info("用户 {} 设置默认账户: id={}", userId, accountId);
    }

    /**
     * 获取默认账户
     */
    public PayAccountVO getDefaultAccount(Long userId) {
        PayAccount account = payAccountMapper.selectOne(
                new LambdaQueryWrapper<PayAccount>()
                        .eq(PayAccount::getUserId, userId)
                        .eq(PayAccount::getIsDefault, 1)
                        .eq(PayAccount::getStatus, 1));
        if (account == null) {
            // 如果没有默认，取最新一个
            account = payAccountMapper.selectOne(
                    new LambdaQueryWrapper<PayAccount>()
                            .eq(PayAccount::getUserId, userId)
                            .eq(PayAccount::getStatus, 1)
                            .orderByDesc(PayAccount::getCreatedTime)
                            .last("LIMIT 1"));
        }
        return account != null ? toVO(account) : null;
    }

    // ===================== 内部方法 =====================

    private void validateAccount(PayAccountAddDTO dto) {
        Integer type = dto.getAccountType();
        if (type == null || type < 1 || type > 3) {
            throw new BusinessException(400, "无效的账户类型");
        }
        if (type == TYPE_BANK) {
            if (dto.getBankName() == null || dto.getBankName().trim().isEmpty()) {
                throw new BusinessException(400, "开户银行不能为空");
            }
            if (dto.getBankCard() == null || dto.getBankCard().trim().isEmpty()) {
                throw new BusinessException(400, "银行卡号不能为空");
            }
            if (dto.getBankCard().trim().length() < 10) {
                throw new BusinessException(400, "银行卡号格式不正确");
            }
        }
        if (type == TYPE_ALIPAY && (dto.getAlipayAccount() == null || dto.getAlipayAccount().trim().isEmpty())) {
            throw new BusinessException(400, "支付宝账号不能为空");
        }
        if (type == TYPE_WECHAT && (dto.getWechatOpenid() == null || dto.getWechatOpenid().trim().isEmpty())) {
            throw new BusinessException(400, "微信授权信息不能为空");
        }
    }

    private boolean checkDuplicate(Long userId, PayAccountAddDTO dto) {
        LambdaQueryWrapper<PayAccount> wrapper = new LambdaQueryWrapper<PayAccount>()
                .eq(PayAccount::getUserId, userId)
                .eq(PayAccount::getStatus, 1);
        switch (dto.getAccountType()) {
            case TYPE_WECHAT -> wrapper.eq(PayAccount::getWechatOpenid, dto.getWechatOpenid());
            case TYPE_ALIPAY -> wrapper.eq(PayAccount::getAlipayAccount, dto.getAlipayAccount());
            case TYPE_BANK -> wrapper.eq(PayAccount::getBankCard, AESUtil.encrypt(dto.getBankCard().trim()));
        }
        return payAccountMapper.selectCount(wrapper) > 0;
    }

    private PayAccountVO toVO(PayAccount account) {
        String maskedRealName = account.getRealName() != null && account.getRealName().length() > 1
                ? account.getRealName().charAt(0) + "**" : account.getRealName();

        String maskedBankCard = null;
        if (account.getBankCard() != null && !account.getBankCard().isEmpty()) {
            try {
                String decrypted = AESUtil.decrypt(account.getBankCard());
                maskedBankCard = AESUtil.maskBankCard(decrypted);
            } catch (Exception e) {
                maskedBankCard = "****";
            }
        }

        String maskedAlipay = account.getAlipayAccount();
        if (maskedAlipay != null && maskedAlipay.contains("@")) {
            maskedAlipay = maskedAlipay.charAt(0) + "***" + maskedAlipay.substring(maskedAlipay.indexOf('@'));
        } else if (maskedAlipay != null && maskedAlipay.length() > 3) {
            maskedAlipay = maskedAlipay.substring(0, 3) + "****";
        }

        String maskedWechatOpenid = account.getWechatOpenid();
        if (maskedWechatOpenid != null && maskedWechatOpenid.length() > 8) {
            maskedWechatOpenid = maskedWechatOpenid.substring(0, 4)
                    + "****"
                    + maskedWechatOpenid.substring(maskedWechatOpenid.length() - 4);
        }

        return PayAccountVO.builder()
                .id(account.getId())
                .accountType(account.getAccountType())
                .accountTypeText(getTypeText(account.getAccountType()))
                .realName(maskedRealName)
                .idCard(account.getIdCard())
                .phone(account.getPhone() != null ? AESUtil.maskPhone(account.getPhone()) : null)
                .bankName(account.getBankName())
                .bankCard(maskedBankCard)
                .alipayAccount(maskedAlipay)
                .wechatOpenid(maskedWechatOpenid)
                .isDefault(Integer.valueOf(1).equals(account.getIsDefault()))
                .verifyStatus(account.getVerifyStatus())
                .createdTime(account.getCreatedTime())
                .icon(getTypeIcon(account.getAccountType()))
                .build();
    }

    private void clearDefault(Long userId) {
        payAccountMapper.update(null,
                new LambdaUpdateWrapper<PayAccount>()
                        .eq(PayAccount::getUserId, userId)
                        .eq(PayAccount::getIsDefault, 1)
                        .set(PayAccount::getIsDefault, 0));
    }

    private RealnameCertification getVerifiedRealname(Long userId) {
        return realnameCertMapper.selectOne(
                new LambdaQueryWrapper<RealnameCertification>()
                        .eq(RealnameCertification::getUserId, userId)
                        .eq(RealnameCertification::getStatus, 1)
                        .last("LIMIT 1"));
    }

    private String resolveWechatRealName(User user, RealnameCertification realname) {
        if (realname != null && realname.getRealName() != null && !realname.getRealName().isBlank()) {
            return realname.getRealName().trim();
        }
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname().trim();
        }
        return "微信用户";
    }
}
