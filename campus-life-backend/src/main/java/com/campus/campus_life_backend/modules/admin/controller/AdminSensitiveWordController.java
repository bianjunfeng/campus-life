package com.campus.campus_life_backend.modules.admin.controller;

import com.campus.campus_life_backend.common.result.ApiResponse;
import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.common.security.support.CurrentUserAccessor;
import com.campus.campus_life_backend.common.util.AdminAuthUtil;
import com.campus.campus_life_backend.modules.admin.service.SensitiveWordAdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sensitive-words")
@RequireRole(RoleCode.ADMIN)
public class AdminSensitiveWordController {

    private final SensitiveWordAdminService sensitiveWordAdminService;
    private final CurrentUserAccessor currentUserAccessor;
    private final AdminAuthUtil adminAuthUtil;

    public AdminSensitiveWordController(
            SensitiveWordAdminService sensitiveWordAdminService,
            CurrentUserAccessor currentUserAccessor,
            AdminAuthUtil adminAuthUtil) {
        this.sensitiveWordAdminService = sensitiveWordAdminService;
        this.currentUserAccessor = currentUserAccessor;
        this.adminAuthUtil = adminAuthUtil;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> listWords(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size) {
        requireAdmin();
        return ApiResponse.success(sensitiveWordAdminService.listWords(keyword, page, size));
    }

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        requireAdmin();
        return ApiResponse.success(sensitiveWordAdminService.getStats());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> addWords(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long adminId = requireAdmin();
        return ApiResponse.success(sensitiveWordAdminService.addWords(
                adminId,
                extractWords(request),
                adminAuthUtil.getClientIp(httpRequest)
        ));
    }

    @DeleteMapping
    public ApiResponse<Map<String, Object>> removeWords(
            @RequestBody(required = false) Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long adminId = requireAdmin();
        return ApiResponse.success(sensitiveWordAdminService.removeWords(
                adminId,
                extractWords(request),
                adminAuthUtil.getClientIp(httpRequest)
        ));
    }

    @PutMapping
    public ApiResponse<Map<String, Object>> replaceWord(
            @RequestBody Map<String, Object> request,
            HttpServletRequest httpRequest) {
        Long adminId = requireAdmin();
        String oldWord = request == null ? null : stringValue(request.get("oldWord"));
        String newWord = request == null ? null : stringValue(request.get("newWord"));
        return ApiResponse.success(sensitiveWordAdminService.replaceWord(
                adminId,
                oldWord,
                newWord,
                adminAuthUtil.getClientIp(httpRequest)
        ));
    }

    @PostMapping("/reload")
    public ApiResponse<Map<String, Object>> reload(HttpServletRequest httpRequest) {
        Long adminId = requireAdmin();
        return ApiResponse.success(sensitiveWordAdminService.reload(adminId, adminAuthUtil.getClientIp(httpRequest)));
    }

    private Long requireAdmin() {
        return currentUserAccessor.requireUserId();
    }

    private Collection<String> extractWords(Map<String, Object> request) {
        if (request == null || request.isEmpty()) {
            return List.of();
        }

        List<String> words = new ArrayList<>();
        Object wordList = request.get("words");
        if (wordList instanceof Collection<?> collection) {
            for (Object item : collection) {
                String value = stringValue(item);
                if (value != null && !value.isBlank()) {
                    words.add(value);
                }
            }
        }

        Object content = request.get("content");
        if (content != null) {
            String raw = String.valueOf(content);
            for (String line : raw.split("\\r?\\n")) {
                String value = stringValue(line);
                if (value != null && !value.isBlank()) {
                    words.add(value);
                }
            }
        }
        return words;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
