package com.campus.campus_life_backend.modules.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 帖子DTO，包含帖子信息和用户信息
 */
@Data
public class PostDTO {
    private Long id;
    private Long userId;
    private Long categoryId;
    private String title;
    private String content;
    private Integer status;
    private Integer isPinned;
    private Integer isHot;
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private LocalDateTime lastCommentTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 用户信息
    private String authorName;  // 用户名
    private String authorAvatar; // 用户头像
    private Long authorId;       // 作者ID
    
    // 分类信息（可选）
    private String categoryName; // 分类名称
    private String categoryCode; // 分类代码
    
    // 封面图片（取第一张图片）
    private String coverImage; // 封面图片URL
    
    // 图片列表（用于详情页）
    private java.util.List<PostImageDTO> images;
    
    // 当前登录用户是否已关注作者（需要传入当前用户ID）
    private Boolean isFollowed; // 是否已关注作者
    
    // 当前登录用户是否已点赞此帖子（需要传入当前用户ID）
    private Boolean isLiked; // 是否已点赞

    // 当前登录用户是否已收藏此帖子（需要传入当前用户ID）
    private Boolean isCollected; // 是否已收藏
    
    @Data
    public static class PostImageDTO {
        private Long id;
        private String url;
        private Integer width;
        private Integer height;
    }
}


