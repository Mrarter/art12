package com.shiyiju.user.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息 VO
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long id;
    
    /** 用户UID */
    private String uid;

    /** 昵称 */
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 性别 */
    private Integer gender;

    /** 简介 */
    private String bio;

    /** 地区 */
    private String region;

    /** 身份列表 */
    private List<String> identities;

    /** 是否艺术家 */
    private Boolean isArtist;

    /** 是否经纪人 */
    private Boolean isAgent;

    /** 是否收藏家 */
    private Boolean isCollector;

    /** 是否艺荐官 */
    private Boolean isPromoter;

    /** 粉丝数 */
    private Integer followerCount;

    /** 关注数 */
    private Integer followingCount;

    /** 注册时间 */
    private String registerTime;
}
