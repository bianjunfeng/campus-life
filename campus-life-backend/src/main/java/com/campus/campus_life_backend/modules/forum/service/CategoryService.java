package com.campus.campus_life_backend.modules.forum.service;

import com.campus.campus_life_backend.modules.forum.entity.PostCategory;
import com.campus.campus_life_backend.modules.forum.mapper.PostCategoryMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final PostCategoryMapper postCategoryMapper;

    public CategoryService(PostCategoryMapper postCategoryMapper) {
        this.postCategoryMapper = postCategoryMapper;
    }

    public Map<String, List<Map<String, Object>>> getCategories() {
        List<Map<String, Object>> categoryList = postCategoryMapper.findAllActive().stream()
                .map(this::toCategoryItem)
                .collect(Collectors.toList());
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        result.put("categories", categoryList);
        return result;
    }

    public List<Map<String, Object>> getPostCategories() {
        List<Map<String, Object>> result = postCategoryMapper.findAllActive().stream()
                .map(category -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", category.getId().toString());
                    item.put("name", category.getName());
                    item.put("code", category.getCode());
                    item.put("type", "category");
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> recommend = new HashMap<>();
        recommend.put("id", "recommend");
        recommend.put("name", "推荐");
        recommend.put("code", "recommend");
        recommend.put("type", "content");
        result.add(0, recommend);
        return result;
    }

    public Long resolveCategoryId(Object categoryIdValue, Object categoryCodeValue) {
        Long categoryId = parseLong(categoryIdValue);
        if (categoryId != null) {
            return categoryId;
        }
        if (categoryCodeValue != null) {
            PostCategory category = postCategoryMapper.findByCode(String.valueOf(categoryCodeValue));
            if (category != null) {
                return category.getId();
            }
        }
        return resolveDefaultCategoryId();
    }

    public Long resolveCategoryId(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        Long id = parseLong(value);
        if (id != null) {
            return id;
        }
        PostCategory category = postCategoryMapper.findByCode(value);
        if (category != null) {
            return category.getId();
        }
        for (PostCategory cat : postCategoryMapper.findAllActive()) {
            if (cat.getName().contains(value) || value.contains(cat.getName())) {
                return cat.getId();
            }
        }
        return null;
    }

    private Long resolveDefaultCategoryId() {
        PostCategory defaultCategory = postCategoryMapper.findByCode("campus-life");
        if (defaultCategory != null) {
            return defaultCategory.getId();
        }
        for (PostCategory cat : postCategoryMapper.findAllActive()) {
            if ("校园生活".equals(cat.getName())) {
                return cat.getId();
            }
        }
        return null;
    }

    private Map<String, Object> toCategoryItem(PostCategory category) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", category.getId());
        item.put("name", category.getName());
        item.put("code", category.getCode());
        item.put("description", category.getDescription());
        return item;
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }
}
