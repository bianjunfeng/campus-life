package com.campus.campus_life_backend.common.security.service;

import com.campus.campus_life_backend.common.security.model.PermissionCode;
import com.campus.campus_life_backend.common.security.model.RoleCode;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RolePermissionRegistry {

    private final Map<RoleCode, Set<String>> permissionsByRole;

    public RolePermissionRegistry() {
        EnumMap<RoleCode, Set<String>> mapping = new EnumMap<>(RoleCode.class);
        mapping.put(RoleCode.GUEST, Set.of());
        mapping.put(RoleCode.STUDENT, codesOf(
                PermissionCode.USER_PROFILE_READ_SELF,
                PermissionCode.USER_PROFILE_UPDATE_SELF,
                PermissionCode.POST_CREATE,
                PermissionCode.POST_UPDATE_SELF,
                PermissionCode.POST_DELETE_SELF,
                PermissionCode.COMMENT_CREATE,
                PermissionCode.COMMENT_DELETE_SELF,
                PermissionCode.REPORT_CREATE,
                PermissionCode.MESSAGE_USE,
                PermissionCode.FILE_UPLOAD,
                PermissionCode.ORDER_READ_SELF,
                PermissionCode.PAYMENT_REFUND_APPLY_SELF,
                PermissionCode.VOUCHER_ORDER_SELF,
                PermissionCode.AI_USE
        ));
        mapping.put(RoleCode.MERCHANT, merge(
                mapping.get(RoleCode.STUDENT),
                codesOf(
                        PermissionCode.MERCHANT_ACCESS,
                        PermissionCode.MERCHANT_PROFILE_MANAGE,
                        PermissionCode.MERCHANT_LOCATION_UPDATE,
                        PermissionCode.MERCHANT_VOUCHER_MANAGE,
                        PermissionCode.MERCHANT_REFUND_REVIEW
                )
        ));
        mapping.put(RoleCode.ADMIN, Arrays.stream(PermissionCode.values())
                .map(PermissionCode::code)
                .collect(Collectors.toUnmodifiableSet()));
        this.permissionsByRole = Map.copyOf(mapping);
    }

    public Set<String> getPermissions(RoleCode roleCode) {
        return permissionsByRole.getOrDefault(roleCode, Set.of());
    }

    private static Set<String> codesOf(PermissionCode... permissionCodes) {
        return Arrays.stream(permissionCodes)
                .map(PermissionCode::code)
                .collect(Collectors.toUnmodifiableSet());
    }

    private static Set<String> merge(Set<String> first, Set<String> second) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.addAll(first);
        merged.addAll(second);
        return Set.copyOf(merged);
    }
}
