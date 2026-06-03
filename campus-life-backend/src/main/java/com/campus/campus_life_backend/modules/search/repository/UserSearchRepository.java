package com.campus.campus_life_backend.modules.search.repository;

import com.campus.campus_life_backend.modules.search.es.UserSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserSearchRepository extends ElasticsearchRepository<UserSearchDocument, Long> {
}
