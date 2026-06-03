package com.campus.campus_life_backend.modules.user.dto;

import lombok.Data;

/**
 * 用户DTO，用于返回给前端
 */
@Data
public class UserDTO {
    private Long id;
    private String phone;
    private String email;
    private String nickName;  // 对应User的username
    private String icon;      // 对应User的avatarUrl
    private String bio;       // 对应User的bio
    private String gender;    // 对应User的gender（转换为字符串：保密、男、女）
    private String birthday;  // 对应User的birthday（转换为字符串）
    private String region;    // 对应User的region
    private String occupation; // 对应User的occupation
    private Integer status;
    private String createTime;
    private String updateTime;
    private String role;      // 转换为字符串: GUEST, STUDENT, MERCHANT, ADMIN
    
    public static UserDTO fromUser(com.campus.campus_life_backend.modules.user.entity.User user) {
        if (user == null) {
            return null;
        }
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone() != null ? user.getPhone() : "");
        dto.setEmail(user.getEmail() != null ? user.getEmail() : "");
        dto.setNickName(user.getUsername() != null ? user.getUsername() : "");
        dto.setIcon(user.getAvatarUrl());
        dto.setBio(user.getBio());
        dto.setRegion(user.getRegion());
        dto.setOccupation(user.getOccupation());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime() == null ? null : user.getCreateTime().toString());
        dto.setUpdateTime(user.getUpdateTime() == null ? null : user.getUpdateTime().toString());
        
        // 转换性别：0-保密;1-男;2-女
        if (user.getGender() != null) {
            switch (user.getGender()) {
                case 0:
                    dto.setGender("保密");
                    break;
                case 1:
                    dto.setGender("男");
                    break;
                case 2:
                    dto.setGender("女");
                    break;
                default:
                    dto.setGender("保密");
            }
        }
        
        // 转换生日为字符串
        if (user.getBirthday() != null) {
            dto.setBirthday(user.getBirthday().toString());
        }
        
        // 转换role
        if (user.getRole() == null) {
            dto.setRole("GUEST");
        } else {
            switch (user.getRole()) {
                case 0:
                    dto.setRole("STUDENT");
                    break;
                case 1:
                    dto.setRole("MERCHANT");
                    break;
                case 2:
                    dto.setRole("ADMIN");
                    break;
                default:
                    dto.setRole("GUEST");
            }
        }
        
        return dto;
    }
}

