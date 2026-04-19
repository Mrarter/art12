package com.shiyiju.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 艺术家认证 DTO
 */
@Data
public class ArtistCertDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 个人履历 */
    private String resume;

    /** 代表作图片URLs */
    private List<String> artworks;

    /** 参展证明图片URLs */
    private List<String> exhibits;
}
