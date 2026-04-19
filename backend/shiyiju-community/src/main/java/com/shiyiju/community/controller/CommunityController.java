package com.shiyiju.community.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.Result;
import com.shiyiju.community.entity.*;
import com.shiyiju.community.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/community")
@RequiredArgsConstructor
public class CommunityController {

    private final PostMapper postMapper;
    private final PostCommentMapper commentMapper;
    private final PostLikeMapper likeMapper;
    private final TopicMapper topicMapper;

    /**
     * 获取帖子流 (GET /posts)
     */
    @GetMapping("/posts")
    public Result<PageResult<PostVO>> getPosts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long topicId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getStatus, 1);
        if (topicId != null) {
            wrapper.eq(Post::getTopicId, topicId);
        }
        wrapper.orderByDesc(Post::getCreateTime);

        Page<Post> result = postMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<PostVO> voList = result.getRecords().stream()
                .map(p -> convertToVO(p, userId)).toList();

        return Result.success(PageResult.of(result.getTotal(), page, pageSize, voList));
    }

    /**
     * 发帖 (POST /posts)
     */
    @Transactional
    @PostMapping("/posts")
    public Result<Map<String, Long>> createPost(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        Post post = new Post();
        post.setUserId(userId);
        post.setContent(params.get("content") != null ? params.get("content").toString() : null);
        post.setImages(params.get("images") != null ? params.get("images").toString() : null);
        post.setTopicId(params.get("topicId") != null ? 
                Long.valueOf(params.get("topicId").toString()) : null);
        post.setArtworkId(params.get("artworkId") != null ? 
                Long.valueOf(params.get("artworkId").toString()) : null);
        post.setStatus(1);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setShareCount(0);
        post.setCreateTime(LocalDateTime.now());
        postMapper.insert(post);
        
        Map<String, Long> result = new HashMap<>();
        result.put("postId", post.getId());
        return Result.success(result);
    }

    /**
     * 获取帖子详情 (GET /posts/{post_id})
     */
    @GetMapping("/posts/{postId}")
    public Result<PostVO> getPostDetail(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            return Result.fail(404, "帖子不存在");
        }
        return Result.success(convertToVO(post, userId));
    }

    /**
     * 点赞帖子 (POST /posts/{post_id}/like)
     */
    @Transactional
    @PostMapping("/posts/{postId}/like")
    public Result<Void> likePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        PostLike existing = likeMapper.selectOne(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId)
        );
        if (existing != null) {
            return Result.success();
        }

        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        likeMapper.insert(like);

        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
        }

        return Result.success();
    }

    /**
     * 取消点赞
     */
    @Transactional
    @PostMapping("/posts/{postId}/unlike")
    public Result<Void> unlikePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        likeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId));
        Post post = postMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }
        return Result.success();
    }

    /**
     * 评论帖子 (POST /posts/{post_id}/comments)
     */
    @Transactional
    @PostMapping("/posts/{postId}/comments")
    public Result<Map<String, Long>> commentPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody Map<String, Object> params
    ) {
        if (userId == null) {
            return Result.fail(401, "请先登录");
        }
        
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(params.get("content") != null ? params.get("content").toString() : null);
        comment.setParentId(params.get("parentId") != null ? 
                Long.valueOf(params.get("parentId").toString()) : null);
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        Post post = postMapper.selectById(postId);
        if (post != null) {
            post.setCommentCount(post.getCommentCount() + 1);
            postMapper.updateById(post);
        }

        Map<String, Long> result = new HashMap<>();
        result.put("commentId", comment.getId());
        return Result.success(result);
    }

    /**
     * 获取评论列表 (GET /posts/{post_id}/comments)
     */
    @GetMapping("/posts/{postId}/comments")
    public Result<PageResult<PostComment>> getComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Page<PostComment> pageResult = new Page<>(page, pageSize);
        Page<PostComment> result = commentMapper.selectPage(pageResult,
                new LambdaQueryWrapper<PostComment>()
                        .eq(PostComment::getPostId, postId)
                        .orderByAsc(PostComment::getCreateTime));
        
        return Result.success(PageResult.of(result.getTotal(), page, pageSize, result.getRecords()));
    }

    /**
     * 获取话题列表 (GET /topics)
     */
    @GetMapping("/topics")
    public Result<List<Topic>> getTopics() {
        return Result.success(topicMapper.selectList(
                new LambdaQueryWrapper<Topic>()
                        .eq(Topic::getStatus, 1)
                        .orderByAsc(Topic::getSortOrder)
        ));
    }

    private PostVO convertToVO(Post post, Long userId) {
        PostVO vo = new PostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setContent(post.getContent());
        if (post.getImages() != null && !post.getImages().isEmpty()) {
            vo.setImages(Arrays.asList(post.getImages().split(",")));
        }
        vo.setTopicId(post.getTopicId());
        vo.setArtworkId(post.getArtworkId());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setShareCount(post.getShareCount());
        vo.setCreateTime(post.getCreateTime() != null ? post.getCreateTime().toString() : null);
        vo.setAuthorName("用户" + post.getUserId());
        
        // 检查是否已点赞
        if (userId != null) {
            PostLike like = likeMapper.selectOne(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getPostId, post.getId())
                            .eq(PostLike::getUserId, userId)
            );
            vo.setIsLiked(like != null);
        }
        
        return vo;
    }

    static class PostVO {
        public Long id;
        public Long userId;
        public String authorName;
        public String content;
        public List<String> images;
        public Long topicId;
        public Long artworkId;
        public Integer likeCount;
        public Integer commentCount;
        public Integer shareCount;
        public String createTime;
        public Boolean isLiked;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
        public Long getTopicId() { return topicId; }
        public void setTopicId(Long topicId) { this.topicId = topicId; }
        public Long getArtworkId() { return artworkId; }
        public void setArtworkId(Long artworkId) { this.artworkId = artworkId; }
        public Integer getLikeCount() { return likeCount; }
        public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
        public Integer getCommentCount() { return commentCount; }
        public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
        public Integer getShareCount() { return shareCount; }
        public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }
        public String getCreateTime() { return createTime; }
        public void setCreateTime(String createTime) { this.createTime = createTime; }
        public Boolean getIsLiked() { return isLiked; }
        public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
    }
}
