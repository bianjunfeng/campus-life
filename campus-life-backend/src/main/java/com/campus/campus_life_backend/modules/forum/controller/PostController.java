package com.campus.campus_life_backend.modules.forum.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.exception.BusinessErrorCode;
import com.campus.campus_life_backend.common.exception.BusinessException;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.annotation.RequireOwnerOrPermission;
import com.campus.campus_life_backend.common.security.annotation.RequirePermission;
import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.forum.dto.PostDTO;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.service.CategoryService;
import com.campus.campus_life_backend.modules.forum.service.PostService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/posts")
public class PostController {

    private final CurrentUserAccessor currentUserAccessor;
    private final PostService postService;
    private final CategoryService categoryService;

    public PostController(CurrentUserAccessor currentUserAccessor,
                          PostService postService,
                          CategoryService categoryService) {
        this.currentUserAccessor = currentUserAccessor;
        this.postService = postService;
        this.categoryService = categoryService;
    }

    @PostMapping
    @RequirePermission(anyOf = {"post:create"})
    public ApiResponse<Post> createPost(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();

        String title = (String) request.get("title");
        String content = (String) request.get("content");
        Long categoryId = categoryService.resolveCategoryId(request.get("categoryId"), request.get("category"));

        List<PostService.PostImageInfo> images = null;
        if (request.get("images") != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> imagesList = (List<Map<String, Object>>) request.get("images");
            if (imagesList != null && !imagesList.isEmpty()) {
                if (imagesList.size() > 9) {
                    throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "最多只能上传9张图片");
                }

                images = new java.util.ArrayList<>();
                for (Map<String, Object> imgMap : imagesList) {
                    String url = (String) imgMap.get("url");
                    if (url == null || url.isEmpty()) {
                        continue;
                    }

                    Integer width = null;
                    Integer height = null;
                    if (imgMap.get("width") != null) {
                        width = Integer.valueOf(imgMap.get("width").toString());
                    }
                    if (imgMap.get("height") != null) {
                        height = Integer.valueOf(imgMap.get("height").toString());
                    }

                    images.add(new PostService.PostImageInfo(url, width, height));
                }
            }
        }

        Post post = postService.createPost(userId, title, content, categoryId, images);
        return ApiResponse.success(post);
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> getPosts(
            @RequestParam(value = "order", defaultValue = "latest") String order,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "categoryId", required = false) String categoryId) {
        Long categoryIdLong = null;
        Long currentUserId = currentUserAccessor.getCurrentUserId();
        if (categoryId != null && !categoryId.isEmpty()) {
            try {
                categoryIdLong = Long.parseLong(categoryId);
            } catch (NumberFormatException e) {
                categoryIdLong = categoryService.resolveCategoryId(categoryId);
            }
        }

        List<PostDTO> list = postService.getPostsWithUserByOrder(order, page, size, categoryIdLong, currentUserId);
        long total = postService.countPostsByOrder(order, categoryIdLong, currentUserId);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);

        return ApiResponse.success(result);
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<Map<String, Object>> getPostsByUserId(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        List<PostDTO> posts = postService.getPostsByUserId(userId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/user/{userId}/favorites")
    public ApiResponse<Map<String, Object>> getUserFavoritePosts(
            @PathVariable Long userId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long currentUserId = currentUserAccessor.getCurrentUserId();
        List<PostDTO> posts = postService.getFavoritePostsByUserId(userId, currentUserId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/me")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getMyPosts(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = currentUserAccessor.requireUserId();
        List<PostDTO> posts = postService.getPostsByUserId(userId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }

    @GetMapping("/following")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getFollowingPosts(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "12") Integer size) {
        Long userId = currentUserAccessor.requireUserId();
        List<PostDTO> list = postService.getFollowPostsWithUser(userId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", page);
        result.put("size", size);
        result.put("total", postService.countPostsByOrder("follow", null, userId));
        return ApiResponse.success(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<PostDTO> getPostById(@PathVariable Long id) {
        Long currentUserId = currentUserAccessor.getCurrentUserId();
        PostDTO postDTO = postService.getPostDetailById(id, currentUserId);
        if (postDTO == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }
        return ApiResponse.success(postDTO);
    }

    @PutMapping("/{id}")
    @RequirePermission(anyOf = {"post:update:self"})
    @RequireOwnerOrPermission(resource = ResourceTypeCode.POST, idParam = "id")
    public ApiResponse<Post> updatePost(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Post post = postService.getPostById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }

        String title = (String) request.get("title");
        String content = (String) request.get("content");
        Long categoryId = request.get("categoryId") != null
                ? Long.valueOf(request.get("categoryId").toString())
                : null;

        Post updatedPost = postService.updatePost(id, title, content, categoryId);
        return ApiResponse.success(updatedPost);
    }

    @PutMapping("/{id}/status")
    @RequirePermission(anyOf = {"post:update:self"})
    @RequireOwnerOrPermission(resource = ResourceTypeCode.POST, idParam = "id")
    public ApiResponse<Post> updatePostStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Post post = postService.getPostById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }

        Integer status = request.get("status") != null
                ? Integer.valueOf(request.get("status").toString())
                : null;
        if (status == null) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "状态不能为空");
        }
        if (status < 0 || status > 3) {
            throw new BusinessException(BusinessErrorCode.INVALID_PARAM, "状态值无效（0-正常，1-仅自己可见，2-已删除，3-屏蔽）");
        }

        Post updatedPost = postService.updatePostStatus(id, status);
        return ApiResponse.success(updatedPost);
    }

    @DeleteMapping("/{id}")
    @RequirePermission(anyOf = {"post:delete:self"})
    @RequireOwnerOrPermission(resource = ResourceTypeCode.POST, idParam = "id")
    public ApiResponse<String> deletePost(@PathVariable Long id) {
        Post post = postService.getPostById(id);
        if (post == null) {
            throw new BusinessException(BusinessErrorCode.FORUM_POST_NOT_FOUND);
        }

        postService.deletePost(id);
        return ApiResponse.success("删除成功");
    }

    @PostMapping("/{postId}/like")
    @RequireLogin
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long postId) {
        Long userId = currentUserAccessor.requireUserId();
        boolean liked = postService.toggleLike(postId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("liked", liked);
        result.put("likeCount", postService.getPostLikeCount(postId));
        return ApiResponse.success(result);
    }

    @PostMapping("/{postId}/favorite")
    @RequireLogin
    public ApiResponse<Map<String, Object>> toggleFavorite(@PathVariable Long postId) {
        Long userId = currentUserAccessor.requireUserId();
        boolean favorited = postService.toggleFavorite(postId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("favorited", favorited);
        result.put("favoriteCount", postService.getPostFavoriteCount(postId));
        return ApiResponse.success(result);
    }

    @GetMapping("/me/favorites")
    @RequireLogin
    public ApiResponse<Map<String, Object>> getMyFavorites(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = currentUserAccessor.requireUserId();
        List<PostDTO> posts = postService.getFavoritePostsByUserId(userId, page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("list", posts);
        result.put("page", page);
        result.put("size", size);
        return ApiResponse.success(result);
    }
}
