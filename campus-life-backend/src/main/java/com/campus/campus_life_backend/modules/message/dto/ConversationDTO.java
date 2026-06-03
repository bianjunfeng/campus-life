package com.campus.campus_life_backend.modules.message.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 浼氳瘽DTO锛屽寘鍚鏂圭敤鎴蜂俊鎭拰鏈€鏂版秷鎭?
 */
@Data
public class ConversationDTO {
    private String conversationId;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private Boolean isOnline;  // 瀵规柟鏄惁鍦ㄧ嚎锛堝彲閫夛紝闇€瑕侀澶栧疄鐜帮級
}



