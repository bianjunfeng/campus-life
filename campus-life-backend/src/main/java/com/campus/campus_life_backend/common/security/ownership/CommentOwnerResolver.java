package com.campus.campus_life_backend.common.security.ownership;

import com.campus.campus_life_backend.common.security.model.ResourceTypeCode;
import com.campus.campus_life_backend.modules.forum.entity.Comment;
import com.campus.campus_life_backend.modules.forum.mapper.CommentMapper;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class CommentOwnerResolver implements ResourceOwnerResolver {

    private final CommentMapper commentMapper;

    public CommentOwnerResolver(CommentMapper commentMapper) {
        this.commentMapper = commentMapper;
    }

    @Override
    public boolean supports(ResourceTypeCode resourceType) {
        return resourceType == ResourceTypeCode.COMMENT;
    }

    @Override
    @Nullable
    public Long resolveOwnerId(Long resourceId) {
        Comment comment = commentMapper.findById(resourceId);
        return comment != null ? comment.getUserId() : null;
    }
}
