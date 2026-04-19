package com.shiyiju.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shiyiju.common.constant.ProductConstant;
import com.shiyiju.common.exception.BusinessException;
import com.shiyiju.common.result.PageResult;
import com.shiyiju.common.result.ResultCode;
import com.shiyiju.product.dto.ArtworkQueryDTO;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final RedisTemplate<String, Object> redisTemplate;

    /** 获取艺术门类列表 */
    public List<Category> getCategoryList() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSortOrder)
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
            wrapper.orderByDesc(Artwork::getCreateTime);
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
        wrapper.eq(Artwork::getStatus, ProductConstant.STATUS_ON_SALE);
        
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

        List<Artwork> artworks = artworkMapper.selectBatchIds(artworkIds);
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
        return bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, 1)
                        .le(Banner::getStartTime, LocalDateTime.now())
                        .ge(Banner::getEndTime, LocalDateTime.now())
                        .orderByAsc(Banner::getSortOrder)
        );
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
        vo.setMedium(artwork.getMedium());
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
        vo.setStock(artwork.getStock());
        vo.setPriceRise(artwork.getPriceRise() != null ? artwork.getPriceRise() : BigDecimal.ZERO);
        vo.setViewCount(artwork.getViewCount() != null ? artwork.getViewCount() : 0);
        vo.setFavoriteCount(artwork.getFavoriteCount() != null ? artwork.getFavoriteCount() : 0);
        vo.setSaleCount(artwork.getSaleCount() != null ? artwork.getSaleCount() : 0);
        vo.setCreateTime(artwork.getCreateTime() != null ? artwork.getCreateTime().toString() : null);

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

        // 模拟作者信息（实际应调用用户服务）
        vo.setAuthorName("艺术家");
        vo.setAuthorBadge("艺术家");

        return vo;
    }
}
