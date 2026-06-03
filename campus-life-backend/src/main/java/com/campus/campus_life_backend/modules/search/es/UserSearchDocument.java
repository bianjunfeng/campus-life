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
@Document(indexName = "forum_user")
@Setting(settingPath = "/search/user-search-settings.json")
public class UserSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String username;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String bio;

    @Field(type = FieldType.Text, analyzer = "campus_text_analyzer", searchAnalyzer = "campus_search_analyzer")
    private String phone;

    @Field(type = FieldType.Keyword)
    private String avatarUrl;

    @Field(type = FieldType.Integer)
    private Integer role;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Integer)
    private Integer postCount;

    @Field(type = FieldType.Integer)
    private Integer followerCount;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;

    @Field(type = FieldType.Long)
    private Long syncVersion;
}
