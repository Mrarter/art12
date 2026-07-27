package com.shiyiju.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
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

    /** 艺术领域 */
    private String artField;

    /** 个人简介 (前端bio字段映射到此) */
    @Size(min = 10, max = 500, message = "简介长度需在10-500字之间")
    private String resume;

    /** 身份证正面照URL */
    private String idCardFront;

    /** 身份证反面照URL */
    private String idCardBack;

    /** 是否已完成人脸核验 */
    private Boolean faceVerified;

    /** 代表作图片URLs */
    @NotEmpty(message = "请上传代表作品")
    private List<String> artworks;

    /** 参展证明图片URLs (可选) */
    private List<String> exhibits;
}
