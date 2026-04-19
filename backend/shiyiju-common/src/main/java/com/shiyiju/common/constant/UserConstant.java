package com.shiyiju.common.constant;

/**
 * 用户身份常量
 */
public class UserConstant {

    /** 艺术家 */
    public static final String IDENTITY_ARTIST = "artist";

    /** 经纪人 */
    public static final String IDENTITY_AGENT = "agent";

    /** 收藏家/持有者 */
    public static final String IDENTITY_COLLECTOR = "collector";

    /** 艺荐官 */
    public static final String IDENTITY_PROMOTER = "promoter";

    /** 艺术家认证状态 - 待审核 */
    public static final Integer ARTIST_CERT_PENDING = 0;

    /** 艺术家认证状态 - 已通过 */
    public static final Integer ARTIST_CERT_APPROVED = 1;

    /** 艺术家认证状态 - 已拒绝 */
    public static final Integer ARTIST_CERT_REJECTED = 2;

    /** 经纪人认证状态 - 待审核 */
    public static final Integer AGENT_CERT_PENDING = 0;

    /** 经纪人认证状态 - 已通过 */
    public static final Integer AGENT_CERT_APPROVED = 1;

    /** 经纪人认证状态 - 已拒绝 */
    public static final Integer AGENT_CERT_REJECTED = 2;
}
