package com.shiyiju.common.constant;

/**
 * 分销/艺荐官常量
 */
public class PromoterConstant {

    /** 艺荐官等级 - 普通 */
    public static final Integer LEVEL_NORMAL = 1;

    /** 艺荐官等级 - 高级 */
    public static final Integer LEVEL_SENIOR = 2;

    /** 艺荐官等级 - 资深 */
    public static final Integer LEVEL_EXPERT = 3;

    /** 佣金状态 - 待结算 */
    public static final Integer COMMISSION_PENDING = 0;

    /** 佣金状态 - 已结算 */
    public static final Integer COMMISSION_SETTLED = 1;

    /** 佣金状态 - 已提现 */
    public static final Integer COMMISSION_WITHDRAWN = 2;

    /** 佣金状态 - 已失效（退款） */
    public static final Integer COMMISSION_INVALID = -1;

    /** 提现状态 - 待处理 */
    public static final Integer WITHDRAW_PENDING = 0;

    /** 提现状态 - 已通过 */
    public static final Integer WITHDRAW_APPROVED = 1;

    /** 提现状态 - 已拒绝 */
    public static final Integer WITHDRAW_REJECTED = 2;

    /** 提现状态 - 已打款 */
    public static final Integer WITHDRAW_TRANSFERRED = 3;

    /** 分销层级 - 一级 */
    public static final Integer LEVEL_FIRST = 1;

    /** 分销层级 - 二级 */
    public static final Integer LEVEL_SECOND = 2;
}
