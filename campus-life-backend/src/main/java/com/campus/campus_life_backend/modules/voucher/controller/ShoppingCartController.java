package com.campus.campus_life_backend.modules.voucher.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireLogin;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.modules.voucher.service.ShoppingCartService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequireLogin
public class ShoppingCartController {

    private final CurrentUserAccessor currentUserAccessor;
    private final ShoppingCartService shoppingCartService;

    public ShoppingCartController(CurrentUserAccessor currentUserAccessor,
                                  ShoppingCartService shoppingCartService) {
        this.currentUserAccessor = currentUserAccessor;
        this.shoppingCartService = shoppingCartService;
    }

    @GetMapping("/items")
    public ApiResponse<List<Map<String, Object>>> getItems() {
        Long userId = currentUserAccessor.requireUserId();
        return ApiResponse.success(shoppingCartService.getCartViews(userId));
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, Object>> addItem(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        Long voucherId = toLong(request.get("voucherId"));
        Integer quantity = toInteger(request.get("quantity"));
        shoppingCartService.addToCart(userId, voucherId, quantity);
        return ApiResponse.success(Collections.singletonMap("success", true));
    }

    @PutMapping("/items/{id}")
    public ApiResponse<Map<String, Object>> updateItem(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> request
    ) {
        Long userId = currentUserAccessor.requireUserId();
        Integer quantity = toInteger(request.get("quantity"));
        Integer selected = toInteger(request.get("selected"));
        shoppingCartService.updateCartItem(userId, id, quantity, selected);
        return ApiResponse.success(Collections.singletonMap("success", true));
    }

    @PatchMapping("/items/{id}/selected")
    public ApiResponse<Map<String, Object>> updateItemSelected(
            @PathVariable("id") Long id,
            @RequestBody Map<String, Object> request
    ) {
        Long userId = currentUserAccessor.requireUserId();
        Integer selected = toInteger(request.get("selected"));
        shoppingCartService.updateCartItemSelected(userId, id, selected);
        return ApiResponse.success(Collections.singletonMap("success", true));
    }

    @DeleteMapping("/items/{id}")
    public ApiResponse<Map<String, Object>> deleteItem(@PathVariable("id") Long id) {
        Long userId = currentUserAccessor.requireUserId();
        shoppingCartService.removeCartItem(userId, id);
        return ApiResponse.success(Collections.singletonMap("success", true));
    }

    @DeleteMapping("/items")
    public ApiResponse<Map<String, Object>> clear() {
        Long userId = currentUserAccessor.requireUserId();
        shoppingCartService.clearCart(userId);
        return ApiResponse.success(Collections.singletonMap("success", true));
    }

    @PostMapping("/checkout")
    public ApiResponse<Map<String, Object>> checkout(@RequestBody Map<String, Object> request) {
        Long userId = currentUserAccessor.requireUserId();
        List<Long> itemIds = toLongList(request.get("itemIds"));
        return ApiResponse.success(shoppingCartService.checkout(userId, itemIds));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private Integer toInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    @SuppressWarnings("unchecked")
    private List<Long> toLongList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }
        return list.stream().map(this::toLong).toList();
    }
}
