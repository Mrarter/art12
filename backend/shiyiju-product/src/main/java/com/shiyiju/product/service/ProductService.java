package com.shiyiju.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.product.dto.ArtworkQueryDTO;
import com.shiyiju.product.dto.ArtworkUpdateDTO;
import com.shiyiju.product.entity.Artwork;
import com.shiyiju.product.entity.ArtworkFavorite;
import com.shiyiju.product.entity.Banner;
import com.shiyiju.product.entity.Category;
import com.shiyiju.product.mapper.ArtworkFavoriteMapper;
import com.shiyiju.product.mapper.ArtworkMapper;
import com.shiyiju.product.mapper.BannerMapper;
import com.shiyiju.product.mapper.CategoryMapper;
import com.shiyiju.common.vo.ArtworkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final CategoryMapper categoryMapper;
    private final ArtworkMapper artworkMapper;
    private final ArtworkFavoriteMapper favoriteMapper;
    private final BannerMapper bannerMapper;
    private final PriceGrowthService priceGrowthService;
    private final RestTemplate restTemplate;

    /** 获取艺术门类列表（按权重降序，权重大的在前） */
    public List<Category> getCategoryList() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByDesc(Category::getSort)  // 权重大的排前面
        );
    }

    /** 获取作品列表 (新版，支持更多筛选参数) */
    public PageResult<ArtworkVO> getProductList(ArtworkQueryDTO query, Long userId) {
        LambdaQueryWrapper<Artwork> wrapper = buildArtworkQueryWrapper(query);
        
        // 处理排序 - 新API格式
        if (query.getSort() != null) {
            switch (query.getSort()) {
                case "price_asc" -> wrapper.orderByAsc(Artwork::getPrice);
                case "price_desc" -> wrapper.orderByDesc(Artwork::getPrice);
                case "time", "new" -> wrapper.orderByDesc(Artwork::getCreateTime);
                default -> wrapper.orderByDesc(Artwork::getCreateTime);
            }
        } else if (query.getSortBy() != null) {
            // 旧API兼容
            if ("price".equals(query.getSortBy())) {
                if ("asc".equalsIgnoreCase(query.getSortOrder())) {
                    wrapper.orderByAsc(Artwork::getPrice);
                } else {
                    wrapper.orderByDesc(Artwork::getPrice);
                }
            } else if ("saleCount".equals(query.getSortBy())) {
                wrapper.orderByDesc(Artwork::getSaleCount);
            } else {
                wrapper.orderByDesc(Artwork::getCreateTime);
            }
} else {
            // 默认按权重降序，然后按创建时间排序
            wrapper.orderByDesc(Artwork::getWeight).orderByDesc(Artwork::getCreateTime);
        }

        Page<Artwork> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Artwork> result = artworkMapper.selectPage(page, wrapper);

        List<ArtworkVO> voList = result.getRecords().stream()
                .map(a -> convertToVO(a, userId))
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), query.getPage(), query.getPageSize(), voList);
    }

    /** 获取作品列表 (旧方法，保持兼容) */
    public PageResult<ArtworkVO> getArtworkList(ArtworkQueryDTO query, Long userId) {
        return getProductList(query, userId);
    }

    /** 构建作品查询条件 */
    private LambdaQueryWrapper<Artwork> buildArtworkQueryWrapper(ArtworkQueryDTO query) {
        LambdaQueryWrapper<Artwork> wrapper = new LambdaQueryWrapper<>();
        
        // 只有明确指定状态时才筛选，否则查所有状态
        if (query.getStatus() != null) {
            wrapper.eq(Artwork::getStatus, query.getStatus());
        }
        
        // 作品ID精确搜索
        if (query.getId() != null) {
            wrapper.eq(Artwork::getId, query.getId());
        }
        // 作品名称模糊搜索
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            wrapper.like(Artwork::getTitle, query.getTitle());
        }
        // 艺术家名称模糊搜索
        if (query.getAuthorName() != null && !query.getAuthorName().isEmpty()) {
            wrapper.like(Artwork::getAuthorName, query.getAuthorName());
        }
        
        if (query.getCategoryId() != null) {
            wrapper.eq(Artwork::getCategoryId, query.getCategoryId());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(Artwork::getTitle, query.getKeyword())
                    .or().like(Artwork::getDescription, query.getKeyword()));
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(Artwork::getPrice, query.getMinPrice() * 100L);
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(Artwork::getPrice, query.getMaxPrice() * 100L);
        }
        if (query.getYearFrom() != null) {
            wrapper.ge(Artwork::getYear, query.getYearFrom());
        }
        if (query.getYearTo() != null) {
            wrapper.le(Artwork::getYear, query.getYearTo());
        }
        // region和holdDuration需要关联查询，这里简化处理
        
        return wrapper;
    }

    /** 获取我的收藏列表 */
    public PageResult<ArtworkVO> getMyFavorites(Long userId, Integer page, Integer pageSize) {
        List<ArtworkFavorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<ArtworkFavorite>()
                        .eq(ArtworkFavorite::getUserId, userId)
                        .orderByDesc(ArtworkFavorite::getCreateTime)
        );

        if (favorites.isEmpty()) {
            return PageResult.of(0L, page, pageSize, List.of());
        }

        List<Long> artworkIds = favorites.stream()
                .map(ArtworkFavorite::getArtworkId)
                .collect(Collectors.toList());

        List<Artwork> artworks = artworkMapper.selectByIds(artworkIds);
        List<ArtworkVO> voList = artworks.stream()
                .map(a -> convertToVO(a, userId))
                .collect(Collectors.toList());

        return PageResult.of((long) voList.size(), page, pageSize, voList);
    }

    /** 获取作品详情 */
    @Transactional
    public ArtworkVO getArtworkDetail(Long id, Long userId) {
        Artwork artwork = artworkMapper.selectById(id);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        
        // 增加浏览量
        artwork.setViewCount(artwork.getViewCount() == null ? 1 : artwork.getViewCount() + 1);
        artworkMapper.updateById(artwork);
        
        // 实时计算价格（基于新的浏览量）
        priceGrowthService.updateSinglePrice(artwork.getId());
        artwork = artworkMapper.selectById(id); // 重新获取更新后的作品
        
        return convertToVO(artwork, userId);
    }

    /** 收藏作品 */
    @Transactional
    public void favoriteArtwork(Long artworkId, Long userId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        
        ArtworkFavorite existing = favoriteMapper.selectOne(
                new LambdaQueryWrapper<ArtworkFavorite>()
                        .eq(ArtworkFavorite::getUserId, userId)
                        .eq(ArtworkFavorite::getArtworkId, artworkId)
        );
        
        if (existing != null) {
            return; // 已收藏
        }
        
        ArtworkFavorite favorite = new ArtworkFavorite();
        favorite.setUserId(userId);
        favorite.setArtworkId(artworkId);
        favorite.setCreateTime(LocalDateTime.now());
        favoriteMapper.insert(favorite);
        
        // 更新收藏数
        artwork.setFavoriteCount(artwork.getFavoriteCount() == null ? 1 : artwork.getFavoriteCount() + 1);
        artworkMapper.updateById(artwork);
        
        // 实时计算价格（基于新的收藏量）
        priceGrowthService.updateSinglePrice(artworkId);
    }

    /** 取消收藏 */
    @Transactional
    public void unfavoriteArtwork(Long artworkId, Long userId) {
        favoriteMapper.delete(
                new LambdaQueryWrapper<ArtworkFavorite>()
                        .eq(ArtworkFavorite::getUserId, userId)
                        .eq(ArtworkFavorite::getArtworkId, artworkId)
        );
        
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork != null && artwork.getFavoriteCount() != null && artwork.getFavoriteCount() > 0) {
            artwork.setFavoriteCount(artwork.getFavoriteCount() - 1);
            artworkMapper.updateById(artwork);
        }
    }

    /** 获取首页Banner */
    public List<Banner> getBanners() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Banner::getStatus, 1);
        // 处理时间条件：(start_time IS NULL OR start_time <= now) AND (end_time IS NULL OR end_time >= now)
        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w -> w
            .and(n -> n.isNull(Banner::getStartTime).or().le(Banner::getStartTime, now))
            .and(n -> n.isNull(Banner::getEndTime).or().ge(Banner::getEndTime, now))
        );
        wrapper.orderByAsc(Banner::getSort);
        return bannerMapper.selectList(wrapper);
    }

    /** 创建作品 */
    @Transactional
    public Long createArtwork(ArtworkUpdateDTO dto) {
        // 处理艺术家：如果有作者名称但没有作者ID，自动查找或创建
        Long authorId = dto.getAuthorId();
        String authorName = dto.getAuthorName();
        if (authorId == null && authorName != null && !authorName.isEmpty()) {
            authorId = findOrCreateArtist(authorName);
        }
        if (authorId == null) {
            authorId = 1L; // 默认作者ID
        }

        Artwork artwork = new Artwork();
        artwork.setTitle(dto.getTitle());
        artwork.setAuthorId(authorId);
        artwork.setAuthorName(authorName);
        artwork.setCategoryId(dto.getCategoryId());
        artwork.setCoverImage(dto.getCover() != null ? dto.getCover() : "https://picsum.photos/400/400");
        artwork.setPrice(dto.getPrice() != null ? dto.getPrice().multiply(BigDecimal.valueOf(100)).longValue() : 0L);
        artwork.setOriginalPrice(dto.getOriginalPrice() != null ? dto.getOriginalPrice().multiply(BigDecimal.valueOf(100)).longValue() : null);
        artwork.setStock(dto.getStock() != null ? dto.getStock() : 0);
        artwork.setDescription(dto.getDescription());
        artwork.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        artwork.setWeight(dto.getWeight() != null ? dto.getWeight() : 0);
        artwork.setOwnershipType(dto.getOwnershipType() != null ? dto.getOwnershipType() : 1); // 默认原创
        artwork.setArtType(dto.getArtType());
        artwork.setSize(dto.getSize());
        artwork.setYear(dto.getYear());
        artwork.setDistributionEnabled(dto.getDistributionEnabled() != null ? dto.getDistributionEnabled() : false);
        artwork.setCommissionRate(dto.getCommissionRate() != null ? dto.getCommissionRate() : 10);
        artwork.setCreateTime(LocalDateTime.now());
        artwork.setUpdateTime(LocalDateTime.now());
        // 生成作品编号：画种缩写 + 日期 + 序号
        String artworkCode = generateArtworkCode(dto.getArtType(), dto.getCategoryId());
        artwork.setArtworkCode(artworkCode);
        artworkMapper.insert(artwork);
        return artwork.getId();
    }
    
    /** 生成作品编号 */
    private String generateArtworkCode(String artType, Long categoryId) {
        // 根据画种生成前缀
        String prefix = getTypePrefix(artType);
        // 日期格式：yyyyMMdd
        String dateStr = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 查询当天该类型作品数量
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        Long count = artworkMapper.selectCount(new LambdaQueryWrapper<Artwork>()
                .ge(Artwork::getCreateTime, startOfDay)
                .lt(Artwork::getCreateTime, endOfDay)
                .likeRight(Artwork::getArtworkCode, prefix + dateStr));
        // 生成序号，从1开始
        int seq = (count != null ? count.intValue() : 0) + 1;
        return prefix + dateStr + String.format("%04d", seq);
    }
    
    /** 根据画种获取前缀缩写 */
    private String getTypePrefix(String artType) {
        if (artType == null) return "qt"; // 其他
        String lower = artType.toLowerCase();
        if (lower.contains("油画")) return "yh";
        if (lower.contains("国画") || lower.contains("水墨")) return "gh";
        if (lower.contains("书法")) return "sf";
        if (lower.contains("版画")) return "bk";
        if (lower.contains("雕塑")) return "ds";
        if (lower.contains("水彩")) return "sc";
        if (lower.contains("素描")) return "sm";
        if (lower.contains("丙烯")) return "bj";
        return "qt"; // 其他
    }

    /** 更新作品 */
    @Transactional
    public void updateArtwork(ArtworkUpdateDTO dto) {
        Artwork artwork = artworkMapper.selectById(dto.getId());
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (dto.getTitle() != null) artwork.setTitle(dto.getTitle());
        
        // 处理艺术家：如果有作者名称但没有作者ID，自动查找或创建
        if (dto.getAuthorName() != null && !dto.getAuthorName().isEmpty()) {
            artwork.setAuthorName(dto.getAuthorName());
            // 只有当 authorId 为空时才自动创建
            if (dto.getAuthorId() == null) {
                Long authorId = findOrCreateArtist(dto.getAuthorName());
                if (authorId != null) {
                    artwork.setAuthorId(authorId);
                }
            } else {
                artwork.setAuthorId(dto.getAuthorId());
            }
        } else if (dto.getAuthorId() != null) {
            artwork.setAuthorId(dto.getAuthorId());
        }
        if (dto.getCategoryId() != null) artwork.setCategoryId(dto.getCategoryId());
        if (dto.getCover() != null) artwork.setCoverImage(dto.getCover());
        // 价格单位是分，转换 BigDecimal -> Long (分)
        if (dto.getPrice() != null) artwork.setPrice(dto.getPrice().multiply(BigDecimal.valueOf(100)).longValue());
        if (dto.getOriginalPrice() != null) artwork.setOriginalPrice(dto.getOriginalPrice().multiply(BigDecimal.valueOf(100)).longValue());
        if (dto.getStock() != null) artwork.setStock(dto.getStock());
if (dto.getDescription() != null) artwork.setDescription(dto.getDescription());
        if (dto.getStatus() != null) artwork.setStatus(dto.getStatus());
        if (dto.getWeight() != null) artwork.setWeight(dto.getWeight());
        if (dto.getOwnershipType() != null) artwork.setOwnershipType(dto.getOwnershipType());
        if (dto.getArtType() != null) artwork.setArtType(dto.getArtType());
        if (dto.getSize() != null) artwork.setSize(dto.getSize());
        if (dto.getYear() != null) artwork.setYear(dto.getYear());
        // 分销相关
        if (dto.getDistributionEnabled() != null) artwork.setDistributionEnabled(dto.getDistributionEnabled());
        if (dto.getCommissionRate() != null) artwork.setCommissionRate(dto.getCommissionRate());
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);
    }

    /** 批量更新作品状态 */
    @Transactional
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) return;
        for (Long id : ids) {
            Artwork artwork = artworkMapper.selectById(id);
            if (artwork != null) {
                artwork.setStatus(status);
                artwork.setUpdateTime(LocalDateTime.now());
                artworkMapper.updateById(artwork);
            }
        }
    }

    /** 删除作品 */
    @Transactional
    public void deleteArtwork(Long id) {
        artworkMapper.deleteById(id);
    }

    /** 获取推荐作品 */
    public PageResult<ArtworkVO> getRecommendArtworks(ArtworkQueryDTO query, Long userId) {
        // 简化实现：按创建时间和销量综合排序
        LambdaQueryWrapper<Artwork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Artwork::getStatus, ProductConstant.STATUS_ON_SALE);
        wrapper.orderByDesc(Artwork::getSaleCount)
                .orderByDesc(Artwork::getCreateTime);

        Page<Artwork> page = new Page<>(query.getPage(), query.getPageSize());
        Page<Artwork> result = artworkMapper.selectPage(page, wrapper);

        List<ArtworkVO> voList = result.getRecords().stream()
                .map(a -> convertToVO(a, userId))
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), query.getPage(), query.getPageSize(), voList);
    }

    /** 转换实体为VO */
    private ArtworkVO convertToVO(Artwork artwork, Long userId) {
        ArtworkVO vo = new ArtworkVO();
        vo.setId(artwork.getId());
        vo.setTitle(artwork.getTitle());
        vo.setAuthorId(artwork.getAuthorId());
        vo.setCategoryId(artwork.getCategoryId());
        vo.setArtType(artwork.getArtType());
        vo.setSize(artwork.getSize());
        vo.setYear(artwork.getYear());
        vo.setEdition(artwork.getEdition());
        vo.setDescription(artwork.getDescription());
        vo.setCoverImage(artwork.getCoverImage());
        if (artwork.getImages() != null) {
            vo.setImages(Arrays.asList(artwork.getImages().split(",")));
        }
        vo.setSource(artwork.getSource());
        vo.setPrice(artwork.getPrice());
        vo.setOriginalPrice(artwork.getOriginalPrice());
        // 实时计算当前价格（包含最新浏览量、收藏量等因素）
        Long currentPrice = priceGrowthService.calculateCurrentPrice(artwork);
        vo.setCurrentPrice(currentPrice);
        vo.setStock(artwork.getStock());
        vo.setStatus(artwork.getStatus());
        vo.setWeight(artwork.getWeight() != null ? artwork.getWeight() : 0);
        vo.setOwnershipType(artwork.getOwnershipType() != null ? artwork.getOwnershipType() : 1);
        vo.setArtworkCode(artwork.getArtworkCode());
        // 作品类型文本
        vo.setOwnershipTypeText(switch (artwork.getOwnershipType()) {
            case 1 -> "原创";
            case 2 -> "收藏";
            default -> "原创";
        });
        vo.setPriceRise(artwork.getPriceRise() != null ? artwork.getPriceRise() : BigDecimal.ZERO);
        vo.setViewCount(artwork.getViewCount() != null ? artwork.getViewCount() : 0);
        vo.setFavoriteCount(artwork.getFavoriteCount() != null ? artwork.getFavoriteCount() : 0);
        vo.setSaleCount(artwork.getSaleCount() != null ? artwork.getSaleCount() : 0);
        vo.setCreateTime(artwork.getCreateTime() != null ? artwork.getCreateTime().toString() : null);

        // 是否新品（创建时间在30天内）
        boolean isNew = false;
        if (artwork.getCreateTime() != null) {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            isNew = artwork.getCreateTime().isAfter(thirtyDaysAgo);
        }
        vo.setIsNew(isNew);
        
        // 是否热门（销量>0或收藏数>5）
        boolean isHot = (artwork.getSaleCount() != null && artwork.getSaleCount() > 0)
                || (artwork.getFavoriteCount() != null && artwork.getFavoriteCount() > 5);
        vo.setIsHot(isHot);

        // 来源文本
        vo.setSourceText(switch (artwork.getSource()) {
            case 1 -> "艺术家发布";
            case 2 -> "经纪人代理";
            case 3 -> "持有者转售";
            case 4 -> "平台自营";
            default -> "未知";
        });

        // 状态文本
        vo.setStatusText(switch (artwork.getStatus()) {
            case 0 -> "已下架";
            case 1 -> "上架中";
            case 2 -> "已售罄";
            default -> "未知";
        });

        // 检查是否已收藏
        if (userId != null) {
            ArtworkFavorite fav = favoriteMapper.selectOne(
                    new LambdaQueryWrapper<ArtworkFavorite>()
                            .eq(ArtworkFavorite::getUserId, userId)
                            .eq(ArtworkFavorite::getArtworkId, artwork.getId())
            );
            vo.setIsFavorited(fav != null);
        } else {
            vo.setIsFavorited(false);
        }

        // 作者信息
        vo.setAuthorName(artwork.getAuthorName());
        vo.setAuthorBadge(artwork.getAuthorBadge());
        vo.setAuthorAvatar(artwork.getAuthorAvatar());
        // 作者身份（从authorBadge推断）
        vo.setAuthorIdentity(getAuthorIdentity(artwork.getAuthorBadge()));
        // 作者简介（可扩展）
        vo.setAuthorBio(artwork.getAuthorBio());
        // 作者电话（可扩展）
        vo.setAuthorPhone(artwork.getAuthorPhone());

        // 检查是否已关注艺术家（需要关注表，这里简化处理）
        vo.setIsFollowing(false);

        // 计算持有时长（天）- 从创建时间或首次购买时间计算
        if (artwork.getCreateTime() != null) {
            long daysSinceCreation = java.time.Duration.between(
                    artwork.getCreateTime(), LocalDateTime.now()).toDays();
            vo.setHoldDuration((int) daysSinceCreation);
        }

        // 分销相关
        vo.setDistributionEnabled(artwork.getDistributionEnabled());
        vo.setCommissionRate(artwork.getCommissionRate());
        vo.setDistributionOrders(artwork.getDistributionOrders());
        vo.setDistributionEarnings(artwork.getDistributionEarnings());
        vo.setDistributionUsers(artwork.getDistributionUsers());
        vo.setStatus(artwork.getStatus());

        return vo;
    }

    /** 根据徽章推断艺术家身份 */
    private String getAuthorIdentity(String badge) {
        if (badge == null) return "artist";
        String lowerBadge = badge.toLowerCase();
        if (lowerBadge.contains("大师") || lowerBadge.contains("master")) {
            return "master";
        } else if (lowerBadge.contains("藏家") || lowerBadge.contains("collector")) {
            return "collector";
        } else if (lowerBadge.contains("机构") || lowerBadge.contains("gallery")) {
            return "gallery";
        }
        return "artist";
    }

    /**
     * 查找或创建艺术家
     * 如果艺术家名称已存在则返回其ID，否则创建待审核艺术家并返回ID
     */
    private Long findOrCreateArtist(String artistName) {
        if (artistName == null || artistName.trim().isEmpty()) {
            return null;
        }
        try {
            // 调用 user 服务的 API
            String url = "http://localhost:8081/user/artist/find-or-create?name=" + java.net.URLEncoder.encode(artistName.trim(), "UTF-8");
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.get("code") != null && ((Number) response.get("code")).intValue() == 0) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null && data.get("id") != null) {
                    Long artistId = ((Number) data.get("id")).longValue();
                    log.info("艺术家自动处理成功: 名称={}, ID={}, 是否新建={}", artistName, artistId, !Boolean.TRUE.equals(data.get("exists")));
                    return artistId;
                }
            }
            log.warn("艺术家自动处理返回异常: {}", response);
        } catch (Exception e) {
            log.error("调用艺术家查找/创建接口失败: {}", e.getMessage());
        }
        return null;
    }
}
