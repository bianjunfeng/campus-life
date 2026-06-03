package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.modules.forum.entity.Comment;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.entity.PostCategory;
import com.campus.campus_life_backend.modules.forum.vo.PostTrendVO;
import com.campus.campus_life_backend.modules.forum.mapper.CommentMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostCategoryMapper;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import com.campus.campus_life_backend.modules.forum.mapper.ReportMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminCommunityQueryService {

    private final PostMapper postMapper;
    private final PostCategoryMapper postCategoryMapper;
    private final CommentMapper commentMapper;
    private final ReportMapper reportMapper;

    public AdminCommunityQueryService(
            PostMapper postMapper,
            PostCategoryMapper postCategoryMapper,
            CommentMapper commentMapper,
            ReportMapper reportMapper) {
        this.postMapper = postMapper;
        this.postCategoryMapper = postCategoryMapper;
        this.commentMapper = commentMapper;
        this.reportMapper = reportMapper;
    }

    public Map<String, Object> getPostList(Integer page, Integer size, Integer status, String keyword, Long userId) {
        String keywordLower = toLower(keyword);
        List<Post> filtered = findAllPostsForAdmin().stream()
                .filter(p -> status == null || Objects.equals(p.getStatus(), status))
                .filter(p -> userId == null || Objects.equals(p.getUserId(), userId))
                .filter(p -> isBlank(keyword)
                        || containsIgnoreCase(p.getTitle(), keywordLower)
                        || containsIgnoreCase(p.getContent(), keywordLower))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(filtered, page, size));
        result.put("page", normalizePage(page));
        result.put("size", normalizeSize(size));
        result.put("total", filtered.size());
        return result;
    }

    public Map<String, Object> getCommentList(Integer page, Integer size, Integer status,
                                              Long postId, Long userId, String keyword) {
        String keywordLower = toLower(keyword);
        Long keywordId = tryParseLong(keyword);
        List<Comment> filtered = findAllCommentsForAdmin().stream()
                .filter(c -> status == null || Objects.equals(c.getStatus(), status))
                .filter(c -> postId == null || Objects.equals(c.getPostId(), postId))
                .filter(c -> userId == null || Objects.equals(c.getUserId(), userId))
                .filter(c -> isBlank(keyword)
                        || Objects.equals(c.getId(), keywordId)
                        || containsIgnoreCase(c.getContent(), keywordLower))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("list", paginate(filtered, page, size));
        result.put("page", normalizePage(page));
        result.put("size", normalizeSize(size));
        result.put("total", filtered.size());
        return result;
    }

    public Map<String, Object> getReportList(Integer page, Integer size, Integer status,
                                             Integer targetType, String keyword) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int offset = (p - 1) * s;

        List<Map<String, Object>> list = reportMapper.findAdminList(status, targetType, keyword, offset, s);
        Long total = reportMapper.countAdminList(status, targetType, keyword);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list == null ? List.of() : list);
        result.put("page", p);
        result.put("size", s);
        result.put("total", total == null ? 0 : total);
        return result;
    }

    public List<Post> findAllPostsForAdmin() {
        List<Post> posts = postMapper.findAllForAdmin();
        return posts == null ? List.of() : posts;
    }

    public List<Comment> findAllCommentsForAdmin() {
        List<Comment> comments = commentMapper.findAllForAdmin();
        return comments == null ? List.of() : comments;
    }

    public List<PostCategory> findActiveCategoriesForAdmin() {
        List<PostCategory> categories = postCategoryMapper.findAllActive();
        return categories == null ? List.of() : categories;
    }

    public long countPosts() {
        return postMapper.countAllForAdmin();
    }

    public long countPostsByStatus(Integer status) {
        return postMapper.countByStatusForAdmin(status);
    }

    public long countPostsSince(java.time.LocalDateTime since) {
        return postMapper.countPostsSince(since);
    }

    public List<PostTrendVO> countPostsTrend(int days) {
        java.time.LocalDateTime end = java.time.LocalDateTime.now();
        java.time.LocalDateTime start = end.minusDays(Math.max(days, 1) - 1L).toLocalDate().atStartOfDay();
        List<Map<String, Object>> rows = postMapper.countPostsTrend(start, end);
        return (rows == null ? List.<Map<String, Object>>of() : rows).stream()
                .map(row -> new PostTrendVO(String.valueOf(row.get("day")), toLong(row.get("value"))))
                .toList();
    }

    public long countComments() {
        return commentMapper.countAllForAdmin();
    }

    public long countCommentsByStatus(Integer status) {
        return commentMapper.countByStatusForAdmin(status);
    }

    public long countPendingReports() {
        return reportMapper.countByStatus(0);
    }

    public List<Map<String, Object>> countContributionByMonthWeek(java.time.LocalDateTime startTime) {
        List<Map<String, Object>> rows = postMapper.countContributionByMonthWeek(startTime);
        return rows == null ? List.of() : rows;
    }

    public List<Map<String, Object>> countContentTypeData() {
        List<Map<String, Object>> rows = postMapper.countByCategoryForAdmin();
        return rows == null ? List.of() : rows;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        return size == null || size < 1 ? 20 : size;
    }

    private <T> List<T> paginate(List<T> source, Integer page, Integer size) {
        int p = normalizePage(page);
        int s = normalizeSize(size);
        int from = (p - 1) * s;
        if (from >= source.size()) {
            return List.of();
        }
        int to = Math.min(from + s, source.size());
        return new ArrayList<>(source.subList(from, to));
    }

    private boolean containsIgnoreCase(String value, String expectedLower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(expectedLower);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String toLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Long tryParseLong(String value) {
        if (isBlank(value)) return null;
        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return 0L;
        }
    }
}
