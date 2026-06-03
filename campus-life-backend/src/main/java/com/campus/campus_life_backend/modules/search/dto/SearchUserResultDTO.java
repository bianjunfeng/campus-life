package com.campus.campus_life_backend.modules.search.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchUserResultDTO {
    private List<SearchUserItemDTO> list;
    private long total;
    private int page;
    private int size;
}

