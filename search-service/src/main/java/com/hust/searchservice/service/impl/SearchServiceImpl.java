package com.hust.searchservice.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.stereotype.Service;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.searchservice.document.CourseDocument;
import com.hust.searchservice.dto.response.CourseSearchResponse;
import com.hust.searchservice.service.SearchService;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public ListResponse<CourseSearchResponse> searchCourses(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            String status,
            Pageable pageable
    ) {
        log.debug("Executing Elasticsearch query with q: {}, authors: {}, topics: {}, levels: {}, prices: {}, rating: {}, status: {}",
                q, authors, topics, levels, prices, rating, status);

        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 1. Text Search (Fuzzy/Match mapping using multiMatch with Boosting)
        if (q != null && !q.trim().isEmpty()) {
            String cleanQ = q.trim();
            boolQueryBuilder.must(QueryBuilders.multiMatch(m -> m
                    .query(cleanQ)
                    .fields("name^3.0", "subTitle^2.0", "description^1.0")
                    .fuzziness("AUTO")
                    .operator(co.elastic.clients.elasticsearch._types.query_dsl.Operator.And)
            ));
        }

        // 2. Filter: Authors (instructorId)
        if (authors != null && !authors.isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.terms(t -> t
                    .field("instructorId")
                    .terms(terms -> terms.value(authors.stream().map(FieldValue::of).toList()))
            ));
        }

        // 3. Filter: Topics (categoryId)
        if (topics != null && !topics.isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.terms(t -> t
                    .field("categoryId")
                    .terms(terms -> terms.value(topics.stream().map(FieldValue::of).toList()))
            ));
        }

        // 4. Filter: Levels
        if (levels != null && !levels.isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.terms(t -> t
                    .field("level")
                    .terms(terms -> terms.value(levels.stream().map(FieldValue::of).toList()))
            ));
        }

        // 5. Filter: Prices (Free / Paid)
        if (prices != null && !prices.isEmpty()) {
            BoolQuery.Builder priceBoolBuilder = new BoolQuery.Builder();
            for (String p : prices) {
                if ("free".equalsIgnoreCase(p)) {
                    priceBoolBuilder.should(QueryBuilders.term(t -> t.field("finalPrice").value(FieldValue.of(0.0))));
                } else if ("paid".equalsIgnoreCase(p)) {
                    priceBoolBuilder.should(QueryBuilders.range(r -> r.field("finalPrice").gt(JsonData.of(0.0))));
                }
            }
            boolQueryBuilder.filter(priceBoolBuilder.build()._toQuery());
        }

        // 6. Filter: Rating Stars
        if (rating != null) {
            boolQueryBuilder.filter(QueryBuilders.range(r -> r.field("avgRatingStars").gte(JsonData.of(rating))));
        }

        // 7. Filter: Status
        if (status != null && !status.trim().isEmpty()) {
            boolQueryBuilder.filter(QueryBuilders.term(t -> t.field("status").value(FieldValue.of(status.trim()))));
        }

        Query esQuery = boolQueryBuilder.build()._toQuery();

        // Build NativeQuery
        NativeQueryBuilder queryBuilder = NativeQuery.builder()
                .withQuery(esQuery)
                .withPageable(pageable);


        // Highlight fields
        queryBuilder.withHighlightQuery(new org.springframework.data.elasticsearch.core.query.HighlightQuery(
                new Highlight(
                        List.of(
                                new HighlightField("name"),
                                new HighlightField("description")
                        )
                ),
                CourseDocument.class
        ));

        // Faceted Search Aggregations
        queryBuilder.withAggregation("by_levels", Aggregation.of(a -> a.terms(t -> t.field("level"))));
        queryBuilder.withAggregation("by_categories", Aggregation.of(a -> a.terms(t -> t.field("categoryId"))));
        queryBuilder.withAggregation("by_prices", Aggregation.of(a -> a.filters(f -> f.filters(
                new co.elastic.clients.elasticsearch._types.aggregations.Buckets.Builder<Query>()
                        .keyed(Map.of(
                                "free", QueryBuilders.term(t -> t.field("finalPrice").value(FieldValue.of(0.0))),
                                "paid", QueryBuilders.range(r -> r.field("finalPrice").gt(JsonData.of(0.0)))
                        ))
                        .build()
        ))));

        // Execute Search
        SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(queryBuilder.build(), CourseDocument.class);

        // Extract list of documents and apply Highlight replacement
        List<CourseDocument> content = searchHits.stream()
                .map(hit -> {
                    CourseDocument doc = hit.getContent();
                    List<String> nameHighlights = hit.getHighlightField("name");
                    if (nameHighlights != null && !nameHighlights.isEmpty()) {
                        doc.setName(nameHighlights.get(0));
                    }
                    List<String> descHighlights = hit.getHighlightField("description");
                    if (descHighlights != null && !descHighlights.isEmpty()) {
                        doc.setDescription(descHighlights.get(0));
                    }
                    return doc;
                })
                .collect(Collectors.toList());

        long totalElements = searchHits.getTotalHits();
        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());

        // Extract Facets (Aggregations)
        Map<String, Object> metaMap = new HashMap<>();
        ElasticsearchAggregations aggregationsContainer = (ElasticsearchAggregations) searchHits.getAggregations();
        if (aggregationsContainer != null) {
            Map<String, Object> aggsMap = new HashMap<>();

            // 1. Levels Aggregation
            try {
                var levelsAgg = aggregationsContainer.get("by_levels");
                if (levelsAgg != null) {
                    Map<String, Long> levelCounts = new HashMap<>();
                    levelsAgg.aggregation().getAggregate().sterms().buckets().array().forEach(bucket ->
                            levelCounts.put(bucket.key().toString(), bucket.docCount())
                    );
                    aggsMap.put("levels", levelCounts);
                }
            } catch (Exception e) {
                log.warn("Failed to extract levels aggregation: {}", e.getMessage());
            }

            // 2. Categories Aggregation
            try {
                var catsAgg = aggregationsContainer.get("by_categories");
                if (catsAgg != null) {
                    Map<String, Long> catCounts = new HashMap<>();
                    catsAgg.aggregation().getAggregate().sterms().buckets().array().forEach(bucket ->
                            catCounts.put(bucket.key().toString(), bucket.docCount())
                    );
                    aggsMap.put("categories", catCounts);
                }
            } catch (Exception e) {
                log.warn("Failed to extract categories aggregation: {}", e.getMessage());
            }

            // 3. Prices Aggregation
            try {
                var pricesAgg = aggregationsContainer.get("by_prices");
                if (pricesAgg != null) {
                    Map<String, Long> priceCounts = new HashMap<>();
                    pricesAgg.aggregation().getAggregate().filters().buckets().keyed().forEach((key, bucket) ->
                            priceCounts.put(key, bucket.docCount())
                    );
                    aggsMap.put("prices", priceCounts);
                }
            } catch (Exception e) {
                log.warn("Failed to extract prices aggregation: {}", e.getMessage());
            }

            metaMap.put("aggregations", aggsMap);
        }

        List<CourseSearchResponse> mappedContent = content.stream()
                .map(doc -> CourseSearchResponse.builder()
                        .id(doc.getId())
                        .name(doc.getName())
                        .subTitle(doc.getSubTitle())
                        .description(doc.getDescription())
                        .price(doc.getPrice())
                        .finalPrice(doc.getFinalPrice())
                        .avgRatingStars(doc.getAvgRatingStars())
                        .studentCount(doc.getStudentCount())
                        .numOfReviews(doc.getNumOfReviews())
                        .level(doc.getLevel())
                        .status(doc.getStatus() != null ? doc.getStatus().name() : null)
                        .instructorId(doc.getInstructorId())
                        .category(doc.getCategoryId() != null ? CourseSearchResponse.CategoryDto.builder()
                                .id(doc.getCategoryId())
                                .name(doc.getCategoryName())
                                .build() : null)
                        .createdAt(doc.getCreatedAt())
                        .build())
                .toList();

        // Map to ListResponse (common response model)
        return ListResponse.of(
                mappedContent,
                pageable.getPageNumber() + 1, // 1-indexed for client compatibility
                pageable.getPageSize(),
                totalElements,
                totalPages,
                pageable.getPageNumber() + 1 >= totalPages,
                metaMap
        );
    }

    @Override
    public List<String> suggestCourses(String q) {
        if (q == null || q.trim().isEmpty()) {
            return List.of();
        }
        String cleanQ = q.trim();

        NativeQuery query = NativeQuery.builder()
                .withQuery(QueryBuilders.match(m -> m
                        .field("name.suggest")
                        .query(cleanQ)
                ))
                .withPageable(PageRequest.of(0, 5))
                .build();

        SearchHits<CourseDocument> searchHits = elasticsearchOperations.search(query, CourseDocument.class);
        return searchHits.stream()
                .map(hit -> hit.getContent().getName())
                .distinct()
                .collect(Collectors.toList());
    }
}
