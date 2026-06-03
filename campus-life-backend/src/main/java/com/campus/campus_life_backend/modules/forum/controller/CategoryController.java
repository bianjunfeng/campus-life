package com.campus.campus_life_backend.modules.forum.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.modules.forum.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * 获取分类列表（通用，包含帖子分类和商家分类）
     * GET /api/categories
     */
    @GetMapping("/categories")
    public ApiResponse<Map<String, List<Map<String, Object>>>> getCategories() {
        return ApiResponse.success(categoryService.getCategories());
    }

    /**
     * 获取帖子分类列表
     * GET /api/categories/posts
     */
    @GetMapping("/categories/posts")
    public ApiResponse<List<Map<String, Object>>> getPostCategories() {
        return ApiResponse.success(categoryService.getPostCategories());
    }
}


