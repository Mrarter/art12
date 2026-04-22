package com.shiyiju.promotion.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;

/**
 * 收益明细 VO
 */
@Data
public class EarningsDetailVO implements Serializable {
    private Long id;
    
    /** 订单标题/作品名称 */
    private String title;
    
    /** 收益金额 */
    private Long amount;
    
    /** 状态: settled-已结算, pending-待结算, cancelled-已取消 */
    private String status;
    
    /** 创建时间 */
    private String createTime;
    
    /** 订单ID */
    @JsonProperty("orderId")
    private Long orderId;
    
    /** 客户名称 */
    private String customerName;
    
    /** 客户头像 */
    private String customerAvatar;
    
    /** 佣金类型: order-订单佣金, team-团队奖励 */
    private String type;
    
    /** 佣金等级 */
    private Integer level;
}
