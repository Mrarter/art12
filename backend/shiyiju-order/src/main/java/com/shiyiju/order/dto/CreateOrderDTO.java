package com.shiyiju.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class CreateOrderDTO implements Serializable {
    /** 客户端生成的一次性下单请求号，用于防止重复提交 */
    private String requestId;
    private List<Long> cartIds;
    private Long artworkId;
    private Integer quantity;
    private Long addressId;
    private Long promoterId;
    private String remark;
}
