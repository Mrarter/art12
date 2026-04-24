package com.shiyiju.common.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 艺术家信息VO - 作品关联艺术家时使用
 */
@Data
public class ArtistInfoVO implements Serializable {

    /** 用户ID */
    private Long userId;

    /** 昵称/艺术家名称 */
    private String nickname;

    /** 艺术家真名 */
    private String realName;

    /** 头像URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 个人简介 */
    private String bio;

    /** 个人履历 */
    private String resume;

    /** 地区 */
    private String region;

    /** 身份标签列表 */
    private List<String> identities;

    /** 认证状态: 0-待审核, 1-已通过, 2-已拒绝 */
    private Integer certStatus;

    /** 是否已认证艺术家 */
    @JsonProperty("isArtist")
    private Boolean isArtist;

    /** 艺术家身份类型：artist/master/collector/gallery */
    private String identityType;

    /** 粉丝数 */
    private Integer followerCount;

    /** 作品数 */
    private Integer artworkCount;

    /** 参展证明图片列表 */
    private List<String> exhibits;

    /** 代表作图片列表 */
    private List<String> artworks;

    /** 认证徽章文本 */
    private String badge;
}
