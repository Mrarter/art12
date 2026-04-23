package com.shiyiju.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shiyiju.common.constant.UserConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.common.util.JwtUtil;
import com.shiyiju.user.dto.WxLoginDTO;
import com.shiyiju.user.dto.ArtistCertDTO;
import com.shiyiju.user.entity.ArtistCertification;
import com.shiyiju.user.entity.PromoterRecord;
import com.shiyiju.user.entity.User;
import com.shiyiju.common.entity.Address;
import com.shiyiju.user.mapper.ArtistCertificationMapper;
import com.shiyiju.user.mapper.PromoterRecordMapper;
import com.shiyiju.user.mapper.UserMapper;
import com.shiyiju.user.vo.LoginVO;
import com.shiyiju.user.vo.UserInfoVO;
import com.shiyiju.user.vo.ArtistCertStatusVO;
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
     * 根据名称模糊搜索已认证的艺术家
     */
    public List<Map<String, Object>> searchArtists(String keyword, int limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(User::getNickname, keyword.trim())
               .eq(User::getDeleted, 0)
               .orderByDesc(User::getCreateTime)
               .last("LIMIT " + limit);

        List<User> users = userMapper.selectList(wrapper);

        return users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("name", user.getNickname());
            map.put("avatar", user.getAvatar());
            map.put("bio", user.getBio());
            // 检查认证状态
            LambdaQueryWrapper<ArtistCertification> certWrapper = new LambdaQueryWrapper<>();
            certWrapper.eq(ArtistCertification::getUserId, user.getId())
                       .eq(ArtistCertification::getStatus, 1); // 已认证
            ArtistCertification cert = artistCertMapper.selectOne(certWrapper);
            map.put("certified", cert != null);
            map.put("badge", cert != null ? cert.getRealName() : null); // 使用真实姓名作为徽章
            return map;
        }).toList();
    }

    /**
     * 查找或创建艺术家
     * 如果艺术家存在则返回，不存在则创建未审核状态的艺术家
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> findOrCreateArtist(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "艺术家名称不能为空");
        }

        // 先搜索是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getNickname, name.trim())
               .eq(User::getDeleted, 0);
        User existingUser = userMapper.selectOne(wrapper);

        if (existingUser != null) {
            // 已存在
            Map<String, Object> result = new HashMap<>();
            result.put("id", existingUser.getId());
            result.put("name", existingUser.getNickname());
            result.put("avatar", existingUser.getAvatar());
            result.put("exists", true);
            result.put("certified", false); // 需检查认证状态
            return result;
        }

        // 不存在，创建新用户作为艺术家（未认证状态）
        User newUser = new User();
        newUser.setNickname(name.trim());
        newUser.setCreateTime(LocalDateTime.now());
        newUser.setUpdateTime(LocalDateTime.now());
        newUser.setStatus(1);
        newUser.setDeleted(0);

        // 生成随机头像
        newUser.setAvatar("https://picsum.photos/200/200?random=" + System.currentTimeMillis());

        userMapper.insert(newUser);

        // 创建认证记录（待审核状态）
        ArtistCertification cert = new ArtistCertification();
        cert.setUserId(newUser.getId());
        cert.setRealName(name.trim());
        cert.setStatus(0); // 待审核
        cert.setCreateTime(LocalDateTime.now());
        cert.setUpdateTime(LocalDateTime.now());
        artistCertMapper.insert(cert);

        Map<String, Object> result = new HashMap<>();
        result.put("id", newUser.getId());
        result.put("name", newUser.getNickname());
        result.put("avatar", newUser.getAvatar());
        result.put("exists", false);
        result.put("certified", false);
        result.put("pending", true); // 标记为待审核状态
        result.put("message", "艺术家不存在，已创建待审核艺术家");

        return result;
    }
}
