package com.shiyiju.user.vo;

import lombok.Data;

/**
 * 用户聚合数据 VO（个人中心）
 */
@Data
public class UserCenterVO {

    /** 用户信息 */
    private UserInfoVO userInfo;

    /** 待付款订单数 */
    private Integer pendingPayCount;

    /** 待发货订单数 */
    private Integer pendingShipCount;

    /** 待收货订单数 */
    private Integer pendingReceiveCount;

    /** 收藏作品数 */
    private Integer favoriteCount;

    /** 浏览足迹数 */
    private Integer historyCount;

    /** 优惠券数 */
    private Integer couponCount;
}
