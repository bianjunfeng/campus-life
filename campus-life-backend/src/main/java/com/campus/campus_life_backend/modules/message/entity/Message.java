package com.campus.campus_life_backend.modules.message.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 绉佷俊娑堟伅瀹炰綋绫?
 */
@Data
public class Message {

    private Long id;

    private String conversationId;

    private Long fromUserId;

    private Long toUserId;

    private String content;

    private Integer status;  // 0-鏈;1-宸茶;2-鎾ゅ洖/鍒犻櫎

    private LocalDateTime createTime;
}



