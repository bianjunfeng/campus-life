package com.campus.campus_life_backend.modules.user.dto;

import com.campus.campus_life_backend.modules.user.entity.User;
import lombok.Data;

@Data
public class PublicUserProfileDTO {
    private Long id;
    private String nickName;
    private String icon;
    private String bio;
    private String region;
    private String occupation;
    private Integer postCount;
    private Integer followingCount;
    private Integer followerCount;
    private Boolean isFollowing;

    public static PublicUserProfileDTO fromUser(User user) {
        if (user == null) {
            return null;
        }
        PublicUserProfileDTO dto = new PublicUserProfileDTO();
        dto.setId(user.getId());
        dto.setNickName(user.getUsername() == null ? "" : user.getUsername());
        dto.setIcon(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setRegion(user.getRegion());
        dto.setOccupation(user.getOccupation());
        dto.setPostCount(0);
        dto.setFollowingCount(0);
        dto.setFollowerCount(0);
        dto.setIsFollowing(false);
        return dto;
    }
}
