package com.shiyiju.user.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 拼音工具类 - 简单的ASCII首字母映射
 * 支持常用汉字到拼音首字母的转换
 */
public class PinyinUtil {

    // 常用汉字首字母映射表
    private static final Map<Character, String> PINYIN_MAP = new HashMap<>();

    static {
        // a
        putChars('a', "啊阿吖嗄腌锕");
        // o
        putChars('o', "哦");
        // e
        putChars('e', "额鳄鹅恶噩扼");
        // i
        putChars('i', "一衣铱依伊医颐夷遗仪宜咦彝姨移驿亿");
        // u
        putChars('u', "呜乌屋污巫无忧无吴吾芜梧");
        // v (ü)
        putChars('v', "迂淤吁");
        // b
        putChars('b', "吧芭八扒叭巴伯柏百摆败拜般搬板版办半绊邦帮榜绑棒宝饱保报抱暴豹卑杯悲背贝钡倍狈");
        // p
        putChars('p', "趴啪拍排牌裴赔陪配佩盆朋彭棚蓬鹏碰皮疲脾匹僻痞");
        // m
        putChars('m', "妈麻马吗码玛骂抹嘛蟆");
        // f
        putChars('f', "发罚阀法乏筏堡佛");
        // d
        putChars('d', "大答打达搭嗒妲大方待怠歹带代戴袋贷弹蛋当刀叨倒导岛蹈盗稻");
        // t
        putChars('t', "他她它塌塔挞踏台抬太态泰瘫弹汤唐糖膛塘");
        // n
        putChars('n', "那拿纳娜呐呢哪南难男囊恼闹");
        // l
        putChars('l', "啦拉垃辣喇蜡腊来赖莱蓝篮栏拦懒烂浪");
        // g
        putChars('g', "嘎噶尬夹旮");
        // k
        putChars('k', "咔咯");
        // h
        putChars('h', "哈呵嗨");
        // j
        putChars('j', "加夹家嘉佳枷荚颊嫁稼架假价驾");
        // q
        putChars('q', "七欺戚期漆栖凄齐其奇歧祈脐骑旗棋企启弃契气器汽砌");
        // x
        putChars('x', "西希息惜嘻吸锡溪熙膝稀犀");
        // zh
        putChars('z', "之芝支知只汁织职直值植殖执侄址指止旨至志治质灸滞");
        // ch
        putChars('c', "吃痴赤池弛耻齿尺斥翅");
        // sh
        putChars('s', "是时十市世狮施诗尸石拾时蚀食实识史使始驶屎士氏");
        // r
        putChars('r', "日惹热仁人任忍认刃仍");
    }

    private static void putChars(char pinyin, String chars) {
        for (int i = 0; i < chars.length(); i++) {
            PINYIN_MAP.put(chars.charAt(i), String.valueOf(pinyin));
        }
    }

    /**
     * 获取汉字的首字母拼音（大写）
     * 例如：张三 -> ZS
     */
    public static String getPinYinHeadChar(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chinese.length(); i++) {
            char c = chinese.charAt(i);
            // 判断是否为汉字（Unicode范围 4E00-9FFF）
            if (c >= 0x4E00 && c <= 0x9FA5) {
                String pinyin = PINYIN_MAP.get(c);
                if (pinyin != null) {
                    sb.append(pinyin.toUpperCase());
                }
            } else if (c >= 'a' && c <= 'z') {
                sb.append(Character.toUpperCase(c));
            } else if (c >= 'A' && c <= 'Z') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 获取汉字的完整拼音（简单实现，返回首字母）
     */
    public static String getPinYin(String chinese) {
        return getPinYinHeadChar(chinese);
    }

    /**
     * 检查拼音首字母是否匹配关键词
     */
    public static boolean matchesPinyinHead(String chinese, String keyword) {
        if (chinese == null || keyword == null) {
            return false;
        }
        String pinYinHead = getPinYinHeadChar(chinese);
        return pinYinHead.toLowerCase().startsWith(keyword.toLowerCase());
    }
}