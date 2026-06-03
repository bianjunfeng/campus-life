package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceOwnershipService {

    private final List<ResourceOwnerResolver> resolvers;

    public ResourceOwnershipService(List<ResourceOwnerResolver> resolvers) {
        this.resolvers = resolvers;
    }

    @Nullable
    public Long resolveOwnerId(ResourceTypeCode resourceType, Long resourceId) {
        return resolvers.stream()
                .filter(resolver -> resolver.supports(resourceType))
                .findFirst()
                .map(resolver -> resolver.resolveOwnerId(resourceId))
                .orElse(null);
    }

    public boolean isOwner(Long currentUserId, ResourceTypeCode resourceType, Long resourceId) {
        if (currentUserId == null || resourceId == null) {
            return false;
        }
        Long ownerId = resolveOwnerId(resourceType, resourceId);
        return currentUserId.equals(ownerId);
    }
}
