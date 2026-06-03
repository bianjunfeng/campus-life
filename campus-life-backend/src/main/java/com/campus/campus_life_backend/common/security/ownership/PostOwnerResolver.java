package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.forum.entity.Post;
import com.campus.campus_life_backend.modules.forum.mapper.PostMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PostOwnerResolver implements ResourceOwnerResolver {

    private final PostMapper postMapper;

    public PostOwnerResolver(PostMapper postMapper) {
        this.postMapper = postMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.POST;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        Post post = postMapper.findById(resourceId);
        return post != null ? post.getUserId() : null;
    }
}
