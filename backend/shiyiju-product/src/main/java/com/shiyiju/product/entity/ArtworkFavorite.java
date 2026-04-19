package com.shiyiju.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/** 作品收藏表 */
@Data
@TableName("artwork_favorites")
public class ArtworkFavorite implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long artworkId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
