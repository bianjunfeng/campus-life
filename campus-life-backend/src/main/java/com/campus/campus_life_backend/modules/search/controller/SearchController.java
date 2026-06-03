package com.campus.campus_life_backend.modules.search.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.modules.search.dto.SearchPostResultDTO;
import com.campus.campus_life_backend.modules.search.dto.SearchUserResultDTO;
import com.campus.campus_life_backend.modules.search.service.PostSearchService;
import com.campus.campus_life_backend.modules.search.service.SearchOpsService;
import com.campus.campus_life_backend.modules.search.service.UserSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final PostSearchService postSearchService;
    private final UserSearchService userSearchService;
    private final SearchOpsService searchOpsService;

    public SearchController(
            PostSearchService postSearchService,
            UserSearchService userSearchService,
            SearchOpsService searchOpsService
    ) {
        this.postSearchService = postSearchService;
        this.userSearchService = userSearchService;
        this.searchOpsService = searchOpsService;
    }

    @GetMapping("/posts")
    public ApiResponse<SearchPostResultDTO> searchPosts(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            SearchPostResultDTO empty = new SearchPostResultDTO();
            empty.setList(java.util.Collections.emptyList());
            empty.setTotal(0);
            empty.setPage(page == null ? 1 : page);
            empty.setSize(size == null ? 10 : size);
            return ApiResponse.success(empty);
        }

        SearchPostResultDTO result = postSearchService.searchPosts(keyword.trim(), page == null ? 1 : page, size == null ? 10 : size);
        return ApiResponse.success(result);
    }

    @GetMapping("/users")
    public ApiResponse<SearchUserResultDTO> searchUsers(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "8") Integer size
    ) {
        if (keyword == null || keyword.trim().isEmpty()) {
            SearchUserResultDTO empty = new SearchUserResultDTO();
            empty.setList(java.util.Collections.emptyList());
            empty.setTotal(0);
            empty.setPage(page == null ? 1 : page);
            empty.setSize(size == null ? 8 : size);
            return ApiResponse.success(empty);
        }
        SearchUserResultDTO result = userSearchService.searchUsers(keyword.trim(), page == null ? 1 : page, size == null ? 8 : size);
        return ApiResponse.success(result);
    }

    @GetMapping("/all")
    public ApiResponse<Map<String, Object>> searchAll(
            @RequestParam(value = "q", required = false, defaultValue = "") String keyword,
            @RequestParam(value = "postPage", defaultValue = "1") Integer postPage,
            @RequestParam(value = "postSize", defaultValue = "10") Integer postSize,
            @RequestParam(value = "userPage", defaultValue = "1") Integer userPage,
            @RequestParam(value = "userSize", defaultValue = "8") Integer userSize
    ) {
        SearchPostResultDTO posts = postSearchService.searchPosts(keyword, postPage == null ? 1 : postPage, postSize == null ? 10 : postSize);
        SearchUserResultDTO users = userSearchService.searchUsers(keyword, userPage == null ? 1 : userPage, userSize == null ? 8 : userSize);
        Map<String, Object> result = new HashMap<>();
        result.put("posts", posts);
        result.put("users", users);
        return ApiResponse.success(result);
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.success(searchOpsService.getHealthStatus());
    }
}
