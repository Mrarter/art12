package com.shiyiju.product.service;

import com.shiyiju.product.config.IdGenerator;
import com.shiyiju.product.entity.*;
import com.shiyiju.product.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * ID迁移服务
 * 用于将旧的自增ID迁移为新的标准化19位ID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdMigrationService {

    private final SysUserMapper sysUserMapper;
    private final ArtworkMapper artworkMapper;
    private final AuctionSessionMapper auctionSessionMapper;
    private final AuctionLotMapper auctionLotMapper;
    private final CommunityPostMapper communityPostMapper;
    private final PostCommentMapper postCommentMapper;
    private final TopicMapper topicMapper;
    private final WithdrawRecordMapper withdrawRecordMapper;
    private final CommissionLogMapper commissionLogMapper;
    private final AuctionBidMapper auctionBidMapper;
    private final RefundRecordMapper refundRecordMapper;

    // ID映射缓存，用于关联表更新
    private final Map<String, Map<Long, String>> idMappingCache = new HashMap<>();

    /**
     * 迁移所有实体ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateAllIds() {
        log.info("开始ID迁移任务...");
        
        migrateUserIds();
        migrateArtworkIds();
        migrateAuctionSessionIds();
        migrateAuctionLotIds();
        migratePostIds();
        migrateCommentIds();
        migrateTopicIds();
        migrateWithdrawIds();
        migrateCommissionIds();
        migrateBidIds();
        migrateRefundIds();
        
        log.info("ID迁移任务完成！共迁移 {} 个实体类型", 11);
    }

    /**
     * 迁移用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateUserIds() {
        log.info("迁移用户ID...");
        var users = sysUserMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (SysUser user : users) {
            if (user.getUid() == null || user.getUid().isEmpty()) {
                String newId = IdGenerator.generateUserId();
                user.setUid(newId);
                sysUserMapper.updateById(user);
                mapping.put(user.getId(), newId);
                log.debug("用户ID迁移: {} -> {}", user.getId(), newId);
            }
        }
        
        idMappingCache.put("user", mapping);
        log.info("用户ID迁移完成，共 {} 条", users.size());
    }

    /**
     * 迁移作品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateArtworkIds() {
        log.info("迁移作品ID...");
        var artworks = artworkMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (Artwork artwork : artworks) {
            if (artwork.getArtworkUid() == null || artwork.getArtworkUid().isEmpty()) {
                String newId = IdGenerator.generateArtworkId();
                artwork.setArtworkUid(newId);
                artworkMapper.updateById(artwork);
                mapping.put(artwork.getId(), newId);
                log.debug("作品ID迁移: {} -> {}", artwork.getId(), newId);
            }
        }
        
        idMappingCache.put("artwork", mapping);
        log.info("作品ID迁移完成，共 {} 条", artworks.size());
    }

    /**
     * 迁移拍卖专场ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateAuctionSessionIds() {
        log.info("迁移拍卖专场ID...");
        var sessions = auctionSessionMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (AuctionSession session : sessions) {
            if (session.getSessionCode() == null || session.getSessionCode().isEmpty()) {
                String newId = IdGenerator.generateSessionId();
                session.setSessionCode(newId);
                auctionSessionMapper.updateById(session);
                mapping.put(session.getId(), newId);
            }
        }
        
        idMappingCache.put("session", mapping);
        log.info("拍卖专场ID迁移完成，共 {} 条", sessions.size());
    }

    /**
     * 迁移拍卖拍品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateAuctionLotIds() {
        log.info("迁移拍卖拍品ID...");
        var lots = auctionLotMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (AuctionLot lot : lots) {
            if (lot.getLotCode() == null || lot.getLotCode().isEmpty()) {
                String newId = IdGenerator.generateLotId();
                lot.setLotCode(newId);
                auctionLotMapper.updateById(lot);
                mapping.put(lot.getId(), newId);
            }
        }
        
        idMappingCache.put("lot", mapping);
        log.info("拍卖拍品ID迁移完成，共 {} 条", lots.size());
    }

    /**
     * 迁移帖子ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migratePostIds() {
        log.info("迁移帖子ID...");
        var posts = communityPostMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (CommunityPost post : posts) {
            if (post.getPostCode() == null || post.getPostCode().isEmpty()) {
                String newId = IdGenerator.generatePostId();
                post.setPostCode(newId);
                communityPostMapper.updateById(post);
                mapping.put(post.getId(), newId);
            }
        }
        
        idMappingCache.put("post", mapping);
        log.info("帖子ID迁移完成，共 {} 条", posts.size());
    }

    /**
     * 迁移评论ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateCommentIds() {
        log.info("迁移评论ID...");
        var comments = postCommentMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (PostComment comment : comments) {
            if (comment.getCommentCode() == null || comment.getCommentCode().isEmpty()) {
                String newId = IdGenerator.generateCommentId();
                comment.setCommentCode(newId);
                postCommentMapper.updateById(comment);
                mapping.put(comment.getId(), newId);
            }
        }
        
        idMappingCache.put("comment", mapping);
        log.info("评论ID迁移完成，共 {} 条", comments.size());
    }

    /**
     * 迁移话题ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateTopicIds() {
        log.info("迁移话题ID...");
        var topics = topicMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (Topic topic : topics) {
            if (topic.getTopicCode() == null || topic.getTopicCode().isEmpty()) {
                String newId = IdGenerator.generateTopicId();
                topic.setTopicCode(newId);
                topicMapper.updateById(topic);
                mapping.put(topic.getId(), newId);
            }
        }
        
        idMappingCache.put("topic", mapping);
        log.info("话题ID迁移完成，共 {} 条", topics.size());
    }

    /**
     * 迁移提现记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateWithdrawIds() {
        log.info("迁移提现记录ID...");
        var records = withdrawRecordMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (WithdrawRecord record : records) {
            if (record.getWithdrawCode() == null || record.getWithdrawCode().isEmpty()) {
                String newId = IdGenerator.generateWithdrawId();
                record.setWithdrawCode(newId);
                withdrawRecordMapper.updateById(record);
                mapping.put(record.getId(), newId);
            }
        }
        
        idMappingCache.put("withdraw", mapping);
        log.info("提现记录ID迁移完成，共 {} 条", records.size());
    }

    /**
     * 迁移佣金记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateCommissionIds() {
        log.info("迁移佣金记录ID...");
        var logs = commissionLogMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (CommissionLog log : logs) {
            if (log.getCommissionCode() == null || log.getCommissionCode().isEmpty()) {
                String newId = IdGenerator.generateCommissionId();
                log.setCommissionCode(newId);
                commissionLogMapper.updateById(log);
                mapping.put(log.getId(), newId);
            }
        }
        
        idMappingCache.put("commission", mapping);
        log.info("佣金记录ID迁移完成，共 {} 条", logs.size());
    }

    /**
     * 迁移竞拍记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateBidIds() {
        log.info("迁移竞拍记录ID...");
        var bids = auctionBidMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (AuctionBid bid : bids) {
            if (bid.getBidCode() == null || bid.getBidCode().isEmpty()) {
                String newId = IdGenerator.generateBidId();
                bid.setBidCode(newId);
                auctionBidMapper.updateById(bid);
                mapping.put(bid.getId(), newId);
            }
        }
        
        idMappingCache.put("bid", mapping);
        log.info("竞拍记录ID迁移完成，共 {} 条", bids.size());
    }

    /**
     * 迁移售后记录ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void migrateRefundIds() {
        log.info("迁移售后记录ID...");
        var records = refundRecordMapper.selectList(null);
        Map<Long, String> mapping = new HashMap<>();
        
        for (RefundRecord record : records) {
            if (record.getRefundCode() == null || record.getRefundCode().isEmpty()) {
                String newId = IdGenerator.generateAfterSaleId();
                record.setRefundCode(newId);
                refundRecordMapper.updateById(record);
                mapping.put(record.getId(), newId);
            }
        }
        
        idMappingCache.put("aftersale", mapping);
        log.info("售后记录ID迁移完成，共 {} 条", records.size());
    }

    /**
     * 根据旧ID获取新ID
     */
    public String getNewId(String entityType, Long oldId) {
        Map<Long, String> mapping = idMappingCache.get(entityType);
        if (mapping != null) {
            return mapping.get(oldId);
        }
        return null;
    }
}
