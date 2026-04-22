package com.shiyiju.promotion.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 达人排行榜 VO
 */
@Data
public class RankingVO implements Serializable {
    /** 用户ID */
    private Long userId;
    
    /** 排名 */
    private Integer rank;
    
    /** 昵称 */
    private String nickname;
    
    /** 头像 */
    private String avatar;
    
    /** 订单数 */
    private Integer orderCount;
    
    /** 收益金额 */
    private Long amount;
}
