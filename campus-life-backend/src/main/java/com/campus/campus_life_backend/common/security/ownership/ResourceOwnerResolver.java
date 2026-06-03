package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import org.springframework.lang.Nullable;

public interface ResourceOwnerResolver {

    boolean supports(ResourceTypeCode resourceType);

    @Nullable
    Long resolveOwnerId(Long resourceId);
}
