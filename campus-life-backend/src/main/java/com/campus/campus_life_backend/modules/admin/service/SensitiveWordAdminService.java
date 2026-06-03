package com.campus.campus_life_backend.modules.admin.service;

import com.campus.campus_life_backend.common.security.annotation.RequireRole;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import com.campus.campus_life_backend.modules.admin.entity.AdminOperationLog;
import com.campus.campus_life_backend.modules.admin.mapper.AdminOperationLogMapper;
import com.campus.campus_life_backend.modules.forum.service.SensitiveFilterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequireRole(RoleCode.ADMIN)
public class SensitiveWordAdminService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordAdminService.class);

    private final SensitiveFilterService sensitiveFilterService;
    private final AdminOperationLogMapper adminOperationLogMapper;

    public SensitiveWordAdminService(
            SensitiveFilterService sensitiveFilterService,
            AdminOperationLogMapper adminOperationLogMapper) {
        this.sensitiveFilterService = sensitiveFilterService;
        this.adminOperationLogMapper = adminOperationLogMapper;
    }

    public Map<String, Object> listWords(String keyword, Integer page, Integer size) {
        return sensitiveFilterService.listWords(keyword, page, size);
    }

    public Map<String, Object> getStats() {
        return sensitiveFilterService.getDictionaryStats();
    }

    public Map<String, Object> addWords(Long adminId, Collection<String> words, String ipAddress) {
        Map<String, Object> result = sensitiveFilterService.addWords(words);
        logOperation(adminId, "add_words", result, ipAddress);
        return result;
    }

    public Map<String, Object> removeWords(Long adminId, Collection<String> words, String ipAddress) {
        Map<String, Object> result = sensitiveFilterService.removeWords(words);
        logOperation(adminId, "remove_words", result, ipAddress);
        return result;
    }

    public Map<String, Object> replaceWord(Long adminId, String oldWord, String newWord, String ipAddress) {
        Map<String, Object> result = sensitiveFilterService.replaceWord(oldWord, newWord);
        logOperation(adminId, "replace_word", result, ipAddress);
        return result;
    }

    public Map<String, Object> reload(Long adminId, String ipAddress) {
        Map<String, Object> result = sensitiveFilterService.reloadNow();
        logOperation(adminId, "reload_words", result, ipAddress);
        return result;
    }

    private void logOperation(Long adminId, String action, Map<String, Object> result, String ipAddress) {
        try {
            AdminOperationLog logEntity = new AdminOperationLog();
            logEntity.setAdminId(adminId);
            logEntity.setOperationType("content_moderation");
            logEntity.setTargetType("sensitive_words");
            logEntity.setAction(action);
            logEntity.setReason(buildReason(action, result));
            logEntity.setIpAddress(ipAddress);
            adminOperationLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("敏感词管理操作日志写入失败: action={}, adminId={}", action, adminId, e);
        }
    }

    private String buildReason(String action, Map<String, Object> result) {
        Object affected = result.get("affected");
        Object ignored = result.get("ignored");
        Object wordCount = result.get("wordCount");
        return "action=" + action
                + ", affected=" + (affected == null ? 0 : affected)
                + ", ignored=" + (ignored == null ? 0 : ignored)
                + ", wordCount=" + (wordCount == null ? "unknown" : wordCount);
    }
}
