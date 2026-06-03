package com.campus.campus_life_backend.modules.message.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 娑堟伅DTO锛屽寘鍚敤鎴蜂俊鎭?
 */
@Data
public class MessageDTO {
    private Long id;
    private String conversationId;
    private Long fromUserId;
    private String fromUserName;
    private String fromUserAvatar;
    private Long toUserId;
    private String toUserName;
    private String toUserAvatar;
    private String content;
    private Integer status;  // 0-鏈;1-宸茶;2-鎾ゅ洖/鍒犻櫎
    private LocalDateTime createTime;
}



