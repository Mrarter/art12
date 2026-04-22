package com.shiyiju.order.vo;

import lombok.Data;
import java.io.Serializable;

/**
 * 收货地址 VO
 */
@Data
public class AddressVO implements Serializable {
    private String receiverName;
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private String fullAddress; // 完整地址（用于兼容）
}
