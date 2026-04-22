package com.shiyiju.admin.controller;

import com.shiyiju.common.constant.UserConstant;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 管理员 - 用户与艺术家管理控制器
 */
@RestController
@RequestMapping("/admin/user")
public class UserManagementController {

    private static final Logger log = LoggerFactory.getLogger(UserManagementController.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 模拟用户数据存储（实际应该连接数据库）
    private static final List<Map<String, Object>> mockUsers = new ArrayList<>();
    private static long nextUserId = 10000;
    
    static {
        // 初始化默认用户（每个身份5个）
        initTestUsers();
    }
    
    /**
     * 初始化测试用户
     */
    private static void initTestUsers() {
        String[][] identities = {
            {"artist", "经纪人"},
            {"agent", "艺术家"},
            {"collector", "收藏家"},
            {"promoter", "艺荐官"},
            {"artist,agent", "艺术家+经纪人"},
        };
        
        for (int i = 0; i < identities.length; i++) {
            for (int j = 1; j <= 5; j++) {
                Map<String, Object> user = new HashMap<>();
                long userId = 10000 + i * 5 + j;
                user.put("userId", userId);
                user.put("nickname", identities[i][1] + j);
                user.put("phone", "138" + String.format("%08d", 1000 + i * 5 + j));
                user.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId);
                user.put("identities", identities[i][0]);
                user.put("isArtist", identities[i][0].contains("artist"));
                user.put("isAgent", identities[i][0].contains("agent"));
                user.put("isCollector", identities[i][0].contains("collector"));
                user.put("isPromoter", identities[i][0].contains("promoter"));
                user.put("balance", 10000 + i * 1000 + j * 100);
                user.put("orderCount", 5 + j);
                user.put("createTime", LocalDateTime.now().minusDays(30 - i * 5 - j).format(FORMATTER));
                user.put("status", 1);
                mockUsers.add(user);
            }
        }
        nextUserId = 10025;
    }

    /**
     * 用户列表
     */
    @GetMapping("/list")
    public Result<PageResult<Map<String, Object>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String identity,
            @RequestParam(required = false) String keyword) {
        
        List<Map<String, Object>> filtered = new ArrayList<>(mockUsers);
        
        // 按身份筛选
        if (identity != null && !identity.isEmpty() && !"all".equals(identity)) {
            final String filterIdentity = identity;
            filtered.removeIf(u -> !u.get("identities").toString().contains(filterIdentity));
        }
        
        // 按关键词搜索
        if (keyword != null && !keyword.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            filtered.removeIf(u -> 
                !u.get("nickname").toString().toLowerCase().contains(lowerKeyword) &&
                !u.get("phone").toString().contains(keyword)
            );
        }
        
        // 分页
        int total = filtered.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageList = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();
        
        PageResult<Map<String, Object>> result = new PageResult<>();
        result.setRecords(pageList);
        result.setTotal((long) total);
        result.setPage(page);
        result.setPageSize(size);
        return Result.success(result);
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> params) {
        String nickname = (String) params.get("nickname");
        String phone = (String) params.get("phone");
        String identity = params.containsKey("identity") ? (String) params.get("identity") : "collector";
        
        Map<String, Object> user = new HashMap<>();
        long userId = nextUserId++;
        user.put("userId", userId);
        user.put("nickname", nickname);
        user.put("phone", phone);
        user.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId);
        user.put("identities", identity);
        user.put("isArtist", identity.contains("artist"));
        user.put("isAgent", identity.contains("agent"));
        user.put("isCollector", identity.contains("collector"));
        user.put("isPromoter", identity.contains("promoter"));
        user.put("balance", 0L);
        user.put("orderCount", 0);
        user.put("createTime", LocalDateTime.now().format(FORMATTER));
        user.put("status", 1);
        
        mockUsers.add(user);
        log.info("创建用户: userId={}, nickname={}, identity={}", userId, nickname, identity);
        
        return Result.success(user);
    }
    
    /**
     * 批量创建用户（按身份）
     */
    @PostMapping("/batch-create")
    public Result<Map<String, Object>> batchCreateUsers(@RequestBody Map<String, Object> params) {
        String identity = (String) params.get("identity");
        int count = params.containsKey("count") ? ((Number) params.get("count")).intValue() : 5;
        
        String[][] identityConfig = {
            {"artist", "艺术家"},
            {"agent", "经纪人"},
            {"collector", "收藏家"},
            {"promoter", "艺荐官"},
        };
        
        String identityValue = identity;
        String identityName = identity;
        for (String[] config : identityConfig) {
            if (config[0].equals(identity)) {
                identityValue = config[0];
                identityName = config[1];
                break;
            }
        }
        
        List<Map<String, Object>> createdUsers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Map<String, Object> user = new HashMap<>();
            long userId = nextUserId++;
            user.put("userId", userId);
            user.put("nickname", identityName + System.currentTimeMillis() % 10000 + "_" + i);
            user.put("phone", "138" + String.format("%08d", 20000 + nextUserId));
            user.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=" + userId);
            user.put("identities", identityValue);
            user.put("isArtist", identityValue.contains("artist"));
            user.put("isAgent", identityValue.contains("agent"));
            user.put("isCollector", identityValue.contains("collector"));
            user.put("isPromoter", identityValue.contains("promoter"));
            user.put("balance", 0L);
            user.put("orderCount", 0);
            user.put("createTime", LocalDateTime.now().format(FORMATTER));
            user.put("status", 1);
            mockUsers.add(user);
            createdUsers.add(user);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("count", count);
        result.put("identity", identityValue);
        result.put("users", createdUsers);
        log.info("批量创建用户: identity={}, count={}", identity, count);
        
        return Result.success(result);
    }
    
    /**
     * 获取用户身份统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", mockUsers.size());
        stats.put("artistCount", mockUsers.stream().filter(u -> u.get("identities").toString().contains("artist")).count());
        stats.put("agentCount", mockUsers.stream().filter(u -> u.get("identities").toString().contains("agent")).count());
        stats.put("collectorCount", mockUsers.stream().filter(u -> u.get("identities").toString().contains("collector")).count());
        stats.put("promoterCount", mockUsers.stream().filter(u -> u.get("identities").toString().contains("promoter")).count());
        return Result.success(stats);
    }

    /**
     * 艺术家认证列表
     * 支持按状态筛选：pending-待审核, approved-已认证, rejected-已拒绝, all-全部
     */
    @GetMapping("/artist/list")
    public Result<Map<String, Object>> getArtistList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        
        // 模拟艺术家数据
        List<Map<String, Object>> allArtists = new ArrayList<>();
        
        // 已认证艺术家
        Map<String, Object> artist1 = createArtist(1L, "张大千", "13800138001", "张伟", "110101199001011234", 
                1, "master", "著名国画大师", LocalDateTime.now().minusDays(10));
        artist1.put("certified", true);
        allArtists.add(artist1);
        
        Map<String, Object> artist2 = createArtist(2L, "李娜", "13800138002", "李娜", "110101199002022345", 
                1, "popular", "专注油画创作20年", LocalDateTime.now().minusDays(5));
        artist2.put("certified", true);
        allArtists.add(artist2);
        
        Map<String, Object> artist3 = createArtist(3L, "钱多多", "13800138003", "钱伟", "110101199003033456", 
                1, "verified", "雕塑艺术家", LocalDateTime.now().minusDays(3));
        artist3.put("certified", true);
        allArtists.add(artist3);
        
        // 待审核
        Map<String, Object> artist4 = createArtist(4L, "王强", "13800138004", "王强", "110101199004044567", 
                0, "", "书法爱好者", LocalDateTime.now().minusDays(1));
        artist4.put("certified", false);
        allArtists.add(artist4);
        
        Map<String, Object> artist5 = createArtist(5L, "赵敏", "13800138005", "赵敏", "110101199005055678", 
                0, "", "水彩画家", LocalDateTime.now().minusHours(5));
        artist5.put("certified", false);
        allArtists.add(artist5);
        
        // 已拒绝
        Map<String, Object> artist6 = createArtist(6L, "孙丽", "13800138006", "孙丽", "110101199006689012", 
                2, "", "业余绘画爱好者", LocalDateTime.now().minusDays(7));
        artist6.put("certified", false);
        artist6.put("rejectReason", "材料不全，无法核实身份");
        allArtists.add(artist6);
        
        // 统计各状态数量
        int pendingCount = 0, approvedCount = 0, rejectedCount = 0;
        for (Map<String, Object> a : allArtists) {
            int s = (Integer) a.get("status");
            if (s == 0) pendingCount++;
            else if (s == 1) approvedCount++;
            else if (s == 2) rejectedCount++;
        }
        
        // 按状态筛选
        List<Map<String, Object>> filtered = allArtists;
        if (status != null && !"all".equals(status)) {
            final String filterStatus = status;
            filtered = new ArrayList<>();
            for (Map<String, Object> a : allArtists) {
                int s = (Integer) a.get("status");
                String sStr = s == 0 ? "pending" : s == 1 ? "approved" : "rejected";
                if (sStr.equals(filterStatus)) {
                    filtered.add(a);
                }
            }
        }
        
        // 分页
        int total = filtered.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageList = fromIndex < total ? filtered.subList(fromIndex, toIndex) : new ArrayList<>();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", pageList);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        result.put("pendingCount", pendingCount);
        result.put("approvedCount", approvedCount);
        result.put("rejectedCount", rejectedCount);
        
        return Result.success(result);
    }

    private Map<String, Object> createArtist(Long id, String nickname, String phone, String realName, 
            String idCard, int status, String badge, String resume, LocalDateTime createTime) {
        Map<String, Object> artist = new HashMap<>();
        artist.put("id", id);
        artist.put("nickname", nickname);
        artist.put("phone", phone);
        artist.put("avatar", "");
        artist.put("realName", realName);
        artist.put("idCard", idCard);
        artist.put("status", status);
        artist.put("badge", badge);
        artist.put("resume", resume);
        artist.put("bio", resume);
        artist.put("images", "");
        artist.put("artworks", "");
        artist.put("exhibits", "");
        artist.put("createTime", createTime.format(FORMATTER));
        artist.put("createdAt", createTime.format(FORMATTER));
        return artist;
    }

    /**
     * 通过艺术家认证
     */
    @PostMapping("/artist/approve")
    public Result<Void> approveArtist(@RequestBody Map<String, Object> params) {
        Long id = params.containsKey("id") ? ((Number) params.get("id")).longValue() : null;
        String badge = params.containsKey("badge") ? (String) params.get("badge") : "";
        log.info("通过艺术家认证: id={}, badge={}", id, badge);
        return Result.success();
    }

    /**
     * 拒绝艺术家认证
     */
    @PostMapping("/artist/reject")
    public Result<Void> rejectArtist(@RequestBody Map<String, Object> params) {
        Long id = params.containsKey("id") ? ((Number) params.get("id")).longValue() : null;
        String reason = params.containsKey("reason") ? (String) params.get("reason") : "";
        log.info("拒绝艺术家认证: id={}, reason={}", id, reason);
        return Result.success();
    }

    /**
     * 取消艺术家认证
     */
    @PostMapping("/artist/revoke")
    public Result<Void> revokeArtist(@RequestBody Map<String, Object> params) {
        Long id = params.containsKey("id") ? ((Number) params.get("id")).longValue() : null;
        log.info("取消艺术家认证: id={}", id);
        return Result.success();
    }

    /**
     * 重新发起认证
     */
    @PostMapping("/artist/reapply")
    public Result<Void> reapplyArtist(@RequestBody Map<String, Object> params) {
        Long id = params.containsKey("id") ? ((Number) params.get("id")).longValue() : null;
        log.info("重新发起认证: id={}", id);
        return Result.success();
    }

    /**
     * 手动添加艺术家（后台直接认证）
     */
    @PostMapping("/artist/add")
    public Result<Void> addArtist(@RequestBody Map<String, Object> params) {
        String phone = (String) params.get("phone");
        String realName = (String) params.get("realName");
        String badge = params.containsKey("badge") ? (String) params.get("badge") : "";
        
        log.info("添加艺术家: phone={}, realName={}, badge={}", phone, realName, badge);
        return Result.success();
    }

    /**
     * 设置艺术家等级
     */
    @PostMapping("/artist/badge")
    public Result<Void> setArtistBadge(@RequestBody Map<String, Object> params) {
        Long id = params.containsKey("id") ? ((Number) params.get("id")).longValue() : null;
        String badge = params.containsKey("badge") ? (String) params.get("badge") : "";
        log.info("设置艺术家等级: id={}, badge={}", id, badge);
        return Result.success();
    }

    /**
     * 获取艺术家详情
     */
    @GetMapping("/artist/{id}")
    public Result<Map<String, Object>> getArtistDetail(@PathVariable Long id) {
        Map<String, Object> artist = createArtist(id, "艺术家", "13800138000", "***", "***************", 
                1, "", "", LocalDateTime.now());
        return Result.success(artist);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    public Result<Map<String, Object>> getUserDetail(@PathVariable Long userId) {
        for (Map<String, Object> user : mockUsers) {
            if (user.get("userId").equals(userId)) {
                return Result.success(user);
            }
        }
        return Result.fail(404, "用户不存在");
    }
    
    /**
     * 更新用户身份
     */
    @PostMapping("/{userId}/identity")
    public Result<Void> updateUserIdentity(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> params) {
        String identity = (String) params.get("identity");
        
        for (Map<String, Object> user : mockUsers) {
            if (user.get("userId").equals(userId)) {
                user.put("identities", identity);
                user.put("isArtist", identity.contains("artist"));
                user.put("isAgent", identity.contains("agent"));
                user.put("isCollector", identity.contains("collector"));
                user.put("isPromoter", identity.contains("promoter"));
                log.info("更新用户身份: userId={}, identity={}", userId, identity);
                return Result.success();
            }
        }
        return Result.fail(404, "用户不存在");
    }
    
    /**
     * 更新用户状态
     */
    @PostMapping("/updateStatus")
    public Result<Void> updateStatus(@RequestBody Map<String, Object> params) {
        Long userId = params.containsKey("userId") ? ((Number) params.get("userId")).longValue() : null;
        Integer status = (Integer) params.get("status");
        
        for (Map<String, Object> user : mockUsers) {
            if (user.get("userId").equals(userId)) {
                user.put("status", status);
                log.info("更新用户状态: userId={}, status={}", userId, status);
                return Result.success();
            }
        }
        return Result.fail(404, "用户不存在");
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    public Result<Void> deleteUser(@RequestBody Map<String, Object> params) {
        Long userId = params.containsKey("userId") ? ((Number) params.get("userId")).longValue() : null;
        
        boolean removed = mockUsers.removeIf(u -> u.get("userId").equals(userId));
        if (removed) {
            log.info("删除用户: userId={}", userId);
            return Result.success();
        }
        return Result.fail(404, "用户不存在");
    }

    // ==================== 艺荐官管理接口 ====================

    /**
     * 艺荐官列表
     */
    @GetMapping("/promoter/list")
    public Result<Map<String, Object>> getPromoterList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer level) {
        
        List<Map<String, Object>> list = new ArrayList<>();
        
        // 模拟数据
        String[] nicknames = {"艺荐官A", "艺荐官B", "艺荐官C", "金牌推广员", "高级代理", "资深推广"};
        int[] levels = {3, 2, 1, 3, 2, 1};
        
        for (int i = 0; i < nicknames.length; i++) {
            if (level != null && levels[i] != level) continue;
            
            Map<String, Object> promoter = new HashMap<>();
            promoter.put("userId", "2000" + (i + 1));
            promoter.put("nickname", nicknames[i]);
            promoter.put("phone", "1390013" + String.format("%04d", 9001 + i));
            promoter.put("avatar", "");
            promoter.put("level", levels[i]);
            promoter.put("teamCount", 100 + i * 20);
            promoter.put("directCount", 15 + i * 5);
            promoter.put("totalCommission", 50000 + i * 20000);
            promoter.put("withdrawable", 5000 + i * 2000);
            promoter.put("becomeTime", "2023-0" + (i % 9 + 1) + "-01 10:00:00");
            promoter.put("status", 1);
            list.add(promoter);
        }
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", 1256);
        stats.put("monthlyNew", 89);
        stats.put("pendingCommission", 258000);
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", (long) list.size());
        result.put("stats", stats);
        return Result.success(result);
    }

    /**
     * 添加艺荐官
     */
    @PostMapping("/promoter/add")
    public Result<Void> addPromoter(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        Integer level = (Integer) params.get("level");
        String remark = (String) params.getOrDefault("remark", "");
        
        log.info("添加艺荐官: userId={}, level={}, remark={}", userId, level, remark);
        
        // TODO: 调用 user 服务添加艺荐官
        // promoterService.addPromoter(userId, level, remark);
        
        return Result.success();
    }

    /**
     * 冻结/解冻艺荐官
     */
    @PostMapping("/promoter/freeze")
    public Result<Void> freezePromoter(@RequestBody Map<String, Object> params) {
        String userId = (String) params.get("userId");
        Integer status = (Integer) params.get("status");
        
        log.info("更新艺荐官状态: userId={}, status={}", userId, status);
        
        // TODO: 调用 user 服务更新艺荐官状态
        // promoterService.updateStatus(userId, status);
        
        return Result.success();
    }

    /**
     * 艺荐官详情
     */
    @GetMapping("/promoter/{userId}")
    public Result<Map<String, Object>> getPromoterDetail(@PathVariable String userId) {
        Map<String, Object> promoter = new HashMap<>();
        promoter.put("userId", userId);
        promoter.put("nickname", "艺荐官");
        promoter.put("phone", "13900139001");
        promoter.put("avatar", "");
        promoter.put("level", 2);
        promoter.put("teamCount", 156);
        promoter.put("directCount", 28);
        promoter.put("totalCommission", 125000);
        promoter.put("withdrawable", 8500);
        promoter.put("becomeTime", "2023-06-01 10:00:00");
        promoter.put("status", 1);
        return Result.success(promoter);
    }

    /**
     * 艺荐官团队成员
     */
    @GetMapping("/promoter/team/{userId}")
    public Result<List<Map<String, Object>>> getPromoterTeam(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Map<String, Object>> members = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> member = new HashMap<>();
            member.put("userId", "3000" + i);
            member.put("nickname", "团队成员" + i);
            member.put("phone", "13800" + String.format("%05d", i));
            member.put("level", 1);
            member.put("joinTime", "2023-0" + (i % 9 + 1) + "-15");
            member.put("teamCount", i % 3);
            members.add(member);
        }
        
        return Result.success(members);
    }

    /**
     * 艺荐官佣金记录
     */
    @GetMapping("/promoter/commission/{userId}")
    public Result<List<Map<String, Object>>> getPromoterCommission(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> record = new HashMap<>();
            record.put("id", i);
            record.put("orderNo", "SYJ202401" + String.format("%02d", 20 - i) + "001");
            record.put("amount", 10000 + i * 5000);
            record.put("rate", i % 2 == 0 ? 0.15 : 0.1);
            record.put("commission", (int) ((10000 + i * 5000) * (i % 2 == 0 ? 0.15 : 0.1)));
            record.put("status", i % 3 == 0 ? "pending" : "settled");
            record.put("time", "2024-01-" + String.format("%02d", 25 - i) + " 10:30:00");
            records.add(record);
        }
        
        return Result.success(records);
    }
}
