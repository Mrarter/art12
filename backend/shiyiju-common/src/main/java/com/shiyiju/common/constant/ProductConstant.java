package com.shiyiju.common.constant;

/**
 * 商品/作品常量
 */
public class ProductConstant {

    /** 作品状态 - 上架 */
    public static final Integer STATUS_ON_SALE = 1;

    /** 作品状态 - 下架 */
    public static final Integer STATUS_OFF_SALE = 0;

    /** 作品状态 - 已售罄 */
    public static final Integer STATUS_SOLD_OUT = 2;

    /** 作品状态 - 已删除 */
    public static final Integer STATUS_DELETED = -1;

    /** 作品来源 - 艺术家发布 */
    public static final Integer SOURCE_ARTIST = 1;

    /** 作品来源 - 经纪人代理 */
    public static final Integer SOURCE_AGENT = 2;

    /** 作品来源 - 持有者转售 */
    public static final Integer SOURCE_COLLECTOR = 3;

    /** 作品来源 - 平台自营 */
    public static final Integer SOURCE_PLATFORM = 4;
}
