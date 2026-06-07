package com.campus.campus_life_backend.modules.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageWebSocketTicketResponse {
    private String ticket;
    private long expiresInSeconds;
}
