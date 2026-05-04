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
    private final RedisTemplate<String, Object> redisTemplate;

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
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setIdentities(user.getIdentities());

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
     * 模拟获取微信 OpenID（实际应调用微信接口）
     */
    private String getOpenidFromWx(String code) {
        // TODO: 实际实现中调用微信接口
        // https://api.weixin.qq.com/sns/jscode2session?appid=APPID&secret=SECRET&js_code=CODE&grant_type=authorization_code
        return "mock_openid_" + code;
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
        User artist = userMapper.selectById(artistId);
        if (artist == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", artist.getId());
        data.put("nickname", artist.getNickname());
        data.put("avatar", artist.getAvatar());
        data.put("bio", artist.getBio());
        data.put("region", artist.getRegion());
        
        // 身份
        List<String> identityList = Arrays.asList(artist.getIdentities().split(","));
        data.put("identities", identityList);
        data.put("isArtist", identityList.contains(UserConstant.IDENTITY_ARTIST));
        
        data.put("followerCount", artist.getFollowerCount());
        data.put("followingCount", artist.getFollowingCount());
        
        // 获取艺术家认证信息
        ArtistCertification cert = artistCertMapper.selectOne(
                new LambdaQueryWrapper<ArtistCertification>()
                        .eq(ArtistCertification::getUserId, artistId)
                        .eq(ArtistCertification::getStatus, UserConstant.ARTIST_CERT_APPROVED)
        );
        if (cert != null) {
            data.put("resume", cert.getResume());
            data.put("artworks", cert.getArtworks() != null ? 
                    Arrays.asList(cert.getArtworks().split(",")) : List.of());
            data.put("artistStatus", "已认证");
        } else {
            data.put("artistStatus", "未认证");
        }
        
        return data;
    }

    /**
     * 获取艺术家详细信息（用于作品关联）
     * 返回完整的艺术家信息供作品服务使用
     */
    public Map<String, Object> getArtistInfo(Long artistId) {
        User artist = userMapper.selectById(artistId);
        if (artist == null) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", artist.getId());
        data.put("uid", artist.getUid());
        data.put("nickname", artist.getNickname());
        data.put("realName", null); // 真实姓名需要关联认证表
        data.put("avatar", artist.getAvatar());
        data.put("phone", artist.getPhone());
        data.put("bio", artist.getBio());
        data.put("region", artist.getRegion());

        // 身份信息
        List<String> identityList = artist.getIdentities() != null ?
                Arrays.asList(artist.getIdentities().split(",")) : List.of();
        data.put("identities", identityList);
        data.put("isArtist", identityList.contains(UserConstant.IDENTITY_ARTIST));

        // 推断艺术家身份类型
        String identityType = "artist";
        if (identityList.contains("collector")) {
            identityType = "collector";
        } else if (identityList.contains("gallery")) {
            identityType = "gallery";
        }
        data.put("identityType", identityType);

        data.put("followerCount", artist.getFollowerCount() != null ? artist.getFollowerCount() : 0);
        data.put("artworkCount", 0); // TODO: 从作品服务获取

        // 获取艺术家认证信息
        ArtistCertification cert = artistCertMapper.selectOne(
                new LambdaQueryWrapper<ArtistCertification>()
                        .eq(ArtistCertification::getUserId, artistId)
                        .eq(ArtistCertification::getStatus, UserConstant.ARTIST_CERT_APPROVED)
        );

        if (cert != null) {
            data.put("certStatus", cert.getStatus());
            data.put("realName", cert.getRealName());
            data.put("resume", cert.getResume());
            data.put("artworks", cert.getArtworks() != null ?
                    Arrays.asList(cert.getArtworks().split(",")).stream().filter(s -> !s.isEmpty()).toList() : List.of());
            data.put("exhibits", cert.getExhibits() != null ?
                    Arrays.asList(cert.getExhibits().split(",")).stream().filter(s -> !s.isEmpty()).toList() : List.of());
            data.put("badge", determineBadge(cert.getRealName(), identityList));
            // 添加艺术家认证编号
            data.put("artistCode", cert.getArtistCode());
        } else {
            data.put("certStatus", null);
            data.put("badge", determineBadge(artist.getNickname(), identityList));
        }

        return data;
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
}
