package com.shiyiju.user.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /** 身份证号 */
    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^\\d{17}[\\dXx]$", message = "身份证号格式不正确")
    private String idCard;

    /** 艺术领域 */
    @NotBlank(message = "艺术领域不能为空")
    private String artField;

    /** 个人简介 (前端bio字段映射到此) */
    @NotBlank(message = "个人简介不能为空")
    @Size(min = 10, max = 500, message = "简介长度需在10-500字之间")
    private String resume;

    /** 代表作图片URLs */
    @NotEmpty(message = "请上传代表作品")
    private List<String> artworks;

    /** 参展证明图片URLs (可选) */
    private List<String> exhibits;
}
