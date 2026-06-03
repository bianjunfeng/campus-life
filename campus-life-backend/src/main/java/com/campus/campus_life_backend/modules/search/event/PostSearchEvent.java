package com.campus.campus_life_backend.modules.search.event;

import lombok.Data;

@Data
public class PostSearchEvent {
    private Long postId;
    private String action; // UPSERT / DELETE
    private String reason;
    private Long timestamp;
    private Long syncVersion;
}
