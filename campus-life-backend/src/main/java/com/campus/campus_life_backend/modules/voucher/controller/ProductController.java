package com.campus.campus_life_backend.modules.voucher.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.modules.voucher.service.ProductQueryService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    /**
     * 获取商品详情
     * GET /api/products/{productId}
     */
    @GetMapping("/products/{productId}")
    public ApiResponse<Map<String, Object>> getProductDetail(@PathVariable Long productId) {
        return ApiResponse.success(productQueryService.getProductDetail(productId));
    }
}



