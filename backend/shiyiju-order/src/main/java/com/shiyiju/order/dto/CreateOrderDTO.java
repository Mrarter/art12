package com.shiyiju.order.dto;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class CreateOrderDTO implements Serializable {
    private List<Long> cartIds;
    private Long artworkId;
    private Integer quantity;
    private Long addressId;
    private String remark;
}
