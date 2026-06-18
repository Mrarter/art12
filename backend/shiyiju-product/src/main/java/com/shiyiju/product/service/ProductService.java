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
import com.shiyiju.common.vo.ArtistInfoVO;
import com.shiyiju.common.vo.ArtworkVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
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
    private final JdbcTemplate jdbcTemplate;

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

        try {
            Page<Artwork> page = new Page<>(query.getPage(), query.getPageSize());
            Page<Artwork> result = artworkMapper.selectPage(page, wrapper);

            List<ArtworkVO> voList = result.getRecords().stream()
                    .map(a -> convertToListVO(a, userId))
                    .collect(Collectors.toList());

            long total = result.getTotal() > 0 ? result.getTotal() : voList.size();
            return PageResult.of(total, query.getPage(), query.getPageSize(), voList);
        } catch (BadSqlGrammarException e) {
            log.warn("作品列表查询命中旧库结构，降级到兼容查询: {}", e.getMessage());
            return getProductListFallback(query, userId);
        }
    }

    /** 获取我的作品列表（艺术家自己的作品） */
    public PageResult<ArtworkVO> getMyWorks(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Artwork> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Artwork::getAuthorId, userId)
                .orderByDesc(Artwork::getCreateTime);

        Page<Artwork> artworkPage = new Page<>(page, pageSize);
        Page<Artwork> result = artworkMapper.selectPage(artworkPage, wrapper);

        List<ArtworkVO> voList = result.getRecords().stream()
                .map(a -> convertToSimpleVO(a))
                .collect(Collectors.toList());

        return PageResult.of(result.getTotal(), page, pageSize, voList);
    }

    /** 转换为简化VO（避免调用可能出错的服务） */
    private ArtworkVO convertToSimpleVO(Artwork artwork) {
        loadArtworkPriceGrowthConfig(artwork);
        ArtworkVO vo = new ArtworkVO();
        vo.setId(artwork.getId());
        vo.setTitle(artwork.getTitle());
        vo.setAuthorId(artwork.getAuthorId());
        vo.setAuthorName(artwork.getAuthorName());
        vo.setCategoryId(artwork.getCategoryId());
        vo.setCategoryName(artwork.getArtType());
        vo.setArtType(artwork.getArtType());
        vo.setMaterial(artwork.getMedium());
        vo.setSize(artwork.getSize());
        vo.setYear(artwork.getYear());
        vo.setDescription(artwork.getDescription());
        // 优先使用 cover 字段，备选 coverImage
        String coverUrl = artwork.getCover() != null ? artwork.getCover() : artwork.getCoverImage();
        vo.setCoverImage(coverUrl);
        if (artwork.getImages() != null) {
            vo.setImages(Arrays.asList(artwork.getImages().split(",")));
        }
        vo.setPrice(artwork.getPrice());
        vo.setOriginalPrice(artwork.getOriginalPrice());
        vo.setCurrentPrice(BigDecimal.valueOf(priceGrowthService.calculateCurrentPrice(artwork)));
        vo.setStock(artwork.getStock());
        vo.setStatus(artwork.getStatus());
        vo.setWeight(artwork.getWeight() != null ? artwork.getWeight() : 0);
        vo.setArtworkCode(artwork.getArtworkCode());
        vo.setSaleCount(artwork.getSaleCount());
        applyHeatCounts(vo, artwork);
        return vo;
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
        if (query.getArtworkCode() != null && !query.getArtworkCode().isEmpty()) {
            wrapper.like(Artwork::getArtworkUid, query.getArtworkCode());
        }
        if (query.getAuthorId() != null) {
            wrapper.eq(Artwork::getAuthorId, query.getAuthorId());
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
        if (query.getArtType() != null && !query.getArtType().isEmpty()) {
            wrapper.eq(Artwork::getArtType, query.getArtType());
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
        try {
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
        } catch (BadSqlGrammarException e) {
            log.warn("Banner 查询命中旧库结构，降级到兼容查询: {}", e.getMessage());
            return getBannersFallback();
        }
    }

    private PageResult<ArtworkVO> getProductListFallback(ArtworkQueryDTO query, Long userId) {
        List<Object> whereArgs = new ArrayList<>();
        StringBuilder where = new StringBuilder(" FROM artwork WHERE 1=1");

        if (columnExists("artwork", "deleted")) {
            where.append(" AND deleted = 0");
        }
        if (query.getStatus() != null && columnExists("artwork", "status")) {
            where.append(" AND status = ?");
            whereArgs.add(query.getStatus());
        }
        if (query.getId() != null) {
            where.append(" AND id = ?");
            whereArgs.add(query.getId());
        }
        if (query.getTitle() != null && !query.getTitle().isEmpty()) {
            where.append(" AND title LIKE ?");
            whereArgs.add("%" + query.getTitle() + "%");
        }
        if (query.getAuthorName() != null && !query.getAuthorName().isEmpty()) {
            where.append(" AND author_name LIKE ?");
            whereArgs.add("%" + query.getAuthorName() + "%");
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            where.append(" AND (title LIKE ? OR description LIKE ?)");
            whereArgs.add("%" + query.getKeyword() + "%");
            whereArgs.add("%" + query.getKeyword() + "%");
        }
        if (query.getCategoryId() != null && columnExists("artwork", "category_id")) {
            where.append(" AND category_id = ?");
            whereArgs.add(query.getCategoryId());
        }
        if (query.getArtType() != null && !query.getArtType().isEmpty() && columnExists("artwork", "art_type")) {
            where.append(" AND art_type = ?");
            whereArgs.add(query.getArtType());
        }
        if (query.getMinPrice() != null && columnExists("artwork", "price")) {
            where.append(" AND price >= ?");
            whereArgs.add(query.getMinPrice() * 100L);
        }
        if (query.getMaxPrice() != null && columnExists("artwork", "price")) {
            where.append(" AND price <= ?");
            whereArgs.add(query.getMaxPrice() * 100L);
        }
        if (query.getYearFrom() != null && columnExists("artwork", "year")) {
            where.append(" AND year >= ?");
            whereArgs.add(query.getYearFrom());
        }
        if (query.getYearTo() != null && columnExists("artwork", "year")) {
            where.append(" AND year <= ?");
            whereArgs.add(query.getYearTo());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*)" + where,
                Long.class,
                whereArgs.toArray()
        );

        String coverExpr = columnExists("artwork", "cover_image")
                ? "cover_image"
                : (columnExists("artwork", "cover") ? "cover" : "NULL");
        String artworkCodeExpr = columnExists("artwork", "artwork_code")
                ? "artwork_code"
                : (columnExists("artwork", "artwork_uid") ? "artwork_uid" : "NULL");
        String authorUidExpr = columnExists("artwork", "author_uid") ? "author_uid" : "NULL";
        String categoryExpr = columnExists("artwork", "category_id") ? "category_id"
                : (columnExists("artwork", "category_name") ? "NULL" : "NULL");
        String artTypeExpr = columnExists("artwork", "art_type") ? "art_type"
                : (columnExists("artwork", "category_name") ? "category_name" : "NULL");
        String originalPriceExpr = columnExists("artwork", "original_price") ? "original_price" : "price";
        String stockExpr = columnExists("artwork", "stock") ? "stock" : "1";
        String sourceExpr = columnExists("artwork", "source") ? "source" : "1";
        String dailyViewExpr = columnExists("artwork", "daily_view_count") ? "daily_view_count" : "0";
        String dailyLikeExpr = columnExists("artwork", "daily_like_count") ? "daily_like_count" : "0";
        String saleCountExpr = columnExists("artwork", "sale_count") ? "sale_count" : "0";
        String createTimeExpr = columnExists("artwork", "create_time") ? "create_time" : "NOW()";
        String weightExpr = columnExists("artwork", "weight") ? "weight" : "0";
        String ownershipExpr = columnExists("artwork", "ownership_type") ? "ownership_type" : "1";

        String orderBy = buildFallbackArtworkOrderBy(query);
        int offset = Math.max((query.getPage() - 1) * query.getPageSize(), 0);
        List<Object> listArgs = new ArrayList<>(whereArgs);
        listArgs.add(query.getPageSize());
        listArgs.add(offset);

        String sql = """
                SELECT id,
                       title,
                       author_id,
                       %s AS author_uid,
                       author_name,
                       %s AS category_id,
                       %s AS art_type,
                       size,
                       year,
                       description,
                       %s AS cover_image,
                       price,
                       %s AS original_price,
                       %s AS stock,
                       status,
                       %s AS weight,
                       %s AS ownership_type,
                       %s AS artwork_code,
                       %s AS source,
                       view_count,
                       favorite_count,
                       %s AS daily_view_count,
                       %s AS daily_like_count,
                       %s AS sale_count,
                       %s AS create_time
                %s
                %s
                LIMIT ? OFFSET ?
                """.formatted(
                authorUidExpr,
                categoryExpr,
                artTypeExpr,
                coverExpr,
                originalPriceExpr,
                stockExpr,
                weightExpr,
                ownershipExpr,
                artworkCodeExpr,
                sourceExpr,
                dailyViewExpr,
                dailyLikeExpr,
                saleCountExpr,
                createTimeExpr,
                where,
                orderBy
        );

        List<ArtworkVO> records = jdbcTemplate.query(sql, (rs, rowNum) -> {
            Artwork artwork = new Artwork();
            artwork.setId(rs.getLong("id"));
            artwork.setTitle(rs.getString("title"));
            artwork.setAuthorId(getLongOrNull(rs, "author_id"));
            artwork.setAuthorUid(rs.getString("author_uid"));
            artwork.setAuthorName(rs.getString("author_name"));
            artwork.setCategoryId(getLongOrNull(rs, "category_id"));
            artwork.setArtType(rs.getString("art_type"));
            artwork.setSize(rs.getString("size"));
            artwork.setYear(getIntOrNull(rs, "year"));
            artwork.setDescription(rs.getString("description"));
            artwork.setCoverImage(rs.getString("cover_image"));
            artwork.setPrice(BigDecimal.valueOf(getLongOrDefault(rs, "price", 0L)));
            Long origPriceVal = getLongOrNull(rs, "original_price");
            artwork.setOriginalPrice(origPriceVal != null ? BigDecimal.valueOf(origPriceVal) : null);
            artwork.setStock(getIntOrNull(rs, "stock"));
            artwork.setStatus(getIntOrDefault(rs, "status", 1));
            artwork.setWeight(getIntOrDefault(rs, "weight", 0));
            artwork.setOwnershipType(getIntOrDefault(rs, "ownership_type", 1));
            artwork.setArtworkCode(rs.getString("artwork_code"));
            artwork.setSource(getIntOrDefault(rs, "source", 1));
            artwork.setViewCount(getIntOrDefault(rs, "view_count", 0));
            artwork.setFavoriteCount(getIntOrDefault(rs, "favorite_count", 0));
            artwork.setDailyViewCount(getIntOrDefault(rs, "daily_view_count", 0));
            artwork.setDailyLikeCount(getIntOrDefault(rs, "daily_like_count", 0));
            artwork.setSaleCount(getIntOrDefault(rs, "sale_count", 0));
            artwork.setCreateTime(rs.getTimestamp("create_time") != null
                    ? rs.getTimestamp("create_time").toLocalDateTime()
                    : null);
            return convertToListVO(artwork, userId);
        }, listArgs.toArray());

        return PageResult.of(total != null ? total : (long) records.size(), query.getPage(), query.getPageSize(), records);
    }

    private String buildFallbackArtworkOrderBy(ArtworkQueryDTO query) {
        if (query.getSort() != null) {
            return switch (query.getSort()) {
                case "price_asc" -> "ORDER BY price ASC, id DESC";
                case "price_desc" -> "ORDER BY price DESC, id DESC";
                case "time", "new" -> "ORDER BY create_time DESC, id DESC";
                default -> "ORDER BY weight DESC, create_time DESC, id DESC";
            };
        }
        if ("price".equals(query.getSortBy())) {
            return "asc".equalsIgnoreCase(query.getSortOrder())
                    ? "ORDER BY price ASC, id DESC"
                    : "ORDER BY price DESC, id DESC";
        }
        if ("saleCount".equals(query.getSortBy())) {
            return "ORDER BY sale_count DESC, id DESC";
        }
        return "ORDER BY weight DESC, create_time DESC, id DESC";
    }

    private List<Banner> getBannersFallback() {
        String typeColumn = columnExists("banner", "link_type") ? "link_type"
                : (columnExists("banner", "type") ? "type" : "NULL");
        String valueColumn = columnExists("banner", "link_value") ? "link_value"
                : (columnExists("banner", "target") ? "target" : "NULL");
        String sortColumn = columnExists("banner", "sort") ? "sort"
                : (columnExists("banner", "sort_no") ? "sort_no" : "0");
        String statusCondition = columnExists("banner", "status")
                ? " AND (status = 1 OR status = 'ENABLED')"
                : "";
        String startCondition = columnExists("banner", "start_time")
                ? " AND (start_time IS NULL OR start_time <= NOW())"
                : "";
        String endCondition = columnExists("banner", "end_time")
                ? " AND (end_time IS NULL OR end_time >= NOW())"
                : "";

        String sql = """
                SELECT id,
                       title,
                       image_url,
                       %s AS link_type,
                       %s AS link_value,
                       %s AS sort
                FROM banner
                WHERE 1=1%s%s%s
                ORDER BY sort ASC, id ASC
                """.formatted(typeColumn, valueColumn, sortColumn, statusCondition, startCondition, endCondition);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Banner banner = new Banner();
            banner.setId(rs.getLong("id"));
            banner.setTitle(rs.getString("title"));
            banner.setImageUrl(rs.getString("image_url"));
            banner.setLinkType(rs.getString("link_type"));
            banner.setLinkValue(rs.getString("link_value"));
            banner.setSort(getIntOrDefault(rs, "sort", 0));
            banner.setStatus(1);
            return banner;
        });
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """,
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private Map<String, Object> findActiveResaleListing(Long artworkId) {
        if (artworkId == null || !tableExists("resale_record")) {
            return null;
        }
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    """
                    SELECT id, artwork_id AS artworkId, seller_user_id AS sellerUserId,
                           buyer_user_id AS buyerUserId, resale_price AS resalePrice,
                           status, created_time AS createdTime, updated_time AS updatedTime
                    FROM resale_record
                    WHERE artwork_id = ? AND status = 'pending'
                    ORDER BY updated_time DESC, id DESC
                    LIMIT 1
                    """,
                    artworkId
            );
            return rows.isEmpty() ? null : rows.get(0);
        } catch (Exception e) {
            log.warn("查询作品转售挂单失败: artworkId={}", artworkId, e);
            return null;
        }
    }

    private Long getLongOrNull(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Long getLongOrDefault(java.sql.ResultSet rs, String column, Long defaultValue) throws java.sql.SQLException {
        Long value = getLongOrNull(rs, column);
        return value != null ? value : defaultValue;
    }

    private Integer getIntOrNull(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getIntOrDefault(java.sql.ResultSet rs, String column, Integer defaultValue) throws java.sql.SQLException {
        Integer value = getIntOrNull(rs, column);
        return value != null ? value : defaultValue;
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

        // ---- 内容级幂等校验：通过内容指纹检测是否已存在相同作品 ----
        String today = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String contentRaw = (dto.getTitle() != null ? dto.getTitle() : "") + "|" + authorId + "|" + today;
        String contentFingerprint = sha256(contentRaw);
        dto.setContentFingerprint(contentFingerprint);

        // 查最近 10 分钟内是否有相同指纹的作品
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        Artwork existing = artworkMapper.selectOne(new LambdaQueryWrapper<Artwork>()
                .eq(Artwork::getContentFingerprint, contentFingerprint)
                .ge(Artwork::getCreateTime, tenMinutesAgo)
                .last("LIMIT 1"));
        if (existing != null) {
            log.warn("内容级幂等拦截：相同作品已存在，返回已有ID={}, title={}", existing.getId(), dto.getTitle());
            return existing.getId();
        }

        Artwork artwork = new Artwork();
        artwork.setTitle(dto.getTitle());
        artwork.setAuthorId(authorId);
        artwork.setAuthorUid(dto.getAuthorUid()); // 设置作者UID
        artwork.setAuthorName(authorName);
        artwork.setContentFingerprint(contentFingerprint);
        artwork.setCategoryId(dto.getCategoryId());
        artwork.setCoverImage(dto.getCover() != null ? dto.getCover() : "https://picsum.photos/400/400");
        artwork.setImages(dto.getImages());
        artwork.setPrice(dto.getPrice() != null ? dto.getPrice() : BigDecimal.ZERO);
        artwork.setOriginalPrice(dto.getOriginalPrice() != null ? dto.getOriginalPrice() : null);
        artwork.setStock(dto.getStock() != null ? dto.getStock() : 1);
        artwork.setDescription(dto.getDescription());
        artwork.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        artwork.setWeight(dto.getWeight() != null ? dto.getWeight() : 0);
        artwork.setOwnershipType(dto.getOwnershipType() != null ? dto.getOwnershipType() : 1); // 默认原创
        artwork.setArtType(dto.getArtType());
        artwork.setMedium(dto.getMedium());
        artwork.setSize(dto.getSize());
        artwork.setYear(dto.getYear());
        artwork.setDailyViewCount(dto.getDailyViewCount() != null ? dto.getDailyViewCount() : 0);
        artwork.setDailyLikeCount(dto.getDailyLikeCount() != null ? dto.getDailyLikeCount() : 0);
        artwork.setDistributionEnabled(dto.getDistributionEnabled() != null ? dto.getDistributionEnabled() : false);
        artwork.setCommissionRate(dto.getCommissionRate() != null ? dto.getCommissionRate() : 10);
        artwork.setCreateTime(LocalDateTime.now());
        artwork.setUpdateTime(LocalDateTime.now());
        // 生成作品编号：画种缩写 + 日期 + 序号
        String artworkCode = generateArtworkCode(dto.getArtType(), dto.getCategoryId());
        artwork.setArtworkCode(artworkCode);
        // 设置 artwork_id（等于自增的 id）
        artworkMapper.insert(artwork);
        // 插入后更新 artwork_id = id
        if (artwork.getId() != null) {
            artwork.setArtworkId(artwork.getId());
            artworkMapper.updateById(artwork);
        }
        return artwork.getId();
    }

    /** 计算 SHA256 摘要 */
    private String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("计算SHA256失败", e);
            // 降级：使用字符串的 hashCode（不完美但至少避免 NPE）
            return Integer.toHexString(input.hashCode());
        }
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
        // 调试日志
        System.out.println("【DEBUG】updateArtwork 开始执行: id=" + dto.getId() 
            + ", price=" + dto.getPrice() 
            + ", originalPrice=" + dto.getOriginalPrice());
        
        Artwork artwork = artworkMapper.selectById(dto.getId());
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        
        // 打印当前数据库中的价格
        System.out.println("【DEBUG】updateArtwork 数据库当前值: artwork.price=" + artwork.getPrice() 
            + ", artwork.originalPrice=" + artwork.getOriginalPrice());
        if (dto.getTitle() != null) artwork.setTitle(dto.getTitle());
        
        // 记录原始艺术家名称，用于判断是否变化
        String originalAuthorName = artwork.getAuthorName();
        
        // 处理艺术家关联
        // 优先使用传入的 authorId（从搜索选择），只有当 authorId 为空时才查找或创建
        if (dto.getAuthorId() != null) {
            // 用户从下拉列表选择了艺术家，直接使用传入的 authorId
            artwork.setAuthorId(dto.getAuthorId());
            if (dto.getAuthorUid() != null) {
                artwork.setAuthorUid(dto.getAuthorUid()); // 设置作者UID
            }
            if (dto.getAuthorName() != null) {
                artwork.setAuthorName(dto.getAuthorName());
            }
        } else if (dto.getAuthorName() != null && !dto.getAuthorName().isEmpty()) {
            // 没有 authorId但有作者名称，需要查找或创建艺术家
            artwork.setAuthorName(dto.getAuthorName());
            if (!dto.getAuthorName().equals(originalAuthorName)) {
                Long authorId = findOrCreateArtist(dto.getAuthorName());
                if (authorId != null) {
                    artwork.setAuthorId(authorId);
                }
            }
        } else if (dto.getAuthorId() != null) {
            artwork.setAuthorId(dto.getAuthorId());
        }
        if (dto.getCategoryId() != null) artwork.setCategoryId(dto.getCategoryId());
        if (dto.getCover() != null) artwork.setCoverImage(dto.getCover());
        if (dto.getImages() != null) artwork.setImages(dto.getImages());
        if (dto.getPrice() != null) {
            artwork.setPrice(dto.getPrice());
        }
        if (dto.getOriginalPrice() != null) {
            artwork.setOriginalPrice(dto.getOriginalPrice());
        }
        if (dto.getStock() != null) artwork.setStock(dto.getStock());
        if (dto.getDescription() != null) artwork.setDescription(dto.getDescription());
        if (dto.getStatus() != null) artwork.setStatus(dto.getStatus());
        if (dto.getWeight() != null) artwork.setWeight(dto.getWeight());
        if (dto.getOwnershipType() != null) artwork.setOwnershipType(dto.getOwnershipType());
        if (dto.getArtType() != null) artwork.setArtType(dto.getArtType());
        if (dto.getMedium() != null) artwork.setMedium(dto.getMedium());
        if (dto.getSize() != null) artwork.setSize(dto.getSize());
        if (dto.getYear() != null) artwork.setYear(dto.getYear());
        if (dto.getDailyViewCount() != null) artwork.setDailyViewCount(Math.max(dto.getDailyViewCount(), 0));
        if (dto.getDailyLikeCount() != null) artwork.setDailyLikeCount(Math.max(dto.getDailyLikeCount(), 0));
        // 分销相关
        if (dto.getDistributionEnabled() != null) artwork.setDistributionEnabled(dto.getDistributionEnabled());
        if (dto.getCommissionRate() != null) artwork.setCommissionRate(dto.getCommissionRate());
        artwork.setUpdateTime(LocalDateTime.now());
        
        // 打印设置后的 artwork.price 值
        System.out.println("【DEBUG】updateArtwork 设置后的 artwork.price=" + artwork.getPrice() 
            + ", artwork.originalPrice=" + artwork.getOriginalPrice());
        
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

    /** 更新单个作品上下架状态 */
    @Transactional
    public void updateArtworkStatus(Long id, Integer status) {
        if (!ProductConstant.STATUS_ON_SALE.equals(status) && !ProductConstant.STATUS_OFF_SALE.equals(status)) {
            throw new BusinessException(ResultCode.PARAM_ERROR);
        }
        Artwork artwork = artworkMapper.selectById(id);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (ProductConstant.STATUS_SOLD_OUT.equals(artwork.getStatus())) {
            throw new BusinessException(ResultCode.PRODUCT_SOLD_OUT);
        }
        artwork.setStatus(status);
        artwork.setUpdateTime(LocalDateTime.now());
        artworkMapper.updateById(artwork);
    }

    /** 删除作品 */
    @Transactional
    public void deleteArtwork(Long id) {
        // 检查作品是否存在
        Artwork artwork = artworkMapper.selectById(id);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        
        // 先删除相关的收藏记录
        favoriteMapper.delete(
            new LambdaQueryWrapper<ArtworkFavorite>()
                .eq(ArtworkFavorite::getArtworkId, id)
        );
        
        // 删除作品
        int rows = artworkMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        
        log.info("作品删除成功: id={}, title={}", id, artwork.getTitle());
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
                .map(a -> convertToListVO(a, userId))
                .collect(Collectors.toList());

        long total = result.getTotal() > 0 ? result.getTotal() : voList.size();
        return PageResult.of(total, query.getPage(), query.getPageSize(), voList);
    }

    /** 转换实体为VO */
    private ArtworkVO convertToVO(Artwork artwork, Long userId) {
        loadArtworkPriceGrowthConfig(artwork);
        ArtworkVO vo = new ArtworkVO();
        vo.setId(artwork.getId());
        vo.setTitle(artwork.getTitle());
        vo.setAuthorId(artwork.getAuthorId());
        // 设置作者UID
        vo.setAuthorUid(artwork.getAuthorUid());
        vo.setDisplayAuthorId(artwork.getAuthorUid() != null ? artwork.getAuthorUid() : String.format("%04d", artwork.getAuthorId()));
        vo.setCategoryId(artwork.getCategoryId());
        vo.setCategoryName(artwork.getArtType());
        vo.setArtType(artwork.getArtType());
        vo.setMaterial(artwork.getMedium());
        vo.setSize(artwork.getSize());
        vo.setYear(artwork.getYear());
        vo.setEdition(artwork.getEdition());
        vo.setDescription(artwork.getDescription());
        // 优先使用 cover 字段，备选 coverImage
        String coverUrl = artwork.getCover() != null ? artwork.getCover() : artwork.getCoverImage();
        vo.setCoverImage(coverUrl);
        if (artwork.getImages() != null) {
            vo.setImages(Arrays.asList(artwork.getImages().split(",")));
        }
        vo.setSource(artwork.getSource());
        vo.setPrice(artwork.getPrice());
        vo.setOriginalPrice(artwork.getOriginalPrice());
        // 实时计算当前价格（包含最新浏览量、收藏量等因素）
        Long currentPrice = priceGrowthService.calculateCurrentPrice(artwork);
        vo.setCurrentPrice(currentPrice != null ? BigDecimal.valueOf(currentPrice) : null);
        vo.setStock(artwork.getStock());
        vo.setStatus(artwork.getStatus());
        vo.setHolderId(artwork.getHolderId());
        if (artwork.getHolderSince() != null) {
            vo.setHolderSince(artwork.getHolderSince().toString());
        }
        if (artwork.getHolderId() != null) {
            vo.setHolderName(resolveUserNickname(artwork.getHolderId()));
        }
        vo.setResaleListing(findActiveResaleListing(artwork.getId()));
        vo.setWeight(artwork.getWeight() != null ? artwork.getWeight() : 0);
        vo.setOwnershipType(artwork.getOwnershipType() != null ? artwork.getOwnershipType() : 1);
        vo.setArtworkCode(artwork.getArtworkUid() != null ? artwork.getArtworkUid() : artwork.getArtworkCode());
        // 作品类型文本
        vo.setOwnershipTypeText(switch (artwork.getOwnershipType()) {
            case 1 -> "原创";
            case 2 -> "收藏";
            default -> "原创";
        });
        vo.setPriceRise(priceGrowthService.calculatePriceRise(artwork));
        applyHeatCounts(vo, artwork);
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
                || (vo.getDisplayLikeCount() != null && vo.getDisplayLikeCount() > 5);
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
            try {
                ArtworkFavorite fav = favoriteMapper.selectOne(
                        new LambdaQueryWrapper<ArtworkFavorite>()
                                .eq(ArtworkFavorite::getUserId, userId)
                                .eq(ArtworkFavorite::getArtworkId, artwork.getId())
                );
                vo.setIsFavorited(fav != null);
            } catch (Exception e) {
                log.warn("检查作品收藏状态失败，使用默认未收藏: artworkId={}, userId={}",
                        artwork.getId(), userId, e);
                vo.setIsFavorited(false);
            }
        } else {
            vo.setIsFavorited(false);
        }

        // 获取艺术家详细信息（打通关联）
        // 如果author_id为1（测试用户），但authorName匹配真实艺术家，则查询正确的艺术家ID
        Long authorId = artwork.getAuthorId();
        String authorName = artwork.getAuthorName();
        if (authorId != null && authorId == 1L && authorName != null && !authorName.isEmpty()) {
            // 查询正确的艺术家ID
            Map<String, Object> artistData = getArtistByName(authorName);
            if (artistData != null && artistData.get("id") != null) {
                Long correctAuthorId = ((Number) artistData.get("id")).longValue();
                if (correctAuthorId != 1L) {
                    log.info("修正作品author_id: artworkId={}, 错误ID={}, 正确ID={}, authorName={}", 
                             artwork.getId(), authorId, correctAuthorId, authorName);
                    authorId = correctAuthorId;
                }
            }
        }
        
        ArtistInfoVO artistInfo = getArtistInfo(authorId);
        
        // 设置修正后的authorId到VO中
        vo.setAuthorId(authorId);
        
        // 格式化ID为4位数显示
        vo.setDisplayArtworkId(String.format("%04d", artwork.getId()));
        vo.setDisplayAuthorId(String.format("%04d", authorId));

        if (artistInfo != null) {
            // 使用艺术家表中的真实信息
            vo.setAuthorName(artistInfo.getNickname() != null ? artistInfo.getNickname() : artwork.getAuthorName());
            vo.setAuthorAvatar(artistInfo.getAvatar() != null ? artistInfo.getAvatar() : artwork.getAuthorAvatar());
            vo.setAuthorBadge(artistInfo.getBadge() != null ? artistInfo.getBadge() : artwork.getAuthorBadge());
            vo.setAuthorBio(artistInfo.getBio() != null ? artistInfo.getBio() : artwork.getAuthorBio());
            vo.setAuthorPhone(artistInfo.getPhone() != null ? artistInfo.getPhone() : artwork.getAuthorPhone());
            vo.setAuthorIdentity(artistInfo.getIdentityType() != null ? artistInfo.getIdentityType() : getAuthorIdentity(artwork.getAuthorBadge()));
            // 设置艺术家UID，优先使用artistCode，否则使用uid
            String artistUid = artistInfo.getArtistCode() != null ? artistInfo.getArtistCode() : artistInfo.getUid();
            vo.setAuthorUid(artistUid);
        } else {
            // 回退到作品表中的冗余信息
            vo.setAuthorName(artwork.getAuthorName());
            vo.setAuthorBadge(artwork.getAuthorBadge());
            vo.setAuthorAvatar(artwork.getAuthorAvatar());
            vo.setAuthorBio(artwork.getAuthorBio());
            vo.setAuthorPhone(artwork.getAuthorPhone());
            vo.setAuthorIdentity(getAuthorIdentity(artwork.getAuthorBadge()));
            // 回退时生成一个基于authorId的UID格式
            if (artwork.getAuthorId() != null) {
                vo.setAuthorUid("USR" + String.format("%012d", artwork.getAuthorId()));
            }
        }

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

        // 单个作品价格增长配置
        vo.setCustomPriceGrowthEnabled(artwork.getCustomPriceGrowthEnabled());
        vo.setCustomBaseDailyRate(artwork.getCustomBaseDailyRate());
        vo.setCustomMatureDailyRate(artwork.getCustomMatureDailyRate());
        vo.setCustomMatureDays(artwork.getCustomMatureDays());
        vo.setCustomViewRate(artwork.getCustomViewRate());
        vo.setCustomFavoriteRate(artwork.getCustomFavoriteRate());
        vo.setCustomMaxGrowthMultiple(artwork.getCustomMaxGrowthMultiple());
        vo.setTomorrowIncreaseMin(BigDecimal.valueOf(priceGrowthService.calculateTomorrowIncreaseMin(artwork)));
        vo.setTomorrowIncreaseMax(BigDecimal.valueOf(priceGrowthService.calculateTomorrowIncreaseMax(artwork)));

        return vo;
    }

    private String resolveUserNickname(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT nickname FROM users WHERE id = ? AND deleted = 0 LIMIT 1",
                    String.class,
                    userId);
        } catch (Exception e) {
            log.warn("查询作品持有人昵称失败: userId={}", userId, e);
            return null;
        }
    }

    /** 转换为列表VO：首页瀑布流不阻塞等待用户服务补艺术家资料 */
    private ArtworkVO convertToListVO(Artwork artwork, Long userId) {
        ArtworkVO vo = convertToSimpleVO(artwork);
        vo.setDisplayArtworkId(String.format("%04d", artwork.getId()));
        if (artwork.getAuthorId() != null) {
            vo.setDisplayAuthorId(artwork.getAuthorUid() != null
                    ? artwork.getAuthorUid()
                    : String.format("%04d", artwork.getAuthorId()));
            vo.setAuthorUid(artwork.getAuthorUid() != null
                    ? artwork.getAuthorUid()
                    : "USR" + String.format("%012d", artwork.getAuthorId()));
        }
        vo.setAuthorAvatar(artwork.getAuthorAvatar());
        vo.setAuthorBadge(artwork.getAuthorBadge());
        vo.setAuthorBio(artwork.getAuthorBio());
        vo.setAuthorPhone(artwork.getAuthorPhone());
        vo.setAuthorIdentity(getAuthorIdentity(artwork.getAuthorBadge()));
        vo.setSource(artwork.getSource());
        vo.setSourceText(switch (artwork.getSource()) {
            case 1 -> "艺术家发布";
            case 2 -> "经纪人代理";
            case 3 -> "持有者转售";
            case 4 -> "平台自营";
            default -> "未知";
        });
        vo.setOwnershipType(artwork.getOwnershipType() != null ? artwork.getOwnershipType() : 1);
        vo.setOwnershipTypeText(switch (artwork.getOwnershipType()) {
            case 1 -> "原创";
            case 2 -> "收藏";
            default -> "原创";
        });
        vo.setStatusText(switch (artwork.getStatus()) {
            case 0 -> "已下架";
            case 1 -> "上架中";
            case 2 -> "已售罄";
            default -> "未知";
        });
        vo.setPriceRise(priceGrowthService.calculatePriceRise(artwork));
        vo.setCurrentPrice(BigDecimal.valueOf(priceGrowthService.calculateCurrentPrice(artwork)));
        vo.setIsNew(artwork.getCreateTime() != null
                && artwork.getCreateTime().isAfter(LocalDateTime.now().minusDays(30)));
        vo.setIsHot((artwork.getSaleCount() != null && artwork.getSaleCount() > 0)
                || (vo.getDisplayLikeCount() != null && vo.getDisplayLikeCount() > 5));
        vo.setIsFavorited(false);
        if (userId != null) {
            try {
                ArtworkFavorite fav = favoriteMapper.selectOne(
                        new LambdaQueryWrapper<ArtworkFavorite>()
                                .eq(ArtworkFavorite::getUserId, userId)
                                .eq(ArtworkFavorite::getArtworkId, artwork.getId())
                );
                vo.setIsFavorited(fav != null);
            } catch (Exception e) {
                log.warn("检查作品收藏状态失败，使用默认未收藏: artworkId={}, userId={}",
                        artwork.getId(), userId, e);
            }
        }
        vo.setIsFollowing(false);
        if (artwork.getCreateTime() != null) {
            long daysSinceCreation = java.time.Duration.between(
                    artwork.getCreateTime(), LocalDateTime.now()).toDays();
            vo.setHoldDuration((int) daysSinceCreation);
        }
        vo.setDistributionEnabled(artwork.getDistributionEnabled());
        vo.setCommissionRate(artwork.getCommissionRate());
        vo.setDistributionOrders(artwork.getDistributionOrders());
        vo.setDistributionEarnings(artwork.getDistributionEarnings());
        vo.setDistributionUsers(artwork.getDistributionUsers());
        vo.setResaleListing(findActiveResaleListing(artwork.getId()));
        vo.setCustomPriceGrowthEnabled(artwork.getCustomPriceGrowthEnabled());
        vo.setCustomBaseDailyRate(artwork.getCustomBaseDailyRate());
        vo.setCustomMatureDailyRate(artwork.getCustomMatureDailyRate());
        vo.setCustomMatureDays(artwork.getCustomMatureDays());
        vo.setCustomViewRate(artwork.getCustomViewRate());
        vo.setCustomFavoriteRate(artwork.getCustomFavoriteRate());
        vo.setCustomMaxGrowthMultiple(artwork.getCustomMaxGrowthMultiple());
        vo.setTomorrowIncreaseMin(BigDecimal.valueOf(priceGrowthService.calculateTomorrowIncreaseMin(artwork)));
        vo.setTomorrowIncreaseMax(BigDecimal.valueOf(priceGrowthService.calculateTomorrowIncreaseMax(artwork)));
        return vo;
    }

    private void applyHeatCounts(ArtworkVO vo, Artwork artwork) {
        int realViewCount = artwork.getViewCount() != null ? artwork.getViewCount() : 0;
        int realFavoriteCount = artwork.getFavoriteCount() != null ? artwork.getFavoriteCount() : 0;
        int dailyViewCount = artwork.getDailyViewCount() != null ? artwork.getDailyViewCount() : 0;
        int dailyLikeCount = artwork.getDailyLikeCount() != null ? artwork.getDailyLikeCount() : 0;
        int displayViewCount = priceGrowthService.calculateDisplayViewCount(artwork);
        int displayLikeCount = priceGrowthService.calculateDisplayLikeCount(artwork);

        vo.setRealViewCount(realViewCount);
        vo.setRealFavoriteCount(realFavoriteCount);
        vo.setDailyViewCount(dailyViewCount);
        vo.setDailyLikeCount(dailyLikeCount);
        vo.setDisplayViewCount(displayViewCount);
        vo.setDisplayLikeCount(displayLikeCount);
        vo.setViewCount(displayViewCount);
        vo.setFavoriteCount(displayLikeCount);
        vo.setLikeCount(displayLikeCount);
    }

    /**
     * 根据艺术家名称查询艺术家信息
     */
    private Map<String, Object> getArtistByName(String artistName) {
        if (artistName == null || artistName.trim().isEmpty()) {
            return null;
        }
        try {
            // 调用 user 服务的艺术家查询接口
            String url = "http://localhost:8081/artist/by-name?name=" + java.net.URLEncoder.encode(artistName.trim(), "UTF-8");
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (isSuccessResponse(response)) {
                return (Map<String, Object>) response.get("data");
            }
            log.warn("获取艺术家信息失败: artistName={}, response={}", artistName, response);
        } catch (Exception e) {
            log.error("调用艺术家查询接口失败: artistName={}, error={}", artistName, e.getMessage());
        }
        return null;
    }

    /**
     * 获取单个作品价格增长配置
     */
    public Map<String, Object> getArtworkPriceGrowth(Long artworkId) {
        ensureArtworkPriceGrowthColumns();
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        loadArtworkPriceGrowthConfig(artwork);
        
        Map<String, Object> result = new HashMap<>();
        result.put("artworkId", artwork.getId());
        result.put("artworkTitle", artwork.getTitle());
        result.put("customPriceGrowthEnabled", artwork.getCustomPriceGrowthEnabled() != null ? artwork.getCustomPriceGrowthEnabled() : false);
        result.put("customBaseDailyRate", artwork.getCustomBaseDailyRate() != null ? artwork.getCustomBaseDailyRate() : new BigDecimal("0.0002"));
        result.put("customMatureDailyRate", artwork.getCustomMatureDailyRate() != null ? artwork.getCustomMatureDailyRate() : new BigDecimal("0.0003"));
        result.put("customMatureDays", artwork.getCustomMatureDays() != null && artwork.getCustomMatureDays() > 0 ? artwork.getCustomMatureDays() : 30);
        result.put("customViewRate", artwork.getCustomViewRate() != null ? artwork.getCustomViewRate() : new BigDecimal("1.1"));
        result.put("customFavoriteRate", artwork.getCustomFavoriteRate() != null ? artwork.getCustomFavoriteRate() : new BigDecimal("1.1"));
        result.put("customMaxGrowthMultiple", artwork.getCustomMaxGrowthMultiple() != null ? artwork.getCustomMaxGrowthMultiple() : new BigDecimal("5.0"));
        result.put("dailyViewCount", artwork.getDailyViewCount() != null ? artwork.getDailyViewCount() : 0);
        result.put("dailyLikeCount", artwork.getDailyLikeCount() != null ? artwork.getDailyLikeCount() : 0);
        result.put("displayViewCount", priceGrowthService.calculateDisplayViewCount(artwork));
        result.put("displayLikeCount", priceGrowthService.calculateDisplayLikeCount(artwork));
        
        return result;
    }

    public Long calculateDisplayCurrentPrice(Long artworkId) {
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            return 0L;
        }
        loadArtworkPriceGrowthConfig(artwork);
        return priceGrowthService.calculateCurrentPrice(artwork);
    }

    /**
     * 更新单个作品价格增长配置
     */
    @Transactional
    public void updateArtworkPriceGrowth(Long artworkId, Map<String, Object> config) {
        ensureArtworkPriceGrowthColumns();
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_FOUND);
        }
        
        if (config.get("customPriceGrowthEnabled") != null) {
            artwork.setCustomPriceGrowthEnabled((Boolean) config.get("customPriceGrowthEnabled"));
        }
        if (config.get("customBaseDailyRate") != null) {
            artwork.setCustomBaseDailyRate(new BigDecimal(config.get("customBaseDailyRate").toString()));
        }
        if (config.get("customMatureDailyRate") != null) {
            artwork.setCustomMatureDailyRate(new BigDecimal(config.get("customMatureDailyRate").toString()));
        }
        if (config.get("customMatureDays") != null) {
            artwork.setCustomMatureDays(Integer.parseInt(config.get("customMatureDays").toString()));
        }
        if (config.get("customViewRate") != null) {
            artwork.setCustomViewRate(new BigDecimal(config.get("customViewRate").toString()));
        }
        if (config.get("customFavoriteRate") != null) {
            artwork.setCustomFavoriteRate(new BigDecimal(config.get("customFavoriteRate").toString()));
        }
        if (config.get("customMaxGrowthMultiple") != null) {
            artwork.setCustomMaxGrowthMultiple(new BigDecimal(config.get("customMaxGrowthMultiple").toString()));
        }
        if (config.get("dailyViewCount") != null) {
            artwork.setDailyViewCount(Math.max(Integer.parseInt(config.get("dailyViewCount").toString()), 0));
        }
        if (config.get("dailyLikeCount") != null) {
            artwork.setDailyLikeCount(Math.max(Integer.parseInt(config.get("dailyLikeCount").toString()), 0));
        }
        
        List<String> assignments = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (config.get("customPriceGrowthEnabled") != null) {
            assignments.add("custom_price_growth_enabled = ?");
            args.add(Boolean.TRUE.equals(config.get("customPriceGrowthEnabled")) ? 1 : 0);
        }
        if (config.get("customBaseDailyRate") != null) {
            assignments.add("custom_base_daily_rate = ?");
            args.add(new BigDecimal(config.get("customBaseDailyRate").toString()));
        }
        if (config.get("customMatureDailyRate") != null) {
            assignments.add("custom_mature_daily_rate = ?");
            args.add(new BigDecimal(config.get("customMatureDailyRate").toString()));
        }
        if (config.get("customMatureDays") != null) {
            assignments.add("custom_mature_days = ?");
            args.add(Integer.parseInt(config.get("customMatureDays").toString()));
        }
        if (config.get("customViewRate") != null) {
            assignments.add("custom_view_rate = ?");
            args.add(new BigDecimal(config.get("customViewRate").toString()));
        }
        if (config.get("customFavoriteRate") != null) {
            assignments.add("custom_favorite_rate = ?");
            args.add(new BigDecimal(config.get("customFavoriteRate").toString()));
        }
        if (config.get("customMaxGrowthMultiple") != null) {
            assignments.add("custom_max_growth_multiple = ?");
            args.add(new BigDecimal(config.get("customMaxGrowthMultiple").toString()));
        }
        if (config.get("dailyViewCount") != null) {
            assignments.add("daily_view_count = ?");
            args.add(Math.max(Integer.parseInt(config.get("dailyViewCount").toString()), 0));
        }
        if (config.get("dailyLikeCount") != null) {
            assignments.add("daily_like_count = ?");
            args.add(Math.max(Integer.parseInt(config.get("dailyLikeCount").toString()), 0));
        }
        if (columnExists("artwork", "update_time")) {
            assignments.add("update_time = ?");
            args.add(LocalDateTime.now());
        }
        if (!assignments.isEmpty()) {
            String sql = "UPDATE artwork SET " + String.join(", ", assignments) + " WHERE id = ?";
            args.add(artworkId);
            jdbcTemplate.update(sql, args.toArray());
        }
        
        // 重新计算价格
        priceGrowthService.updateSinglePrice(artworkId);
    }

    private void ensureArtworkPriceGrowthColumns() {
        addColumnIfMissing("artwork", "custom_price_growth_enabled", "TINYINT(1) DEFAULT 0 COMMENT '是否启用单作品自定义涨价配置'");
        addColumnIfMissing("artwork", "custom_base_daily_rate", "DECIMAL(10,6) DEFAULT NULL COMMENT '自定义基础日增长率'");
        addColumnIfMissing("artwork", "custom_mature_daily_rate", "DECIMAL(10,6) DEFAULT NULL COMMENT '自定义成熟期日增长率'");
        addColumnIfMissing("artwork", "custom_mature_days", "INT DEFAULT NULL COMMENT '自定义成熟期天数'");
        addColumnIfMissing("artwork", "custom_view_rate", "DECIMAL(10,4) DEFAULT NULL COMMENT '自定义浏览量加成系数'");
        addColumnIfMissing("artwork", "custom_favorite_rate", "DECIMAL(10,4) DEFAULT NULL COMMENT '自定义收藏量加成系数'");
        addColumnIfMissing("artwork", "custom_max_growth_multiple", "DECIMAL(10,2) DEFAULT NULL COMMENT '自定义最大涨幅倍数'");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private void loadArtworkPriceGrowthConfig(Artwork artwork) {
        if (artwork == null || !columnExists("artwork", "custom_price_growth_enabled")) {
            return;
        }
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap("""
                    SELECT custom_price_growth_enabled,
                           custom_base_daily_rate,
                           custom_mature_daily_rate,
                           custom_mature_days,
                           custom_view_rate,
                           custom_favorite_rate,
                           custom_max_growth_multiple
                    FROM artwork
                    WHERE id = ?
                    """, artwork.getId());
            artwork.setCustomPriceGrowthEnabled(toInt(row.get("custom_price_growth_enabled"), 0) == 1);
            artwork.setCustomBaseDailyRate(toBigDecimal(row.get("custom_base_daily_rate")));
            artwork.setCustomMatureDailyRate(toBigDecimal(row.get("custom_mature_daily_rate")));
            Integer matureDays = row.get("custom_mature_days") != null ? toInt(row.get("custom_mature_days"), 30) : null;
            artwork.setCustomMatureDays(matureDays != null && matureDays > 0 ? matureDays : null);
            artwork.setCustomViewRate(toBigDecimal(row.get("custom_view_rate")));
            artwork.setCustomFavoriteRate(toBigDecimal(row.get("custom_favorite_rate")));
            artwork.setCustomMaxGrowthMultiple(toBigDecimal(row.get("custom_max_growth_multiple")));
        } catch (Exception e) {
            log.warn("加载作品价格增长配置失败: artworkId={}, error={}", artwork.getId(), e.getMessage());
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取艺术家详细信息（打通作品与艺术家关联）
     */
    private ArtistInfoVO getArtistInfo(Long authorId) {
        if (authorId == null) {
            return null;
        }
        try {
            // 调用 user 服务的艺术家详情接口
            String url = "http://localhost:8081/user/artist/info/" + authorId;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (isSuccessResponse(response)) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    return mapToArtistInfoVO(data);
                }
            }
            log.warn("获取艺术家信息返回异常: authorId={}, response={}", authorId, response);
        } catch (Exception e) {
            log.error("调用艺术家详情接口失败: authorId={}, error={}", authorId, e.getMessage());
        }
        return null;
    }

    private boolean isSuccessResponse(Map<String, Object> response) {
        if (response == null || response.get("code") == null) {
            return false;
        }
        int code = ((Number) response.get("code")).intValue();
        return code == 0 || code == 200;
    }

    /**
     * 将Map转换为ArtistInfoVO
     */
    private ArtistInfoVO mapToArtistInfoVO(Map<String, Object> data) {
        ArtistInfoVO vo = new ArtistInfoVO();
        vo.setUserId(data.get("userId") != null ? ((Number) data.get("userId")).longValue() : null);
        vo.setUid((String) data.get("uid"));
        vo.setArtistCode((String) data.get("artistCode"));
        vo.setNickname((String) data.get("nickname"));
        vo.setRealName((String) data.get("realName"));
        vo.setAvatar((String) data.get("avatar"));
        vo.setPhone((String) data.get("phone"));
        vo.setBio((String) data.get("bio"));
        vo.setResume((String) data.get("resume"));
        vo.setRegion((String) data.get("region"));
        vo.setCertStatus(data.get("certStatus") != null ? ((Number) data.get("certStatus")).intValue() : null);
        vo.setIsArtist((Boolean) data.get("isArtist"));
        vo.setIdentityType((String) data.get("identityType"));
        vo.setFollowerCount(data.get("followerCount") != null ? ((Number) data.get("followerCount")).intValue() : 0);
        vo.setArtworkCount(data.get("artworkCount") != null ? ((Number) data.get("artworkCount")).intValue() : 0);
        vo.setBadge((String) data.get("badge"));

        // 处理列表字段
        if (data.get("identities") instanceof List) {
            vo.setIdentities((List<String>) data.get("identities"));
        }
        if (data.get("exhibits") instanceof List) {
            vo.setExhibits((List<String>) data.get("exhibits"));
        }
        if (data.get("artworks") instanceof List) {
            vo.setArtworks((List<String>) data.get("artworks"));
        }

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
            // 调用 user 服务的 API，使用 UriComponentsBuilder 正确编码参数
            String baseUrl = "http://localhost:8081/user/artist/find-or-create";
            URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("name", artistName.trim())
                    .build()
                    .toUri();
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response != null && response.get("code") != null && ((Number) response.get("code")).intValue() == 200) {
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
