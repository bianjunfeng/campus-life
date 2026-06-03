package com.campus.campus_life_backend.modules.search.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDateTime;

@Data
@Document(indexName = "forum_post")
@Setting(settingPath = "/search/post-search-settings.json")
public class PostSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String title;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String content;

    @Field(type = FieldType.Keyword)
    private Long authorId;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String authorName;

    @Field(type = FieldType.Keyword)
    private Long categoryId;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String coverImage;

    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Field(type = FieldType.Integer)
    private Integer commentCount;

    @Field(type = FieldType.Integer)
    private Integer favoriteCount;

    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;

    @Field(type = FieldType.Long)
    private Long syncVersion;
}
