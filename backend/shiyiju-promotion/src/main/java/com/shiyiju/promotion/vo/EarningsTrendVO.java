package com.shiyiju.promotion.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 收益趋势 VO
 */
@Data
public class EarningsTrendVO implements Serializable {
    /** 标签（如日期） */
    private String label;
    
    /** 收益值 */
    private Long value;
}
