package com.hust.searchservice.grpc;

import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.grpc.CourseHit;
import com.hust.commonlibrary.grpc.SearchCourseRequest;
import com.hust.commonlibrary.grpc.SearchCourseResponse;
import com.hust.commonlibrary.grpc.SearchServiceGrpcGrpc;
import com.hust.commonlibrary.grpc.SuggestCourseRequest;
import com.hust.commonlibrary.grpc.SuggestCourseResponse;
import com.hust.searchservice.dto.response.CourseSearchResponse;
import com.hust.searchservice.service.SearchService;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class SearchGrpcService extends SearchServiceGrpcGrpc.SearchServiceGrpcImplBase {

    private final SearchService searchService;

    @Override
    public void searchCourses(SearchCourseRequest request, StreamObserver<SearchCourseResponse> responseObserver) {
        log.info("gRPC search request received, q: {}, authors: {}, topics: {}, levels: {}, prices: {}, rating: {}",
                request.getQ(), request.getAuthorsList(), request.getTopicsList(), request.getLevelsList(),
                request.getPricesList(), request.getRating());

        try {
            org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.unsorted();
            if (request.getSortList() != null && !request.getSortList().isEmpty()) {
                java.util.List<org.springframework.data.domain.Sort.Order> orders = request.getSortList().stream().map(s -> {
                    String[] parts = s.split(",");
                    return new org.springframework.data.domain.Sort.Order(
                            org.springframework.data.domain.Sort.Direction.fromString(parts[1]), parts[0]);
                }).toList();
                sort = org.springframework.data.domain.Sort.by(orders);
            }

            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getSize() > 0 ? request.getSize() : 10,
                    sort
            );

            // Double filter handling
            Double rating = request.getRating() > 0 ? request.getRating() : null;

            ListResponse<CourseSearchResponse> result = searchService.searchCourses(
                    request.getQ(),
                    request.getAuthorsList().isEmpty() ? null : request.getAuthorsList(),
                    request.getTopicsList().isEmpty() ? null : request.getTopicsList(),
                    request.getLevelsList().isEmpty() ? null : request.getLevelsList(),
                    request.getPricesList().isEmpty() ? null : request.getPricesList(),
                    rating,
                    "PUBLISHED", // Exclusively retrieve PUBLISHED courses for client-side queries
                    pageable
            );

            SearchCourseResponse.Builder responseBuilder = SearchCourseResponse.newBuilder()
                    .setTotalElements(result.getTotalElements())
                    .setTotalPages(result.getTotalPages())
                    .setPage(result.getPageNumber())
                    .setSize(result.getPageSize());

            // Map course hits
            if (result.getContent() != null) {
                result.getContent().forEach(item -> {
                    CourseHit hit = CourseHit.newBuilder()
                            .setCourseId(item.getId())
                            .setNameHighlight(item.getName() != null ? item.getName() : "")
                            .setDescriptionHighlight(item.getDescription() != null ? item.getDescription() : "")
                            .build();
                    responseBuilder.addHits(hit);
                });
            }

            // Map facets from meta
            if (result.getMeta() != null && result.getMeta().containsKey("aggregations")) {
                Object aggsObj = result.getMeta().get("aggregations");
                if (aggsObj instanceof Map) {
                    Map<?, ?> aggsMap = (Map<?, ?>) aggsObj;

                    // Levels
                    if (aggsMap.containsKey("levels") && aggsMap.get("levels") instanceof Map) {
                        Map<?, ?> levels = (Map<?, ?>) aggsMap.get("levels");
                        levels.forEach((k, v) -> {
                            if (k != null && v instanceof Number) {
                                responseBuilder.putLevelFacets(k.toString(), ((Number) v).longValue());
                            }
                        });
                    }

                    // Categories
                    if (aggsMap.containsKey("categories") && aggsMap.get("categories") instanceof Map) {
                        Map<?, ?> categories = (Map<?, ?>) aggsMap.get("categories");
                        categories.forEach((k, v) -> {
                            if (k != null && v instanceof Number) {
                                responseBuilder.putCategoryFacets(k.toString(), ((Number) v).longValue());
                            }
                        });
                    }

                    // Prices
                    if (aggsMap.containsKey("prices") && aggsMap.get("prices") instanceof Map) {
                        Map<?, ?> prices = (Map<?, ?>) aggsMap.get("prices");
                        prices.forEach((k, v) -> {
                            if (k != null && v instanceof Number) {
                                responseBuilder.putPriceFacets(k.toString(), ((Number) v).longValue());
                            }
                        });
                    }
                }
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error during gRPC searchCourses: ", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error during search: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void suggestCourses(SuggestCourseRequest request, StreamObserver<SuggestCourseResponse> responseObserver) {
        log.info("gRPC suggest request received, q: {}", request.getQ());
        try {
            var suggestions = searchService.suggestCourses(request.getQ());
            SuggestCourseResponse response = SuggestCourseResponse.newBuilder()
                    .addAllSuggestions(suggestions)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error during gRPC suggestCourses: ", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error during suggest: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
