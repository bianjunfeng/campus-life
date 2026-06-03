package com.campus.campus_life_backend.modules.forum.mapper;

import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.search.dto.SearchSyncCandidateDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;


public interface PostMapper {

    int insertPost(Post post);

    Post findById(@Param("id") Long id);

    List<Post> findByUserId(@Param("userId") Long userId);

    // 分页查询用户帖子
    List<Post> findByUserIdWithPagination(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    // 分页查询用户帖子（带用户信息）
    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findByUserIdWithUserAndPagination(@Param("userId") Long userId, @Param("offset") Integer offset, @Param("limit") Integer limit);

    List<Post> findByCategoryId(@Param("categoryId") Long categoryId);

    List<Post> findHotPosts(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<Post> findLatestPosts(@Param("offset") Integer offset, @Param("limit") Integer limit);

    // 管理后台：查询所有帖子（包含非正常状态）
    List<Post> findAllForAdmin();

    // 带用户信息的查询
    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findHotPostsWithUser(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findLatestPostsWithUser(@Param("offset") Integer offset, @Param("limit") Integer limit);

    // 我关注的用户最新帖子流
    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findFollowPostsWithUser(
            @Param("currentUserId") Long currentUserId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    long countFollowPostsWithUser(@Param("currentUserId") Long currentUserId);

    // 按分类ID查询帖子（带用户信息）
    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findByCategoryIdWithUser(
            @Param("categoryId") Long categoryId,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );
    long countByCategoryId(@Param("categoryId") Long categoryId);
    long countHotPosts();
    long countLatestPosts();

    // 根据ID查询帖子详情（带用户信息）
    com.campus.campus_life_backend.modules.forum.dto.PostDTO findByIdWithUser(@Param("id") Long id);

    int updatePost(Post post);

    int deletePost(@Param("id") Long id);

    int incrementViewCount(@Param("id") Long id);

    int incrementLikeCount(@Param("id") Long id);

    int decrementLikeCount(@Param("id") Long id);

    int incrementCommentCount(@Param("id") Long id);

    int decrementCommentCount(@Param("id") Long id);

    int incrementFavoriteCount(@Param("id") Long id);

    int decrementFavoriteCount(@Param("id") Long id);

    // 统计用户发布的帖子数量
    int countByUserId(@Param("userId") Long userId);

    // 根据ID列表查询帖子（带用户信息）
    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findByIdsWithUser(@Param("ids") List<Long> ids);

    // 搜索兜底：MySQL 模糊查询帖子（带用户信息）
    List<com.campus.campus_life_backend.modules.forum.dto.PostDTO> findSearchPostsWithUser(
            @Param("keyword") String keyword,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    // 搜索兜底：MySQL 模糊查询总数
    long countSearchPosts(@Param("keyword") String keyword);

    // ES 全量重建：分页获取可索引帖子ID
    List<Long> findIndexablePostIds(@Param("offset") Integer offset, @Param("limit") Integer limit);

    List<SearchSyncCandidateDTO> findUpdatedPostCandidates(
            @Param("since") LocalDateTime since,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    long countAllForAdmin();

    long countByStatusForAdmin(@Param("status") Integer status);

    long countPostsSince(@Param("since") LocalDateTime since);

    List<java.util.Map<String, Object>> countContributionByMonthWeek(@Param("startTime") LocalDateTime startTime);

    List<java.util.Map<String, Object>> countByCategoryForAdmin();

    List<java.util.Map<String, Object>> countPostsTrend(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
