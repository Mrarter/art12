package com.shiyiju.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.constant.UserConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.user.util.UserIdUtil;
import org.springframework.jdbc.core.JdbcTemplate;
import com.shiyiju.common.util.JwtUtil;
import com.shiyiju.user.dto.WxLoginDTO;
import com.shiyiju.user.dto.ArtistCertDTO;
import com.shiyiju.user.entity.ArtistCertification;
import com.shiyiju.user.entity.ArtistProfile;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.entity.RealnameCertification;
import com.shiyiju.user.entity.User;
import com.shiyiju.common.entity.Address;
import com.shiyiju.user.mapper.ArtistCertificationMapper;
import com.shiyiju.user.mapper.ArtistProfileMapper;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import com.shiyiju.user.mapper.UserMapper;
import com.shiyiju.user.vo.LoginVO;
import com.shiyiju.user.vo.UserInfoVO;
import com.shiyiju.user.vo.ArtistCertStatusVO;
import com.shiyiju.user.util.PinyinUtil;
import com.shiyiju.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PromoterRecordMapper promoterRecordMapper;
    private final ArtistCertificationMapper artistCertMapper;
    private final ArtistProfileMapper artistProfileMapper;
    private final com.shiyiju.user.mapper.RealnameCertificationMapper realnameCertMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    
    @org.springframework.beans.factory.annotation.Value("${wechat.appid:}")
    private String wechatAppId;
    
    @org.springframework.beans.factory.annotation.Value("${wechat.secret:}")
    private String wechatSecret;

    /**
     * 启动时从 .env 文件加载微信密钥（IDE 开发环境兜底）
     * 优先级：系统环境变量 / application.yml > .env 文件
     */
    @jakarta.annotation.PostConstruct
    public void loadWechatConfigFromDotEnv() {
        boolean secretIsPlaceholder = wechatSecret == null || wechatSecret.isEmpty()
                || "your-wechat-secret".equals(wechatSecret);
        boolean appIdIsPlaceholder = wechatAppId == null || wechatAppId.isEmpty()
                || "your-wechat-appid".equals(wechatAppId);

        if (secretIsPlaceholder || appIdIsPlaceholder) {
            String projectRoot = System.getProperty("user.dir");
            // 向上查找 art12 项目根目录（支持从子模块启动）
            java.io.File dotEnv = findDotEnvFile(new java.io.File(projectRoot));
            if (dotEnv != null && dotEnv.exists()) {
                log.info("从 .env 文件加载微信配置: {}", dotEnv.getAbsolutePath());
                try {
                    java.util.Properties props = new java.util.Properties();
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.FileReader(dotEnv))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (line.isEmpty() || line.startsWith("#")) continue;
                            int eqIdx = line.indexOf('=');
                            if (eqIdx > 0) {
                                String key = line.substring(0, eqIdx).trim();
                                String value = line.substring(eqIdx + 1).trim();
                                props.setProperty(key, value);
                            }
                        }
                    }
                    if (secretIsPlaceholder && props.containsKey("WECHAT_SECRET")) {
                        String fromDotEnv = props.getProperty("WECHAT_SECRET");
                        if (!"your-wechat-secret".equals(fromDotEnv) && !fromEnvIsDefault(fromDotEnv)) {
                            wechatSecret = fromDotEnv;
                            log.info("从 .env 加载 wechat.secret 成功 (长度={})", wechatSecret.length());
                        }
                    }
                    if (appIdIsPlaceholder && props.containsKey("WECHAT_APPID")) {
                        String fromDotEnv = props.getProperty("WECHAT_APPID");
                        if (!"your-wechat-appid".equals(fromDotEnv)) {
                            wechatAppId = fromDotEnv;
                            log.info("从 .env 加载 wechat.appid 成功: {}", maskAppId(wechatAppId));
                        }
                    }
                } catch (Exception e) {
                    log.warn("读取 .env 文件失败: {}", e.getMessage());
                }
            } else {
                log.info("未找到 .env 文件 (搜索路径: {}), 使用 application.yml 配置", projectRoot);
            }
        }
    }

    private java.io.File findDotEnvFile(java.io.File startDir) {
        java.io.File current = startDir;
        for (int i = 0; i < 5 && current != null; i++) {
            java.io.File candidate = new java.io.File(current, ".env");
            if (candidate.exists() && candidate.isFile()) return candidate;
            current = current.getParentFile();
        }
        return null;
    }

    private boolean fromEnvIsDefault(String value) {
        return value == null || value.isEmpty() || "your-wechat-secret".equals(value)
                || value.contains("你的") || value.contains("请修改");
    }

    /**
     * 微信登录
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginVO wxLogin(WxLoginDTO dto) {
        // TODO: 调用微信接口获取 openid
        String openid = getOpenidFromWx(dto.getCode());
        
        // 查询用户是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getOpenid, openid)
        );

        boolean isNewUser = false;
        if (user == null) {
            // 创建新用户
            user = new User();
            user.setUid(UserIdUtil.generateUid()); // 生成用户UID
            user.setOpenid(openid);
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : "用户" + System.currentTimeMillis() % 10000);
            user.setAvatar(dto.getAvatar());
            user.setGender(dto.getGender() != null ? dto.getGender() : 0);
            user.setBirthday(dto.getBirthday());
            user.setRegion(dto.getRegion());
            user.setIdentities(UserConstant.IDENTITY_COLLECTOR); // 默认收藏家身份
            user.setStatus(1);
            user.setFollowerCount(0);
            user.setFollowingCount(0);
            user.setRegisterTime(LocalDateTime.now());
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.insert(user);
            isNewUser = true;

            // 处理邀请关系
            if (dto.getInviteCode() != null && !dto.getInviteCode().isEmpty()) {
                handleInvite(user.getId(), dto.getInviteCode());
            }
        } else {
            // 更新最后登录时间
            user.setLastLoginTime(LocalDateTime.now());
            user.setNickname(dto.getNickname() != null ? dto.getNickname() : user.getNickname());
            user.setAvatar(dto.getAvatar() != null ? dto.getAvatar() : user.getAvatar());
            userMapper.updateById(user);
        }

        // 生成 Token
        String token = JwtUtil.generateToken(user.getId(), openid);

        // 返回结果
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setIsNewUser(isNewUser);
        vo.setUserId(user.getId());
        vo.setUid(user.getUid());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setIdentities(user.getIdentities());
        vo.setOpenId(openid);

        // 将 Token 存入 Redis
        redisTemplate.opsForValue().set("token:" + user.getId(), token, 7, TimeUnit.DAYS);

        return vo;
    }

    /**
     * 获取用户信息
     */
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUid(user.getUid());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setGender(user.getGender());
        vo.setBio(user.getBio());
        vo.setRegion(user.getRegion());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setFollowingCount(user.getFollowingCount());
        vo.setRegisterTime(user.getRegisterTime() != null ? user.getRegisterTime().toString() : null);

        // 解析身份列表
        List<String> identityList = Arrays.asList(user.getIdentities().split(","));
        vo.setIdentities(identityList);
        vo.setIsArtist(identityList.contains(UserConstant.IDENTITY_ARTIST));
        vo.setIsAgent(identityList.contains(UserConstant.IDENTITY_AGENT));
        vo.setIsCollector(identityList.contains(UserConstant.IDENTITY_COLLECTOR));
        vo.setIsPromoter(identityList.contains(UserConstant.IDENTITY_PROMOTER));

        return vo;
    }

    /**
     * 更新用户信息
     */
    public void updateUserInfo(Long userId, User userUpdate) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        if (userUpdate.getNickname() != null) {
            user.setNickname(userUpdate.getNickname());
        }
        if (userUpdate.getAvatar() != null) {
            user.setAvatar(userUpdate.getAvatar());
        }
        if (userUpdate.getBio() != null) {
            user.setBio(userUpdate.getBio());
        }
        if (userUpdate.getGender() != null) {
            user.setGender(userUpdate.getGender());
        }
        if (userUpdate.getRegion() != null) {
            user.setRegion(userUpdate.getRegion());
        }
        if (userUpdate.getBirthday() != null) {
            user.setBirthday(userUpdate.getBirthday());
        }

        userMapper.updateById(user);
    }

    /**
     * 开通艺荐官
     */
    @Transactional(rollbackFor = Exception.class)
    public void openPromoter(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查是否已是艺荐官
        List<String> identities = Arrays.asList(user.getIdentities().split(","));
        if (identities.contains(UserConstant.IDENTITY_PROMOTER)) {
            throw new BusinessException("您已是艺荐官");
        }

        // 生成邀请码
        String inviteCode = generateInviteCode();

        // 创建艺荐官记录
        PromoterRecord record = new PromoterRecord();
        record.setUserId(userId);
        record.setLevel(1);
        record.setStatus(1);
        record.setSignTime(LocalDateTime.now());
        record.setInviteCode(inviteCode);
        record.setTotalSales(BigDecimal.ZERO);
        record.setTotalOrders(0);
        record.setTeamSize(0);
        promoterRecordMapper.insert(record);

        // 更新用户身份
        user.setIdentities(user.getIdentities() + "," + UserConstant.IDENTITY_PROMOTER);
        userMapper.updateById(user);
    }

    /**
     * 获取艺荐官邀请码
     */
    public String getPromoterInviteCode(Long userId) {
        PromoterRecord record = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getUserId, userId)
                        .eq(PromoterRecord::getStatus, 1)
        );
        if (record == null) {
            throw new BusinessException(ResultCode.PROMOTER_NOT_OPENED);
        }
        return record.getInviteCode();
    }

    /**
     * 处理邀请关系
     */
    private void handleInvite(Long userId, String inviteCode) {
        PromoterRecord inviterRecord = promoterRecordMapper.selectOne(
                new LambdaQueryWrapper<PromoterRecord>()
                        .eq(PromoterRecord::getInviteCode, inviteCode)
                        .eq(PromoterRecord::getStatus, 1)
        );

        if (inviterRecord != null) {
            // 设置邀请人
            PromoterRecord userRecord = promoterRecordMapper.selectOne(
                    new LambdaQueryWrapper<PromoterRecord>()
                            .eq(PromoterRecord::getUserId, userId)
            );
            if (userRecord != null) {
                userRecord.setParentId(inviterRecord.getUserId());
                promoterRecordMapper.updateById(userRecord);
            }

            // 增加邀请人团队人数
            inviterRecord.setTeamSize(inviterRecord.getTeamSize() + 1);
            promoterRecordMapper.updateById(inviterRecord);
        }
    }

    /**
     * 生成邀请码
     */
    private String generateInviteCode() {
        return "SYJ" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /**
     * 调用微信接口获取 openId，含密钥校验和 H5 开发降级
     * 文档: https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
     */
    private String getOpenidFromWx(String code) {
        if (code == null || code.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "微信授权码不能为空");
        }

        // ==== 第一步：校验微信密钥配置 ====
        validateWechatConfig();

        // ==== 第二步：H5 开发环境降级（不调用真实微信 API） ====
        if (isDevMockCode(code)) {
            return getDevMockOpenId(code);
        }

        // ==== 第三步：调用微信 code2Session 接口 ====
        try {
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                wechatAppId, wechatSecret, code
            );
            log.info("正在调用微信 code2Session, appid={}, code长度={}", maskAppId(wechatAppId), code.length());

            String response = cn.hutool.http.HttpUtil.get(url, 5000);
            log.debug("微信code2Session响应: {}", response);

            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(response);

            if (json.containsKey("openid")) {
                String openid = json.getString("openid");
                log.info("微信 code2Session 成功, openid={}", maskOpenId(openid));
                return openid;
            } else {
                Integer errcode = json.getInteger("errcode");
                String errmsg = json.getString("errmsg");
                log.error("微信code2Session 返回错误: errcode={}, errmsg={}, appid={}",
                        errcode, errmsg, maskAppId(wechatAppId));
                throw new BusinessException(ResultCode.PARAM_ERROR, 
                    "微信登录失败(" + errcode + "): " + errmsg);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用微信code2Session接口异常, appid={}", maskAppId(wechatAppId), e);
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "微信登录服务异常，请稍后重试");
        }
    }

    /**
     * 校验微信密钥配置是否有效
     * 配置缺失时抛出明确错误，配置异常时输出详细日志
     */
    private void validateWechatConfig() {
        // 检查 appId
        boolean appIdMissing = wechatAppId == null || wechatAppId.isEmpty() 
                || "your-wechat-appid".equals(wechatAppId) 
                || wechatAppId.startsWith("wx") == false;
        
        // 检查 secret
        boolean secretMissing = wechatSecret == null || wechatSecret.isEmpty() 
                || "your-wechat-secret".equals(wechatSecret);
        
        if (appIdMissing || secretMissing) {
            StringBuilder sb = new StringBuilder("微信小程序密钥配置不完整：");
            if (wechatAppId == null || wechatAppId.isEmpty()) {
                sb.append("[appId=空]");
            } else if ("your-wechat-appid".equals(wechatAppId)) {
                sb.append("[appId=默认占位值]");
            } else if (!wechatAppId.startsWith("wx")) {
                sb.append("[appId格式异常: ").append(maskAppId(wechatAppId)).append("]");
            } else {
                sb.append("[appId=").append(maskAppId(wechatAppId)).append("✓]");
            }
            if (wechatSecret == null || wechatSecret.isEmpty()) {
                sb.append("[secret=空]");
            } else if ("your-wechat-secret".equals(wechatSecret)) {
                sb.append("[secret=默认占位值]");
            } else {
                sb.append("[secret=已配置✓]");
            }
            log.error("微信登录配置校验失败: {}", sb);
            throw new BusinessException(500, 
                "微信登录服务暂不可用，请联系管理员配置小程序密钥。\n" + sb.toString());
        }
    }

    /**
     * 判断是否为 H5 开发环境的 mock 授权码
     * H5 环境无法调用 uni.login 获取真实 code，使用 h5_dev_ 前缀标识
     */
    private boolean isDevMockCode(String code) {
        return code != null && code.startsWith("h5_dev_");
    }

    /**
     * H5 开发环境：使用 mock openId（不调用微信API）
     */
    private String getDevMockOpenId(String code) {
        String mockOpenId = "mock_openid_" + code.substring("h5_dev_".length());
        log.warn("H5 开发模式：使用 mock openId={}（请在生产环境配置真实微信密钥）", maskOpenId(mockOpenId));
        return mockOpenId;
    }

    /**
     * 脱敏 appId：保留前4位和后4位
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 8) return appId;
        return appId.substring(0, 4) + "****" + appId.substring(appId.length() - 4);
    }

    /**
     * 脱敏 openId：保留前4位和后4位  
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() <= 8) return openId;
        return openId.substring(0, 4) + "****" + openId.substring(openId.length() - 4);
    }

    /**
     * 绑定手机号
     */
    public void bindPhone(Long userId, String phone, String verifyCode) {
        // TODO: 验证短信验证码
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPhone(phone);
        userMapper.updateById(user);
    }

    /**
     * 艺术家认证申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyArtistCert(Long userId, ArtistCertDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        // 检查是否已有待审核或已通过的申请
        ArtistCertification existing = artistCertMapper.selectOne(
                new LambdaQueryWrapper<ArtistCertification>()
                        .eq(ArtistCertification::getUserId, userId)
                        .in(ArtistCertification::getStatus, 
                            UserConstant.ARTIST_CERT_PENDING, 
                            UserConstant.ARTIST_CERT_APPROVED)
        );
        if (existing != null) {
            throw new BusinessException("您已有认证申请，无需重复提交");
        }

        // 创建认证申请
        ArtistCertification cert = new ArtistCertification();
        cert.setUserId(userId);
        cert.setRealName(dto.getRealName());
        cert.setIdCard(dto.getIdCard());
        cert.setResume(dto.getResume());
        cert.setArtworks(dto.getArtworks() != null ? String.join(",", dto.getArtworks()) : null);
        cert.setExhibits(dto.getExhibits() != null ? String.join(",", dto.getExhibits()) : null);
        cert.setStatus(UserConstant.ARTIST_CERT_PENDING);
        cert.setCreateTime(LocalDateTime.now());
        cert.setUpdateTime(LocalDateTime.now());
        artistCertMapper.insert(cert);
    }

    /**
     * 获取艺术家认证状态
     */
    public ArtistCertStatusVO getArtistCertStatus(Long userId) {
        ArtistCertStatusVO vo = new ArtistCertStatusVO();
        
        // 查询最新认证记录
        ArtistCertification cert = artistCertMapper.selectOne(
                new LambdaQueryWrapper<ArtistCertification>()
                        .eq(ArtistCertification::getUserId, userId)
                        .orderByDesc(ArtistCertification::getCreateTime)
                        .last("LIMIT 1")
        );

        if (cert == null) {
            vo.setStatus(null);
            vo.setStatusText("未申请");
            vo.setIsArtist(false);
        } else {
            vo.setStatus(cert.getStatus());
            vo.setStatusText(getCertStatusText(cert.getStatus()));
            vo.setRejectReason(cert.getRejectReason());
            vo.setReviewTime(cert.getReviewTime() != null ? cert.getReviewTime().toString() : null);
            vo.setIsArtist(cert.getStatus().equals(UserConstant.ARTIST_CERT_APPROVED));
        }

        // 同时检查用户身份
        User user = userMapper.selectById(userId);
        if (user != null && user.getIdentities() != null) {
            List<String> identityList = Arrays.asList(user.getIdentities().split(","));
            if (identityList.contains(UserConstant.IDENTITY_ARTIST)) {
                vo.setIsArtist(true);
            }
        }

        return vo;
    }

    private String getCertStatusText(Integer status) {
        if (status == null) return "未申请";
        if (status.equals(UserConstant.ARTIST_CERT_PENDING)) return "待审核";
        if (status.equals(UserConstant.ARTIST_CERT_APPROVED)) return "已通过";
        if (status.equals(UserConstant.ARTIST_CERT_REJECTED)) return "已拒绝";
        return "未知";
    }

    /**
     * 关注艺术家
     */
    @Transactional(rollbackFor = Exception.class)
    public void followArtist(Long userId, Long artistId) {
        if (userId.equals(artistId)) {
            throw new BusinessException("不能关注自己");
        }
        
        // 增加艺术家的粉丝数
        User artist = userMapper.selectById(artistId);
        if (artist == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        artist.setFollowerCount(artist.getFollowerCount() == null ? 1 : artist.getFollowerCount() + 1);
        artist.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(artist);
        
        // 增加关注者的关注数
        User user = userMapper.selectById(userId);
        user.setFollowingCount(user.getFollowingCount() == null ? 1 : user.getFollowingCount() + 1);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        
        // 存储关注关系到 Redis
        redisTemplate.opsForSet().add("follow:" + userId, artistId);
    }

    /**
     * 取消关注艺术家
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfollowArtist(Long userId, Long artistId) {
        // 减少艺术家的粉丝数
        User artist = userMapper.selectById(artistId);
        if (artist != null && artist.getFollowerCount() != null && artist.getFollowerCount() > 0) {
            artist.setFollowerCount(artist.getFollowerCount() - 1);
            artist.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(artist);
        }
        
        // 减少关注者的关注数
        User user = userMapper.selectById(userId);
        if (user != null && user.getFollowingCount() != null && user.getFollowingCount() > 0) {
            user.setFollowingCount(user.getFollowingCount() - 1);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
        
        // 从 Redis 中移除
        redisTemplate.opsForSet().remove("follow:" + userId, artistId);
    }

    /**
     * 获取艺术家主页信息
     */
    public Map<String, Object> getArtistHomepage(Long artistId) {
        Map<String, Object> data = buildArtistPublicData(artistId);
        if (data == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return data;
    }

    /**
     * 获取艺术家详细信息（用于作品关联）
     * 返回完整的艺术家信息供作品服务使用
     */
    public Map<String, Object> getArtistInfo(Long artistId) {
        return buildArtistPublicData(artistId);
    }

    private Map<String, Object> buildArtistPublicData(Long artistId) {
        User artist = userMapper.selectById(artistId);
        if (artist == null) {
            return null;
        }

        Map<String, Object> profile = loadArtistProfile(artistId);
        Map<String, Object> account = loadArtistAccount(artistId, firstNonBlank(stringValue(profile.get("userUid")), artist.getUid()));
        List<String> identityList = splitToList(firstNonBlank(stringValue(account.get("identities")), artist.getIdentities()));
        List<Map<String, Object>> works = loadArtistWorks(artistId, 6);
        int favoriteCount = works.stream()
            .map(item -> item.get("favoriteCount"))
            .filter(Number.class::isInstance)
            .map(Number.class::cast)
            .mapToInt(Number::intValue)
            .sum();

        String profileResume = stringValue(profile.get("resume"));
        String profileBio = stringValue(profile.get("bio"));
        boolean profileApproved = toInt(profile.get("status"), 0) == UserConstant.ARTIST_CERT_APPROVED;
        boolean isArtist = profileApproved || identityList.contains(UserConstant.IDENTITY_ARTIST);
        String nickname = firstNonBlank(stringValue(account.get("nickname")), stringValue(profile.get("realName")), artist.getNickname(), "艺术家");
        String avatar = firstNonBlank(stringValue(account.get("avatar")), artist.getAvatar(), "/static/images/artist-avatar.png");
        String phone = firstNonBlank(stringValue(account.get("phone")), artist.getPhone());
        String intro = firstNonBlank(profileBio, profileResume, artist.getBio(), "暂未补充艺术家介绍");
        String artistTitle = firstNonBlank(stringValue(profile.get("artistTitle")), stringValue(profile.get("artistLevel")), isArtist ? "认证艺术家" : "");
        List<String> tags = mergeTags(profile.get("artistTags"), determinePublicTags(identityList, profile, works));
        String homepageCover = firstNonBlank(stringValue(profile.get("homepageCover")), works.isEmpty() ? "" : stringValue(works.get(0).get("cover")), avatar);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", artist.getId());
        data.put("id", artist.getId());
        data.put("uid", firstNonBlank(stringValue(account.get("uid")), artist.getUid()));
        data.put("nickname", nickname);
        data.put("realName", firstNonBlank(stringValue(profile.get("realName")), nickname));
        data.put("avatar", avatar);
        data.put("phone", phone);
        data.put("bio", intro);
        data.put("resume", profileResume);
        data.put("region", artist.getRegion());
        data.put("identities", identityList);
        data.put("isArtist", isArtist);
        data.put("identityType", isArtist ? "artist" : "collector");
        data.put("artistTitle", artistTitle);
        data.put("title", artistTitle);
        data.put("artistTags", tags);
        data.put("tags", tags);
        data.put("homepageCover", homepageCover);
        data.put("cover", homepageCover);
        data.put("followerCount", artist.getFollowerCount() != null ? artist.getFollowerCount() : 0);
        data.put("followingCount", artist.getFollowingCount() != null ? artist.getFollowingCount() : 0);
        data.put("collectCount", favoriteCount);
        data.put("favoriteCount", favoriteCount);
        data.put("fansCount", artist.getFollowerCount() != null ? artist.getFollowerCount() : 0);
        data.put("artworkCount", works.size());
        data.put("workCount", works.size());
        data.put("works", works);
        data.put("artworks", works);
        data.put("quote", "");

        ArtistCertification cert = null;
        if (tableExists("artist_certifications")) {
            cert = artistCertMapper.selectOne(
                new LambdaQueryWrapper<ArtistCertification>()
                    .eq(ArtistCertification::getUserId, artistId)
                    .eq(ArtistCertification::getStatus, UserConstant.ARTIST_CERT_APPROVED)
            );
        }
        if (cert != null) {
            data.put("certStatus", cert.getStatus());
            data.put("artistCode", cert.getArtistCode());
            data.put("certified", true);
            data.put("badge", determineBadge(firstNonBlank(cert.getRealName(), artist.getNickname()), identityList));
        } else {
            boolean certified = profileApproved
                || identityList.contains(UserConstant.IDENTITY_ARTIST)
                || tags.contains("平台认证");
            data.put("certStatus", certified ? UserConstant.ARTIST_CERT_APPROVED : null);
            data.put("artistCode", profile.get("artistCode"));
            data.put("certified", certified);
            data.put("badge", firstNonBlank(artistTitle, determineBadge(artist.getNickname(), identityList)));
        }

        return data;
    }

    private Map<String, Object> loadArtistProfile(Long artistId) {
        if (!tableExists("artist_profile")) {
            return Map.of();
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            """
                SELECT user_id,
                       COALESCE(real_name, artist_name) AS real_name,
                       COALESCE(bio, resume) AS bio,
                       resume,
                       user_uid,
                       status,
                       artist_level,
                       artist_code,
                       artist_title,
                       homepage_cover,
                       artist_tags
                FROM artist_profile
                WHERE user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """,
            artistId
        );
        if (rows.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("realName", row.get("real_name"));
        profile.put("bio", row.get("bio"));
        profile.put("resume", row.get("resume"));
        profile.put("userUid", row.get("user_uid"));
        profile.put("status", row.get("status"));
        profile.put("artistLevel", row.get("artist_level"));
        profile.put("artistCode", row.get("artist_code"));
        profile.put("artistTitle", row.get("artist_title"));
        profile.put("homepageCover", row.get("homepage_cover"));
        profile.put("artistTags", splitToList(stringValue(row.get("artist_tags"))));
        return profile;
    }

    private Map<String, Object> loadArtistAccount(Long artistId, String userUid) {
        String accountTable = firstExistingTable("sys_user", "user_account");
        if (accountTable == null) {
            return Map.of();
        }

        String idColumn = Objects.requireNonNullElse(firstExistingColumn(accountTable, "user_id", "id"), "id");
        String uidColumn = firstExistingColumn(accountTable, "user_uid", "uid");
        String nicknameColumn = firstExistingColumn(accountTable, "nickname", "name");
        String avatarColumn = firstExistingColumn(accountTable, "avatar", "avatar_url");
        String phoneColumn = firstExistingColumn(accountTable, "phone", "mobile");
        String identitiesColumn = firstExistingColumn(accountTable, "identities", "identity_json", "identity");

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(uidColumn != null ? uidColumn : "NULL").append(" AS uid, ");
        sql.append(nicknameColumn != null ? nicknameColumn : "NULL").append(" AS nickname, ");
        sql.append(avatarColumn != null ? avatarColumn : "NULL").append(" AS avatar, ");
        sql.append(phoneColumn != null ? phoneColumn : "NULL").append(" AS phone, ");
        sql.append(identitiesColumn != null ? identitiesColumn : "NULL").append(" AS identities ");
        sql.append("FROM ").append(accountTable).append(" WHERE ").append(idColumn).append(" = ?");

        List<Object> args = new ArrayList<>();
        args.add(artistId);
        if (uidColumn != null && userUid != null && !userUid.isBlank()) {
            sql.append(" OR ").append(uidColumn).append(" = ?");
            args.add(userUid);
        }
        sql.append(" ORDER BY ").append(idColumn).append(" DESC LIMIT 1");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private List<Map<String, Object>> loadArtistWorks(Long artistId, int limit) {
        String artworkTable = firstExistingTable("artwork", "artworks");
        if (artworkTable == null) {
            return List.of();
        }
        String coverColumn = Objects.requireNonNullElse(firstExistingColumn(artworkTable, "cover", "cover_image", "image", "thumbnail"), "NULL");
        String materialColumn = Objects.requireNonNullElse(firstExistingColumn(artworkTable, "art_type", "medium"), "NULL");
        String favoriteColumn = firstExistingColumn(artworkTable, "favorite_count");
        String sizeColumn = Objects.requireNonNullElse(firstExistingColumn(artworkTable, "size"), "NULL");
        String yearColumn = Objects.requireNonNullElse(firstExistingColumn(artworkTable, "year"), "NULL");
        String orderColumn = firstExistingColumn(artworkTable, "weight", "create_time", "id");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            ("SELECT id, title, " + coverColumn + " AS cover, " + materialColumn + " AS material, "
                + sizeColumn + " AS size, " + yearColumn + " AS year, price, "
                + (favoriteColumn != null ? favoriteColumn : "0") + " AS favorite_count "
                + "FROM " + artworkTable + " WHERE author_id = ? ORDER BY " + orderColumn + " DESC LIMIT ?"),
            artistId,
            limit
        );
        List<Map<String, Object>> works = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", row.get("id"));
            item.put("title", row.get("title"));
            item.put("cover", row.get("cover"));
            item.put("material", row.get("material"));
            item.put("size", row.get("size"));
            item.put("year", row.get("year"));
            item.put("price", row.get("price"));
            item.put("priceText", row.get("price") == null ? "" : "¥" + formatFen(row.get("price")));
            item.put("favoriteCount", toInt(row.get("favorite_count"), 0));
            works.add(item);
        }
        return works;
    }

    private List<String> determinePublicTags(List<String> identities, Map<String, Object> profile, List<Map<String, Object>> works) {
        List<String> tags = new ArrayList<>();
        if (identities.contains(UserConstant.IDENTITY_ARTIST)) {
            tags.add("平台认证");
        }
        String level = stringValue(profile.get("artistLevel"));
        if (!level.isBlank()) {
            tags.add(level);
        }
        if (!works.isEmpty()) {
            String material = stringValue(works.get(0).get("material"));
            if (!material.isBlank()) {
                tags.add(material);
            }
        }
        return tags;
    }

    private List<String> mergeTags(Object primary, List<String> fallback) {
        LinkedHashSet<String> merged = new LinkedHashSet<>(splitToList(primary));
        merged.addAll(fallback);
        return new ArrayList<>(merged);
    }

    private List<String> splitToList(Object rawValue) {
        String raw = stringValue(rawValue);
        if (raw.isBlank()) {
            return new ArrayList<>();
        }
        String cleaned = raw.replace("[", "").replace("]", "").replace("\"", "");
        return Arrays.stream(cleaned.split("[,，|\\n]"))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .distinct()
            .toList();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstExistingTable(String... tableNames) {
        for (String tableName : tableNames) {
            if (tableExists(tableName)) {
                return tableName;
            }
        }
        return null;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
            Integer.class,
            tableName
        );
        return count != null && count > 0;
    }

    private String firstExistingColumn(String tableName, String... columnNames) {
        for (String columnName : columnNames) {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
            );
            if (count != null && count > 0) {
                return columnName;
            }
        }
        return null;
    }

    private String formatFen(Object value) {
        long amount = value instanceof Number number ? number.longValue() : 0L;
        return String.format("%,d", Math.round(amount / 100.0d));
    }

    private int toInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return value == null ? fallback : Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * 根据身份推断徽章
     */
    private String determineBadge(String name, List<String> identities) {
        if (identities.contains(UserConstant.IDENTITY_ARTIST)) {
            return "认证艺术家";
        }
        if (identities.contains("collector")) {
            return "资深藏家";
        }
        if (identities.contains("gallery")) {
            return "艺术机构";
        }
        return "艺术爱好者";
    }

    /**
     * 获取用户个人中心聚合数据
     */
    public Map<String, Object> getUserCenter(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Map<String, Object> data = new HashMap<>();
        
        // 用户基本信息
        data.put("userId", user.getId());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        
        List<String> identityList = Arrays.asList(user.getIdentities().split(","));
        data.put("isArtist", identityList.contains(UserConstant.IDENTITY_ARTIST));
        data.put("isPromoter", identityList.contains(UserConstant.IDENTITY_PROMOTER));
        
        // TODO: 从订单服务获取订单数量
        data.put("pendingPayCount", 0);
        data.put("pendingShipCount", 0);
        data.put("pendingReceiveCount", 0);
        
        // TODO: 从收藏表获取收藏数
        data.put("favoriteCount", 0);
        
        // TODO: 从足迹表获取足迹数
        data.put("historyCount", 0);
        
        return data;
    }

    /**
     * 检查是否已关注艺术家
     */
    public Boolean isFollowing(Long userId, Long artistId) {
        Boolean following = (Boolean) redisTemplate.opsForSet().isMember("follow:" + userId, artistId);
        return following != null && following;
    }

    /**
     * 获取艺术家作品列表
     * 注意：需要关联商品服务获取作品数据，这里简化处理
     */
    public PageResult<Map<String, Object>> getArtistWorks(Long artistId, Integer page, Integer pageSize) {
        // TODO: 调用商品服务获取艺术家的作品
        // 目前返回空列表，实际应通过 Feign 调用 shiyiju-product 服务
        return PageResult.of(0L, page, pageSize, List.of());
    }

    /**
     * 获取艺术家动态
     * 包括发布的帖子、成交记录等
     */
    public PageResult<Map<String, Object>> getArtistMoments(Long artistId, Integer page, Integer pageSize) {
        // TODO: 调用社区服务获取艺术家发布的帖子
        // 目前返回空列表，实际应通过 Feign 调用 shiyiju-community 服务
        return PageResult.of(0L, page, pageSize, List.of());
    }

    /**
     * 获取收货地址列表
     * 注意：需要关联订单服务获取地址数据，这里简化处理
     */
    public List<Address> getAddressList(Long userId) {
        // TODO: 调用订单服务获取收货地址
        // 目前返回空列表，实际应通过 Feign 调用 shiyiju-order 服务
        return List.of();
    }

    /**
     * 添加收货地址
     */
    public void addAddress(Long userId, Address address) {
        // TODO: 调用订单服务添加收货地址
    }

    /**
     * 更新收货地址
     */
    public void updateAddress(Long addressId, Long userId, Address address) {
        // TODO: 调用订单服务更新收货地址
    }

    /**
     * 删除收货地址
     */
    public void deleteAddress(Long addressId, Long userId) {
        // TODO: 调用订单服务删除收货地址
    }

        /**
     * 搜索艺术家
     * 根据名称模糊搜索艺术家，优先从 artist_profile 表查询，保持与后台艺术家管理一致
     * 支持中文和拼音首字母搜索
     */
    /**
     * 搜索艺术家
     * 根据名称模糊搜索艺术家，同时查询 artist_profile 和 artist_certifications 两个表
     * 支持中文和拼音首字母搜索
     */
    public List<Map<String, Object>> searchArtists(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        String trimmedKeyword = keyword.trim();
        boolean isPinyinSearch = isPinyinSearch(trimmedKeyword);
        LinkedHashMap<Long, User> matchedUsers = new LinkedHashMap<>();
        Map<Long, ArtistProfile> profileMap = new HashMap<>();
        Map<Long, ArtistCertification> certMap = new HashMap<>();

        // 1. 从 artist_profile 表查询
        try {
            List<ArtistProfile> profiles = artistProfileMapper.selectList(
                new LambdaQueryWrapper<ArtistProfile>()
                    .and(w -> w.like(ArtistProfile::getRealName, trimmedKeyword)
                              .or()
                              .like(ArtistProfile::getArtistName, trimmedKeyword))
                    .orderByDesc(ArtistProfile::getUpdatedAt)
                    .last("LIMIT " + limit)
            );

            for (ArtistProfile profile : profiles) {
                profileMap.put(profile.getUserId(), profile);
                User user = findUserByProfile(profile);
                if (user != null) {
                    matchedUsers.put(user.getId(), user);
                }
            }
        } catch (Exception e) {
            log.warn("从 artist_profile 表查询失败: {}", e.getMessage());
        }

        // 2. 从 artist_certifications 表查询（待审核和已认证的艺术家）
        try {
            List<ArtistCertification> certs = artistCertMapper.selectList(
                new LambdaQueryWrapper<ArtistCertification>()
                    .and(w -> w.like(ArtistCertification::getRealName, trimmedKeyword)
                              .or()
                              .like(ArtistCertification::getArtistCode, trimmedKeyword))
                    .in(ArtistCertification::getStatus, 0, 1) // 查询待审核(0)和已认证(1)的艺术家
                    .orderByDesc(ArtistCertification::getUpdateTime)
                    .last("LIMIT " + limit)
            );

            for (ArtistCertification cert : certs) {
                certMap.put(cert.getUserId(), cert);
                User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .eq(User::getId, cert.getUserId())
                );
                if (user != null) {
                    matchedUsers.put(user.getId(), user);
                }
            }
        } catch (Exception e) {
            log.warn("从 artist_certifications 表查询失败: {}", e.getMessage());
        }

        // 3. 如果艺术家表没有匹配，搜索 user_account 表
        if (matchedUsers.isEmpty()) {
            if (isPinyinSearch) {
                userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .orderByDesc(User::getCreateTime)
                        .last("LIMIT 500")
                ).stream()
                    .filter(user -> user.getNickname() != null
                            && PinyinUtil.matchesPinyinHead(user.getNickname(), trimmedKeyword))
                    .limit(limit)
                    .forEach(user -> matchedUsers.put(user.getId(), user));
            } else {
                userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .like(User::getNickname, trimmedKeyword)
                        .orderByDesc(User::getCreateTime)
                        .last("LIMIT " + limit)
                ).forEach(user -> matchedUsers.put(user.getId(), user));
            }
        }

        List<User> users = matchedUsers.values().stream().limit(limit).toList();

        // 预加载 artist_profile 数据
        try {
            List<Long> userIds = users.stream().map(User::getId).distinct().toList();
            if (!userIds.isEmpty()) {
                artistProfileMapper.selectList(
                    new LambdaQueryWrapper<ArtistProfile>()
                        .in(ArtistProfile::getUserId, userIds)
                ).forEach(p -> profileMap.put(p.getUserId(), p));
            }
        } catch (Exception e) {
            log.warn("预加载 artist_profile 失败: {}", e.getMessage());
        }

        return users.stream()
            .map(user -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("uid", user.getUid());
                map.put("nickname", user.getNickname());
                map.put("avatar", user.getAvatar());
                map.put("bio", user.getBio());
                
                ArtistProfile profile = profileMap.get(user.getId());
                ArtistCertification cert = certMap.get(user.getId());
                
                // 显示名称：优先用 profile.real_name > cert.real_name > nickname
                String displayName = null;
                if (profile != null && profile.getRealName() != null && !profile.getRealName().isEmpty()) {
                    displayName = profile.getRealName();
                } else if (cert != null && cert.getRealName() != null && !cert.getRealName().isEmpty()) {
                    displayName = cert.getRealName();
                }
                if (displayName == null || displayName.isEmpty()) {
                    displayName = user.getNickname();
                }
                map.put("name", displayName);
                
                boolean isCertified = (profile != null && profile.getStatus() != null && profile.getStatus() == 1)
                    || (cert != null && cert.getStatus() != null && cert.getStatus() == 1);
                map.put("certified", isCertified);
                
                String artistCode = null;
                if (profile != null) {
                    artistCode = profile.getArtistCode();
                } else if (cert != null) {
                    artistCode = cert.getArtistCode();
                }
                map.put("artistCode", artistCode);
                map.put("badge", displayName);
                
                return map;
            }).toList();
    }
    
    /**
     * 根据 ArtistProfile 查找对应的 User
     */
    private User findUserByProfile(ArtistProfile profile) {
        if (profile.getUserId() == null && (profile.getUserUid() == null || profile.getUserUid().isEmpty())) {
            return null;
        }
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getDeleted, 0)
                .and(w -> w.eq(User::getId, profile.getUserId())
                          .or()
                          .eq(User::getUid, profile.getUserUid()))
        );
    }


    /**
     * 判断是否为拼音搜索（输入的是英文字母）
     */
    private boolean isPinyinSearch(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return false;
        }
        // 如果全是英文字母，则认为是拼音搜索
        return keyword.matches("^[a-zA-Z]+$");
    }

    /**
     * 查找或创建艺术家
     * 如果艺术家存在则返回，不存在则创建未审核状态的艺术家
     * 搜索顺序：artist_profile -> artist_certifications -> user_account
     * 确保与 searchArtists 方法使用一致的搜索逻辑
     * 
     * 创建时同时创建 artist_profile 记录（待审核状态），确保艺术家管理列表可见
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> findOrCreateArtist(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "艺术家名称不能为空");
        }

        String trimmedName = name.trim();
        Map<String, Object> result = new HashMap<>();

        // 1. 先从 artist_profile 表查询（主表）
        try {
            ArtistProfile profile = artistProfileMapper.selectOne(
                new LambdaQueryWrapper<ArtistProfile>()
                    .and(w -> w.eq(ArtistProfile::getRealName, trimmedName)
                              .or()
                              .eq(ArtistProfile::getArtistName, trimmedName))
                    .last("LIMIT 1")
            );
            if (profile != null && profile.getUserId() != null) {
                User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .and(w -> w.eq(User::getId, profile.getUserId())
                                  .or()
                                  .eq(User::getUid, profile.getUserUid()))
                );
                if (user != null) {
                    String displayName = profile.getRealName() != null && !profile.getRealName().isEmpty()
                        ? profile.getRealName() : user.getNickname();
                    result.put("id", user.getId());
                    result.put("uid", user.getUid());
                    result.put("name", displayName);
                    result.put("avatar", user.getAvatar());
                    result.put("artistCode", profile.getArtistCode());
                    result.put("exists", true);
                    result.put("certified", profile.getStatus() != null &&
                        (profile.getStatus().equals("ACTIVE") || profile.getStatus().equals(1)));
                    result.put("source", "artist_profile");
                    log.info("从 artist_profile 表找到艺术家: name={}, userId={}", trimmedName, user.getId());
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("从 artist_profile 表查询失败: {}", e.getMessage());
        }

        // 2. 再从 artist_certifications 表查询（认证表）
        try {
            ArtistCertification cert = artistCertMapper.selectOne(
                new LambdaQueryWrapper<ArtistCertification>()
                    .eq(ArtistCertification::getRealName, trimmedName)
                    .eq(ArtistCertification::getStatus, 1) // 只查询已认证的
                    .last("LIMIT 1")
            );
            if (cert != null && cert.getUserId() != null) {
                User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .eq(User::getId, cert.getUserId())
                );
                if (user != null) {
                    result.put("id", user.getId());
                    result.put("uid", user.getUid());
                    result.put("name", cert.getRealName());
                    result.put("avatar", user.getAvatar());
                    result.put("artistCode", cert.getArtistCode());
                    result.put("exists", true);
                    result.put("certified", true);
                    result.put("source", "artist_certifications");
                    log.info("从 artist_certifications 表找到艺术家: name={}, userId={}", trimmedName, user.getId());
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("从 artist_certifications 表查询失败: {}", e.getMessage());
        }

        // 3. 最后查询 user_account 表
        try {
            User existingUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getNickname, trimmedName)
                    .eq(User::getDeleted, 0)
                    .last("LIMIT 1")
            );
            if (existingUser != null) {
                // 如果用户已存在，检查是否有 artist_profile 记录
                ArtistProfile existingProfile = artistProfileMapper.selectOne(
                    new LambdaQueryWrapper<ArtistProfile>()
                        .eq(ArtistProfile::getUserId, existingUser.getId())
                        .last("LIMIT 1")
                );
                
                // 如果没有 artist_profile 记录，创建一个待审核的
                if (existingProfile == null) {
                    ArtistProfile newProfile = new ArtistProfile();
                    newProfile.setUserId(existingUser.getId());
                    newProfile.setUserUid(existingUser.getUid());
                    newProfile.setRealName(trimmedName);
                    newProfile.setArtistName(trimmedName);
                    newProfile.setStatus(0); // 待审核
                    newProfile.setArtistCode(generateArtistCode()); // 生成艺术家编号
                    newProfile.setCreatedAt(LocalDateTime.now());
                    newProfile.setUpdatedAt(LocalDateTime.now());
                    artistProfileMapper.insert(newProfile);
                    log.info("为已有用户创建 artist_profile: name={}, userId={}", trimmedName, existingUser.getId());
                }
                
                result.put("id", existingUser.getId());
                result.put("uid", existingUser.getUid());
                result.put("name", existingUser.getNickname());
                result.put("avatar", existingUser.getAvatar());
                result.put("exists", true);
                result.put("certified", false); // 用户存在但未认证
                result.put("pending", true); // 标记为待审核状态
                result.put("source", "user_account");
                log.info("从 user_account 表找到用户: name={}, userId={}", trimmedName, existingUser.getId());
                return result;
            }
        } catch (Exception e) {
            log.warn("从 user_account 表查询失败: {}", e.getMessage());
        }

        // 4. 不存在，创建新用户作为艺术家（未认证状态）
        log.info("艺术家不存在，创建新艺术家: name={}", trimmedName);
        User newUser = new User();
        newUser.setNickname(trimmedName);
        newUser.setUid(UserIdUtil.generateUid()); // 生成标准19位UID
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        newUser.setStatus(1);
        newUser.setDeleted(0);

        // 生成随机头像
        newUser.setAvatar("https://picsum.photos/200/200?random=" + System.currentTimeMillis());

        userMapper.insert(newUser);
        log.info("创建新用户: id={}, uid={}, nickname={}", newUser.getId(), newUser.getUid(), trimmedName);

        // 创建 artist_profile 记录（待审核状态）- 艺术家管理列表查询此表
        String artistCode = generateArtistCode(); // 生成艺术家编号
        ArtistProfile newProfile = new ArtistProfile();
        newProfile.setUserId(newUser.getId());
        newProfile.setUserUid(newUser.getUid());
        newProfile.setRealName(trimmedName);
        newProfile.setArtistName(trimmedName);
        newProfile.setStatus(0); // 待审核
        newProfile.setArtistCode(artistCode);
        newProfile.setCreatedAt(LocalDateTime.now());
        newProfile.setUpdatedAt(LocalDateTime.now());
        artistProfileMapper.insert(newProfile);
        log.info("创建 artist_profile 记录: id={}, artistCode={}, userId={}", newProfile.getId(), artistCode, newUser.getId());

        // 同时创建认证记录（待审核状态）- 保留原有逻辑以兼容
        ArtistCertification cert = new ArtistCertification();
        cert.setUserId(newUser.getId());
        cert.setRealName(trimmedName);
        cert.setIdCard(""); // 默认空身份证号，后续补全
        cert.setStatus(0); // 待审核
        cert.setArtistCode(artistCode); // 保持艺术家编号一致
        cert.setCreateTime(LocalDateTime.now());
        cert.setUpdateTime(LocalDateTime.now());
        artistCertMapper.insert(cert);
        log.info("创建 artist_certifications 记录: id={}, artistCode={}", cert.getId(), artistCode);

        result.put("id", newUser.getId());
        result.put("uid", newUser.getUid());
        result.put("name", newUser.getNickname());
        result.put("avatar", newUser.getAvatar());
        result.put("artistCode", artistCode);
        result.put("exists", false);
        result.put("certified", false);
        result.put("pending", true); // 标记为待审核状态
        result.put("message", "艺术家不存在，已创建待审核艺术家，艺术家编号：" + artistCode);
        result.put("source", "created");

        return result;
    }
    
    /**
     * 生成19位艺术家编号
     * 格式: ART + YYYYMMDD + 4位序列号 + 4位随机码
     */
    private String generateArtistCode() {
        String date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "ART" + date;
        
        // 查询当天最大的序列号
        Long maxSeq = 0L;
        try {
            // 从 artist_profile 表查询
            maxSeq = artistProfileMapper.selectList(
                new LambdaQueryWrapper<ArtistProfile>()
                    .likeRight(ArtistProfile::getArtistCode, prefix)
                    .last("LIMIT 100")
            ).stream()
                .filter(p -> p.getArtistCode() != null && p.getArtistCode().length() == 19)
                .map(p -> {
                    try {
                        return Long.parseLong(p.getArtistCode().substring(11, 15));
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .max(Long::compareTo)
                .orElse(0L);
        } catch (Exception e) {
            log.warn("查询 artist_profile 最大序列号失败: {}", e.getMessage());
        }
        
        try {
            // 从 artist_certifications 表查询
            Long certMaxSeq = artistCertMapper.selectList(
                new LambdaQueryWrapper<ArtistCertification>()
                    .likeRight(ArtistCertification::getArtistCode, prefix)
                    .last("LIMIT 100")
            ).stream()
                .filter(c -> c.getArtistCode() != null && c.getArtistCode().length() == 19)
                .map(c -> {
                    try {
                        return Long.parseLong(c.getArtistCode().substring(11, 15));
                    } catch (Exception e) {
                        return 0L;
                    }
                })
                .max(Long::compareTo)
                .orElse(0L);
            maxSeq = Math.max(maxSeq, certMaxSeq);
        } catch (Exception e) {
            log.warn("查询 artist_certifications 最大序列号失败: {}", e.getMessage());
        }
        
        String seq = String.format("%04d", maxSeq + 1);
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        return prefix + seq + random;
    }
    
    /**
     * 批量更新用户UID（用于初始化和迁移）
     * @param userIds 用户ID列表
     * @param uids 新的UID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateUids(List<Long> userIds, List<String> uids) {
        if (userIds == null || uids == null || userIds.size() != uids.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "用户ID和UID列表不匹配");
        }
        
        for (int i = 0; i < userIds.size(); i++) {
            User user = userMapper.selectById(userIds.get(i));
            if (user != null) {
                user.setUid(uids.get(i));
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);
            }
        }
        log.info("批量更新了 {} 个用户的UID", userIds.size());
    }
    
    /**
     * 更新单个用户UID
     * @param userId 用户ID
     * @param uid 新的UID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUid(Long userId, String uid) {
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setUid(uid);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
            log.info("更新用户 {} 的UID为 {}", userId, uid);
        }
    }

    /**
     * 搜索全局用户列表
     * 用于发布作品时选择作者
     * 搜索顺序：artist_profile -> user_account
     * 同时返回艺术家认证状态
     */
    public List<Map<String, Object>> searchUsers(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        String trimmedKeyword = keyword.trim();
        List<Map<String, Object>> results = new ArrayList<>();
        Set<Long> addedUserIds = new HashSet<>();

        // 1. 先从 artist_profile 表查询（艺术家优先）
        try {
            List<ArtistProfile> profiles = artistProfileMapper.selectList(
                new LambdaQueryWrapper<ArtistProfile>()
                    .and(w -> w.like(ArtistProfile::getRealName, trimmedKeyword)
                              .or()
                              .like(ArtistProfile::getArtistName, trimmedKeyword))
                    .orderByDesc(ArtistProfile::getUpdatedAt)
                    .last("LIMIT " + limit)
            );

            for (ArtistProfile profile : profiles) {
                if (addedUserIds.contains(profile.getUserId())) continue;
                
                User user = null;
                if (profile.getUserId() != null) {
                    user = userMapper.selectById(profile.getUserId());
                } else if (profile.getUserUid() != null && !profile.getUserUid().isEmpty()) {
                    user = userMapper.selectOne(
                        new LambdaQueryWrapper<User>()
                            .eq(User::getUid, profile.getUserUid())
                            .eq(User::getDeleted, 0)
                    );
                }
                
                if (user != null && user.getDeleted() != null && user.getDeleted() == 0) {
                    addedUserIds.add(user.getId());
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", user.getId());
                    item.put("uid", user.getUid());
                    item.put("nickname", user.getNickname());
                    item.put("avatar", user.getAvatar());
                    item.put("name", profile.getRealName() != null && !profile.getRealName().isEmpty() 
                        ? profile.getRealName() : user.getNickname());
                    item.put("artistCode", profile.getArtistCode());
                    item.put("isArtist", true);
                    item.put("certified", profile.getStatus() != null && profile.getStatus() == 1);
                    item.put("artistStatus", profile.getStatus());
                    item.put("source", "artist_profile");
                    results.add(item);
                }
            }
        } catch (Exception e) {
            log.warn("从 artist_profile 表搜索用户失败: {}", e.getMessage());
        }

        // 2. 从 user_account 表查询（普通用户）
        if (results.size() < limit) {
            try {
                int userLimit = limit - results.size();
                List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                        .eq(User::getDeleted, 0)
                        .and(w -> w.like(User::getNickname, trimmedKeyword)
                                  .or()
                                  .like(User::getPhone, trimmedKeyword))
                        .orderByDesc(User::getCreateTime)
                        .last("LIMIT " + userLimit)
                );

                for (User user : users) {
                    if (addedUserIds.contains(user.getId())) continue;
                    
                    // 检查是否是艺术家
                    ArtistProfile profile = artistProfileMapper.selectOne(
                        new LambdaQueryWrapper<ArtistProfile>()
                            .eq(ArtistProfile::getUserId, user.getId())
                            .last("LIMIT 1")
                    );
                    
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", user.getId());
                    item.put("uid", user.getUid());
                    item.put("nickname", user.getNickname());
                    item.put("avatar", user.getAvatar());
                    item.put("name", user.getNickname());
                    item.put("isArtist", profile != null);
                    item.put("certified", profile != null && profile.getStatus() != null && profile.getStatus() == 1);
                    item.put("artistStatus", profile != null ? profile.getStatus() : null);
                    item.put("artistCode", profile != null ? profile.getArtistCode() : null);
                    item.put("source", "user_account");
                    results.add(item);
                }
            } catch (Exception e) {
                log.warn("从 user_account 表搜索用户失败: {}", e.getMessage());
            }
        }

        return results.stream().limit(limit).toList();
    }

    // ======================== 数据分析 ========================

    /**
     * 获取艺术家核心指标概览
     */
    public Map<String, Object> getArtistAnalyticsOverview(Long artistId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 作品总量与浏览/收藏/销售统计
        String artworkTable = firstExistingTable("artwork", "artworks");
        if (artworkTable != null) {
            try {
                String viewCol = firstExistingColumn(artworkTable, "view_count");
                String favCol = firstExistingColumn(artworkTable, "favorite_count");
                String saleCol = firstExistingColumn(artworkTable, "sale_count", "display_sale_count");

                StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total_works");
                if (viewCol != null) sql.append(", COALESCE(SUM(").append(viewCol).append("),0) AS total_views");
                if (favCol != null) sql.append(", COALESCE(SUM(").append(favCol).append("),0) AS total_favorites");
                if (saleCol != null) sql.append(", COALESCE(SUM(").append(saleCol).append("),0) AS total_sales");
                sql.append(" FROM ").append(artworkTable).append(" WHERE author_id = ?");

                Map<String, Object> row = jdbcTemplate.queryForMap(sql.toString(), artistId);
                data.put("works", toLong(row.get("total_works"), 0));
                if (viewCol != null) data.put("views", toLong(row.get("total_views"), 0));
                if (favCol != null) data.put("favorites", toLong(row.get("total_favorites"), 0));
                if (saleCol != null) data.put("sales", toLong(row.get("total_sales"), 0));
            } catch (Exception e) {
                log.warn("查询作品统计失败 artistId={}: {}", artistId, e.getMessage());
            }
        }

        // 2. 粉丝数
        try {
            Map<String, Object> userRow = jdbcTemplate.queryForMap(
                "SELECT follower_count FROM users WHERE id = ?", artistId);
            data.put("followers", toLong(userRow.get("follower_count"), 0));
        } catch (Exception e) {
            data.put("followers", 0L);
        }

        // 3. 互动率 = (收藏 + 销售) / 浏览量
        long views = data.containsKey("views") ? toLong(data.get("views"), 0) : 0;
        long interactions = (data.containsKey("favorites") ? toLong(data.get("favorites"), 0) : 0)
                         + (data.containsKey("sales") ? toLong(data.get("sales"), 0) : 0);
        data.put("engagementRate", views > 0
            ? BigDecimal.valueOf(interactions).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(views), 2, BigDecimal.ROUND_HALF_UP)
            : BigDecimal.ZERO);

        // 4. 订单数据：累计交易额
        try {
            String orderTable = firstExistingTable("trade_order", "orders", "order_info");
            if (orderTable != null && artworkTable != null) {
                String itemTable = firstExistingTable("trade_order_item", "order_items", "order_item");
                if (itemTable != null) {
                    Map<String, Object> orderRow = jdbcTemplate.queryForMap(
                        "SELECT COUNT(*) AS total_orders, COALESCE(SUM(i.subtotal),0) AS total_revenue "
                        + "FROM " + itemTable + " i "
                        + "JOIN " + artworkTable + " a ON i.artwork_id = a.id "
                        + "WHERE a.author_id = ?",
                        artistId
                    );
                    data.put("orders", toLong(orderRow.get("total_orders"), 0));
                    data.put("revenue", toLong(orderRow.get("total_revenue"), 0));
                }
            }
        } catch (Exception e) {
            // 没有 order_item 表或字段不匹配时静默跳过
            data.putIfAbsent("orders", 0L);
            data.putIfAbsent("revenue", 0L);
        }

        return data;
    }

    /**
     * 获取艺术家趋势数据
     */
    public Map<String, Object> getArtistAnalyticsTrend(Long artistId, int days) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (days <= 0) days = 30;
        data.put("days", days);

        java.time.LocalDate now = java.time.LocalDate.now();
        java.time.LocalDate since = now.minusDays(days - 1);

        // 1. 销售趋势（按天聚合）
        List<Map<String, Object>> salesTrend = new ArrayList<>();
        try {
            String itemTable = firstExistingTable("trade_order_item", "order_items", "order_item");
            String orderTable = firstExistingTable("trade_order", "orders", "order_info");
            String artworkTable = firstExistingTable("artwork", "artworks");

            if (itemTable != null && orderTable != null && artworkTable != null) {
                // 订单支付时间字段探测
                String payCol = firstExistingColumn(orderTable, "paid_at", "pay_time", "create_time", "created_at");
                if (payCol != null) {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT DATE(o." + payCol + ") AS day, "
                        + "COUNT(DISTINCT o.id) AS order_count, "
                        + "COALESCE(SUM(i.subtotal),0) AS revenue "
                        + "FROM " + orderTable + " o "
                        + "JOIN " + itemTable + " i ON o.id = i.order_id "
                        + "JOIN " + artworkTable + " a ON i.artwork_id = a.id "
                        + "WHERE a.author_id = ? AND o." + payCol + " >= ? "
                        + "GROUP BY DATE(o." + payCol + ") ORDER BY day",
                        artistId, since.toString()
                    );
                    salesTrend = rows;
                }
            }
        } catch (Exception e) {
            log.warn("查询销售趋势失败: {}", e.getMessage());
        }

        // 2. 粉丝增长趋势
        List<Map<String, Object>> followerTrend = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT DATE(create_time) AS day, COUNT(*) AS count "
                + "FROM user_follows WHERE follow_user_id = ? AND create_time >= ? "
                + "GROUP BY DATE(create_time) ORDER BY day",
                artistId, since.toString()
            );
            followerTrend = rows;
        } catch (Exception e) {
            log.warn("查询粉丝趋势失败: {}", e.getMessage());
        }

        // 3. 填充完整日期序列（补零）
        List<String> dateLabels = new ArrayList<>();
        Map<String, Long> salesMap = new HashMap<>();
        Map<String, Long> revenueMap = new HashMap<>();
        Map<String, Long> followerMap = new HashMap<>();

        for (Map<String, Object> r : salesTrend) {
            String d = stringValue(r.get("day"));
            salesMap.put(d, toLong(r.get("order_count"), 0));
            revenueMap.put(d, toLong(r.get("revenue"), 0));
        }
        for (Map<String, Object> r : followerTrend) {
            followerMap.put(stringValue(r.get("day")), toLong(r.get("count"), 0));
        }

        List<Long> salesList = new ArrayList<>();
        List<Long> revenueList = new ArrayList<>();
        List<Long> followersList = new ArrayList<>();

        long cumulativeFollowers = 0;
        // 初始粉丝数（基于用户表的 follower_count 减去趋势内的新增）
        try {
            Map<String, Object> userRow = jdbcTemplate.queryForMap(
                "SELECT follower_count FROM users WHERE id = ?", artistId);
            long currentFollowers = toLong(userRow.get("follower_count"), 0);
            long newInPeriod = followerMap.values().stream().mapToLong(Long::longValue).sum();
            cumulativeFollowers = Math.max(0, currentFollowers - newInPeriod);
        } catch (Exception e) {
            log.warn("查询初始粉丝数失败", e);
        }

        for (int i = 0; i < days; i++) {
            String d = since.plusDays(i).toString();
            dateLabels.add(d);
            salesList.add(salesMap.getOrDefault(d, 0L));
            revenueList.add(revenueMap.getOrDefault(d, 0L));
            cumulativeFollowers += followerMap.getOrDefault(d, 0L);
            followersList.add(cumulativeFollowers);
        }

        data.put("dates", dateLabels);
        data.put("sales", salesList);
        data.put("revenue", revenueList);
        data.put("followers", followersList);

        return data;
    }

    /**
     * 获取受众画像
     */
    public Map<String, Object> getArtistAudienceProfile(Long artistId) {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. 地域分布：关注该艺术家的用户的 region 字段
        List<Map<String, Object>> regionDistribution = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COALESCE(NULLIF(u.region,''),'未知') AS region, COUNT(*) AS count "
                + "FROM user_follows f JOIN users u ON f.user_id = u.id "
                + "WHERE f.follow_user_id = ? AND u.region IS NOT NULL "
                + "GROUP BY region ORDER BY count DESC LIMIT 10",
                artistId
            );
            long total = rows.stream().mapToLong(r -> toLong(r.get("count"), 0)).sum();
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", r.get("region"));
                long cnt = toLong(r.get("count"), 0);
                item.put("count", cnt);
                item.put("ratio", total > 0
                    ? BigDecimal.valueOf(cnt).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 1, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO);
                regionDistribution.add(item);
            }
        } catch (Exception e) {
            log.warn("查询地域分布失败: {}", e.getMessage());
        }
        data.put("regionDistribution", regionDistribution);

        // 2. 性别分布：关注该艺术家的用户的 gender 字段
        List<Map<String, Object>> genderDistribution = new ArrayList<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT u.gender, COUNT(*) AS count "
                + "FROM user_follows f JOIN users u ON f.user_id = u.id "
                + "WHERE f.follow_user_id = ? GROUP BY u.gender",
                artistId
            );
            long totalGender = 0;
            for (Map<String, Object> r : rows) {
                int g = toInt(r.get("gender"), 0);
                long cnt = toLong(r.get("count"), 0);
                totalGender += cnt;
                String label = switch (g) { case 1 -> "男"; case 2 -> "女"; default -> "未知"; };
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", label);
                item.put("count", cnt);
                genderDistribution.add(item);
            }
            // 计算比率
            for (Map<String, Object> item : genderDistribution) {
                long cnt = toLong(item.get("count"), 0);
                item.put("ratio", totalGender > 0
                    ? BigDecimal.valueOf(cnt).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalGender), 1, BigDecimal.ROUND_HALF_UP)
                    : BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.warn("查询性别分布失败: {}", e.getMessage());
        }
        data.put("genderDistribution", genderDistribution);

        // 3. 偏好分析：按作品分类统计销量
        List<Map<String, Object>> preferenceDistribution = new ArrayList<>();
        try {
            String artworkTable = firstExistingTable("artwork", "artworks");
            if (artworkTable != null) {
                String typeCol = firstExistingColumn(artworkTable, "art_type", "category_name", "medium");
                // 使用view_count作为"热度"代理指标（sale_count可能不存在）
                String proxyCol = firstExistingColumn(artworkTable, "view_count", "favorite_count");
                if (typeCol != null && proxyCol != null) {
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT COALESCE(NULLIF(" + typeCol + ",''),'其他') AS label, "
                        + "SUM(COALESCE(" + proxyCol + ",0)) AS count "
                        + "FROM " + artworkTable + " WHERE author_id = ? "
                        + "GROUP BY label ORDER BY count DESC",
                        artistId
                    );
                    long totalPref = rows.stream().mapToLong(r -> toLong(r.get("count"), 0)).sum();
                    for (Map<String, Object> r : rows) {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("name", r.get("label"));
                        long cnt = toLong(r.get("count"), 0);
                        item.put("count", cnt);
                        item.put("ratio", totalPref > 0
                            ? BigDecimal.valueOf(cnt).multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(totalPref), 1, BigDecimal.ROUND_HALF_UP)
                            : BigDecimal.ZERO);
                        preferenceDistribution.add(item);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询偏好分布失败: {}", e.getMessage());
        }
        data.put("preferenceDistribution", preferenceDistribution);

        return data;
    }

    private long toLong(Object value, long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        try { return Long.parseLong(String.valueOf(value)); } catch (NumberFormatException e) { return defaultValue; }
    }

    // ===================== 实名认证 =====================

    /**
     * 提交实名认证申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void submitRealnameCert(Long userId, com.shiyiju.user.dto.RealnameCertSubmitDTO dto) {
        // 检查是否已有认证记录
        RealnameCertification existing = realnameCertMapper.selectOne(
            new LambdaQueryWrapper<RealnameCertification>().eq(RealnameCertification::getUserId, userId));
        if (existing != null) {
            if (existing.getStatus() == 1) {
                throw new BusinessException(400, "您已通过实名认证");
            }
            if (existing.getStatus() == 0) {
                throw new BusinessException(400, "您已有待审核的认证申请");
            }
            // 已拒绝则允许重新提交 - 更新记录
        }

        String idCard = dto.getIdCard().trim().toUpperCase();
        String idCardHash = sha256(idCard);
        String maskedIdCard = maskIdCard(idCard);

        // 查重：同一身份证不能被不同用户认证
        RealnameCertification dup = realnameCertMapper.selectOne(
            new LambdaQueryWrapper<RealnameCertification>()
                .eq(RealnameCertification::getIdCardHash, idCardHash)
                .eq(RealnameCertification::getStatus, 1));
        if (dup != null) {
            throw new BusinessException(400, "该身份证号已被其他账号认证");
        }

        if (existing != null) {
            // 重新提交：更新已有记录
            existing.setRealName(dto.getRealName().trim());
            existing.setIdCard(maskedIdCard);
            existing.setIdCardHash(idCardHash);
            existing.setIdFrontUrl(dto.getIdFrontUrl());
            existing.setIdBackUrl(dto.getIdBackUrl());
            existing.setFaceVerified(Boolean.TRUE.equals(dto.getFaceVerified()) ? 1 : 0);
            existing.setStatus(0);
            existing.setRejectReason(null);
            existing.setReviewTime(null);
            existing.setReviewerId(null);
            realnameCertMapper.updateById(existing);
        } else {
            RealnameCertification cert = new RealnameCertification();
            cert.setUserId(userId);
            cert.setRealName(dto.getRealName().trim());
            cert.setIdCard(maskedIdCard);
            cert.setIdCardHash(idCardHash);
            cert.setIdFrontUrl(dto.getIdFrontUrl());
            cert.setIdBackUrl(dto.getIdBackUrl());
            cert.setFaceVerified(Boolean.TRUE.equals(dto.getFaceVerified()) ? 1 : 0);
            cert.setStatus(0);
            realnameCertMapper.insert(cert);
        }

        log.info("用户 {} 提交实名认证申请", userId);
    }

    /**
     * 获取实名认证状态
     */
    public com.shiyiju.user.vo.RealnameCertStatusVO getRealnameCertStatus(Long userId) {
        RealnameCertification cert = realnameCertMapper.selectOne(
            new LambdaQueryWrapper<RealnameCertification>().eq(RealnameCertification::getUserId, userId));

        if (cert == null) {
            // 检查 users 表是否有 real_name_verified 历史标记
            User user = userMapper.selectById(userId);
            if (user != null) {
                // 暂不使用 JDBC 读取动态列名，后续兼容
            }
            return com.shiyiju.user.vo.RealnameCertStatusVO.builder()
                .status(0)
                .build();
        }

        int displayStatus;
        switch (cert.getStatus()) {
            case 1: displayStatus = 1; break; // 已通过
            case 2: displayStatus = 3; break; // 已拒绝（前端展示码 3）
            default: displayStatus = 2; break; // 审核中（前端展示码 2）
        }

        String maskedRealName = maskRealName(cert.getRealName());
        String maskedIdCard = cert.getIdCard();

        return com.shiyiju.user.vo.RealnameCertStatusVO.builder()
            .status(displayStatus)
            .maskedRealName(maskedRealName)
            .maskedIdCard(maskedIdCard)
            .rejectReason(cert.getRejectReason())
            .submittedAt(cert.getCreateTime())
            .reviewTime(cert.getReviewTime())
            .build();
    }

    /**
     * 管理后台 - 分页查询实名认证列表
     */
    public com.shiyiju.common.result.PageResult<Map<String, Object>> listRealnameCert(
            int page, int size, Integer status, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT r.id, r.user_id, r.real_name, r.id_card, ");
        sql.append("r.id_front_url, r.id_back_url, r.face_verified, r.status, ");
        sql.append("r.reject_reason, r.review_time, r.create_time, ");
        sql.append("COALESCE(u.nickname, '') AS nickname, COALESCE(u.avatar, '') AS avatar, ");
        sql.append("COALESCE(u.phone, '') AS phone, COALESCE(u.uid, '') AS uid ");
        sql.append("FROM realname_certifications r LEFT JOIN users u ON r.user_id = u.id WHERE 1=1");

        List<Object> params = new ArrayList<>();

        if (status != null) {
            sql.append(" AND r.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (u.nickname LIKE ? OR r.real_name LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        // 总数
        String countSql = sql.toString().replaceFirst("SELECT r\\.id,.*?FROM", "SELECT COUNT(*) FROM");
        Integer total = jdbcTemplate.queryForObject(countSql, Integer.class, params.toArray());
        if (total == null) total = 0;

        // 分页
        int offset = (page - 1) * size;
        sql.append(" ORDER BY r.create_time DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        return com.shiyiju.common.result.PageResult.of((long) total, page, size, rows);
    }

    /**
     * 管理后台 - 审核通过
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveRealnameCert(Long certId, Long reviewerId) {
        RealnameCertification cert = realnameCertMapper.selectById(certId);
        if (cert == null) {
            throw new BusinessException(400, "认证记录不存在");
        }
        if (cert.getStatus() != 0) {
            throw new BusinessException(400, "该记录已审核，不能重复操作");
        }

        cert.setStatus(1);
        cert.setReviewTime(LocalDateTime.now());
        cert.setReviewerId(reviewerId);
        realnameCertMapper.updateById(cert);

        // 更新 users 表
        jdbcTemplate.update("UPDATE users SET real_name_verified = 1 WHERE id = ?", cert.getUserId());

        log.info("实名认证审核通过: certId={}, userId={}, reviewerId={}", certId, cert.getUserId(), reviewerId);
    }

    /**
     * 管理后台 - 审核拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public void rejectRealnameCert(Long certId, Long reviewerId, String reason) {
        RealnameCertification cert = realnameCertMapper.selectById(certId);
        if (cert == null) {
            throw new BusinessException(400, "认证记录不存在");
        }
        if (cert.getStatus() != 0) {
            throw new BusinessException(400, "该记录已审核，不能重复操作");
        }

        cert.setStatus(2);
        cert.setRejectReason(reason);
        cert.setReviewTime(LocalDateTime.now());
        cert.setReviewerId(reviewerId);
        realnameCertMapper.updateById(cert);

        log.info("实名认证审核拒绝: certId={}, userId={}, reviewerId={}, reason={}",
            certId, cert.getUserId(), reviewerId, reason);
    }

    // ===================== 工具方法 =====================

    /** 脱敏姓名：张** */
    private String maskRealName(String name) {
        if (name == null || name.length() <= 1) return name;
        return name.charAt(0) + "**";
    }

    /** 脱敏身份证号：410***********1234 */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /** SHA256 哈希 */
    private String sha256(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR.getCode(), "哈希计算失败");
        }
    }
}
