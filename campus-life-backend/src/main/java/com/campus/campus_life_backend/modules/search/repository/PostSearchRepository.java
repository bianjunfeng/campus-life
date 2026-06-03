package com.campus.campus_life_backend.modules.search.repository;

import com.campus.campus_life_backend.modules.search.es.PostSearchDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostSearchRepository extends ElasticsearchRepository<PostSearchDocument, Long> {
}
