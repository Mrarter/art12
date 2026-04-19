package com.shiyiju.user.vo;

import lombok.Data;

import java.util.List;

/**
 * 艺术家主页信息 VO
 */
@Data
public class ArtistHomepageVO {

    /** 用户ID */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 个人简介 */
    private String bio;

    /** 地区 */
    private String region;

    /** 身份标签 */
    private List<String> identities;

    /** 粉丝数 */
    private Integer followerCount;

    /** 关注数 */
    private Integer followingCount;

    /** 作品数 */
    private Integer artworkCount;

    /** 是否已认证艺术家 */
    private Boolean isArtist;

    /** 艺术家认证状态文本 */
    private String artistStatus;

    /** 个人履历 */
    private String resume;

    /** 代表作图片列表 */
    private List<String> artworks;

    /** 是否已关注 */
    private Boolean isFollowing;
}
