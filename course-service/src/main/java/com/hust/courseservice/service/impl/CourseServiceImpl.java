package com.hust.courseservice.service.impl;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hust.commonlibrary.annotation.CustomCache;
import com.hust.commonlibrary.annotation.CustomCacheEvict;
import com.hust.commonlibrary.constants.AppConstants;
import com.hust.commonlibrary.constants.RedisPrefixConstants;
import com.hust.commonlibrary.dto.ListResponse;
import com.hust.commonlibrary.dto.UserSharedProfile;
import com.hust.commonlibrary.event.CourseFinalExamLinkedEvent;
import com.hust.commonlibrary.exception.payload.InvalidParamException;
import com.hust.commonlibrary.exception.payload.ResourceNotFoundException;
import com.hust.commonlibrary.service.RedisService;
import com.hust.commonlibrary.utils.SecurityUtils;
import com.hust.courseservice.dto.request.CourseRequest;
import com.hust.courseservice.dto.response.ActionLogResponse;
import com.hust.courseservice.dto.response.AdminCourseStatsResponse;
import com.hust.courseservice.dto.response.CourseResponse;
import com.hust.courseservice.dto.response.LessonResponse;
import com.hust.courseservice.dto.response.SectionResponse;
import com.hust.courseservice.entity.ActionLog;
import com.hust.courseservice.entity.Category;
import com.hust.courseservice.entity.Course;
import com.hust.courseservice.entity.Lesson;
import com.hust.courseservice.entity.Section;
import com.hust.courseservice.entity.enums.ActionLogType;
import com.hust.courseservice.entity.enums.CourseStatus;
import com.hust.courseservice.entity.enums.FunctionType;
import com.hust.courseservice.mapper.CourseMapper;
import com.hust.courseservice.mapper.LessonMapper;
import com.hust.courseservice.mapper.SectionMapper;
import com.hust.courseservice.repository.ActionLogRepository;
import com.hust.courseservice.repository.CategoryRepository;
import com.hust.courseservice.repository.CourseRepository;
import com.hust.courseservice.repository.LessonRepository;
import com.hust.courseservice.repository.SectionRepository;
import com.hust.courseservice.service.CourseService;
import com.hust.courseservice.service.SectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"null", "rawtypes"})
public class CourseServiceImpl implements CourseService {
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final SectionRepository sectionRepository;
    private final LessonRepository lessonRepository;
    private final CourseMapper courseMapper;
    private final SectionMapper sectionMapper;
    private final LessonMapper lessonMapper;
    private final ActionLogRepository actionLogRepository;
    private final RedisService redisService;
    private final SectionService sectionService;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @net.devh.boot.grpc.client.inject.GrpcClient("search-service")
    private com.hust.commonlibrary.grpc.SearchServiceGrpcGrpc.SearchServiceGrpcBlockingStub searchServiceGrpcBlockingStub;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByCategories(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        // Using MongoTemplate aggregation for high performance
        Aggregation aggregation =
            Aggregation.newAggregation(
                Aggregation.match(
                    org.springframework.data.mongodb.core.query.Criteria.where("category").in(
                        categoryIds.stream().map(org.bson.types.ObjectId::new).toList()
                    )
                ),
                Aggregation.group("category").count().as("count")
            );

        AggregationResults<Map> results =
            mongoTemplate.aggregate(aggregation, Course.class, Map.class);

        Map<String, Long> countMap = new HashMap<>();
        // Initialize all requested categoryIds to 0
        for (String id : categoryIds) {
            countMap.put(id, 0L);
        }
        for (Map<?, ?> row : results.getMappedResults()) {
            Object idObj = row.get("_id");
            Object countObj = row.get("count");
            if (idObj != null && countObj != null) {
                countMap.put(idObj.toString(), ((Number) countObj).longValue());
            }
        }
        return countMap;
    }

    private void updateCourseOwnerCache(Course course) {
        try {
            String key = RedisPrefixConstants.getSharedCourseOwnerKey(course.getId());
            redisService.set(key, course.getInstructorId());
            log.info("Synced Redis: Course {} permanently mapped to Owner {}", course.getId(), course.getInstructorId());
        } catch (Exception e) {
            log.error("Cache Failure: Non-blocking exception syncing Redis for Course owner: ", e);
        }
    }

    private void deleteCourseOwnerCache(String courseId) {
        try {
            String key = RedisPrefixConstants.getSharedCourseOwnerKey(courseId);
            redisService.delete(key);
            log.info("Evicted Redis: Removed Course {} from Shared Auth", courseId);
        } catch (Exception e) {
            log.error("Cache Failure: Non-blocking exception evicting Course owner from Redis: ", e);
        }
    }

    @Override
    @Transactional
    public CourseResponse create(CourseRequest request) {
        String instructorId = SecurityUtils.getCurrentUserIdOrThrow();

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            AppConstants.Resource_Constants.CATEGORY,
                            AppConstants.Field_Constants.ID,
                            request.getCategoryId()));
        }

        Course course = courseMapper.requestToEntity(request);

        // Ensure defaults if null
        if (course.getPrice() == null) course.setPrice(0.0);
        if (course.getFinalPrice() == null) course.setFinalPrice(0.0);
        validatePrice(course.getPrice(), course.getFinalPrice());

        course.setInstructorId(instructorId);
        course.setCategory(category);
        String uniqueSlugSuffix = java.util.UUID.randomUUID().toString().substring(0, 6);
        course.setCourseSlug(toSlug(request.getName()) + "-" + uniqueSlugSuffix);
        String uniqueCodeSuffix = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        course.setCode("CRS-" + uniqueCodeSuffix);

        course = courseRepository.save(course);

        // Permanently map courseId -> instructorId on Redis Cluster
        updateCourseOwnerCache(course);

        // Only publish search sync for courses that are relevant for search index
        if (course.getStatus() != null && course.getStatus() != CourseStatus.DRAFT) {
            publishSearchSyncEvent(course, com.hust.commonlibrary.enums.SyncAction.CREATE);
        }

        logAction(course.getId(), ActionLogType.CREATE, "Course created by instructor", FunctionType.COURSE);

        CourseResponse response = courseMapper.entityToResponse(course);
        enrichInstructors(java.util.Collections.singletonList(response));
        return response;
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'course:detail:' + #id") // 🧹 DỌN DẸP CACHE: Xóa cache cũ ngay khi cập nhật thông tin khóa học!
    public CourseResponse update(String id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));

        validateCourseOwnership(course);

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            AppConstants.Resource_Constants.CATEGORY,
                            AppConstants.Field_Constants.ID,
                            request.getCategoryId()));
        }

        String oldName = course.getName();
        courseMapper.partialUpdate(course, request);

        // Ensure defaults if null after partial mapping
        if (course.getPrice() == null) course.setPrice(0.0);
        if (course.getFinalPrice() == null) course.setFinalPrice(0.0);
        validatePrice(course.getPrice(), course.getFinalPrice());

        course.setCategory(category);
        if (request.getName() != null && !request.getName().equals(oldName)) {
            String uniqueSlugSuffix = java.util.UUID.randomUUID().toString().substring(0, 6);
            course.setCourseSlug(toSlug(request.getName()) + "-" + uniqueSlugSuffix);
        } else if (course.getCourseSlug() == null) {
            String uniqueSlugSuffix = java.util.UUID.randomUUID().toString().substring(0, 6);
            course.setCourseSlug(toSlug(course.getName()) + "-" + uniqueSlugSuffix);
        }

        course = courseRepository.save(course);
        // api update instroctor not allowed update instructor

        publishSearchSyncEvent(course, com.hust.commonlibrary.enums.SyncAction.UPDATE);

        logAction(course.getId(), ActionLogType.UPDATE, "Course information updated by instructor", FunctionType.COURSE);

        CourseResponse response = courseMapper.entityToResponse(course);
        enrichInstructors(java.util.Collections.singletonList(response));
        return response;
    }

    @Override
    @Transactional
    public void delete(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<Course> courses = courseRepository.findAllById(ids);
        if (courses.isEmpty()) return;

        for (Course course : courses) {
            validateCourseOwnership(course);
        }

        // Use only IDs of courses actually found in DB
        List<String> foundIds = courses.stream().map(Course::getId).toList();

        // 1. Cascade delete sections (and their lessons) belonging to these courses
        for (String courseId : foundIds) {
            List<Section> sections = sectionRepository.findAllByCourseIdOrderByPositionAsc(courseId);
            if (sections != null && !sections.isEmpty()) {
                List<String> sectionIds = sections.stream().map(Section::getId).toList();
                sectionService.delete(sectionIds);
            }
        }

        // 2. Cascade delete ActionLogs (orphan cleanup) BEFORE deleting courses
        try {
            actionLogRepository.deleteAllByCourseIdIn(foundIds);
            log.info("Successfully cleaned up action logs for deleted courses: {}", foundIds);
        } catch (Exception e) {
            log.error("Failed to clean up action logs for deleted courses {}: {}", foundIds, e.getMessage());
        }

        // 3. Delete course documents from MongoDB
        courseRepository.deleteAll(courses);

        // 4. Publish sync events for deletion (remove from search index)
        for (Course course : courses) {
            publishSearchSyncEvent(course, com.hust.commonlibrary.enums.SyncAction.DELETE);
        }

        // 5. Evict permanent owner mappings from Redis
        for (String id : foundIds) {
            deleteCourseOwnerCache(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @CustomCache(key = "'course:detail:' + #id", ttl = 30, unit = TimeUnit.MINUTES) // 🚀 TỐI ƯU HIỆU NĂNG: Cache toàn bộ Curriculum cực nặng trong 30 phút!
    public CourseResponse detail(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        AppConstants.Resource_Constants.COURSE,
                        AppConstants.Field_Constants.ID,
                        id));

        CourseResponse response = courseMapper.entityToResponse(course);
        enrichCurriculum(response);
        enrichInstructors(java.util.Collections.singletonList(response));
        return response;
    }



    private void enrichCurriculum(CourseResponse response) {
        var sections = sectionRepository.findAllByCourseIdOrderByPositionAsc(response.getId());
        var allLessons = lessonRepository.findAllByCourseIdOrderByPositionAsc(response.getId());

        var lessonsBySection = allLessons.stream()
                .collect(Collectors.groupingBy(Lesson::getSectionId));

        int totalLessons = allLessons.size();
        double totalDuration = allLessons.stream()
                .mapToDouble(l -> l.getVideoLength() != null ? l.getVideoLength() : 0)
                .sum();

        List<SectionResponse> sectionResponses = sections.stream()
                .map(section -> {
                    var sResp = sectionMapper.entityToResponse(section);
                    var lessons = lessonsBySection.getOrDefault(section.getId(), Collections.emptyList());

                    List<LessonResponse> lResponses = lessonMapper.entityToResponse(lessons);
                    sResp.setLessons(lResponses);
                    return sResp;
                })
                .toList();

        response.setSections(sectionResponses);
        response.setSectionCount(sections.size());
        response.setLessonCount(totalLessons);
        response.setTotalVideosLength(totalDuration);
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<CourseResponse> search(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            CourseStatus status,
            Pageable pageable
    ) {
        return fallbackSearch(q, authors, topics, levels, prices, rating, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<CourseResponse> searchElasticsearch(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            Pageable pageable
    ) {
        log.info("Performing Elasticsearch search via gRPC client: q={}, authors={}, topics={}, levels={}, prices={}, rating={}",
                q, authors, topics, levels, prices, rating);

        try {
            com.hust.commonlibrary.grpc.SearchCourseRequest.Builder requestBuilder = com.hust.commonlibrary.grpc.SearchCourseRequest.newBuilder()
                    .setPage(pageable.getPageNumber()) // Already 0-indexed from Spring MVC
                    .setSize(pageable.getPageSize());

            if (pageable.getSort().isSorted()) {
                List<String> sorts = pageable.getSort().stream()
                        .map(order -> order.getProperty() + "," + order.getDirection().name())
                        .toList();
                requestBuilder.addAllSort(sorts);
            }

            if (q != null) requestBuilder.setQ(q);
            if (authors != null) requestBuilder.addAllAuthors(authors);
            if (topics != null) requestBuilder.addAllTopics(topics);
            if (levels != null) requestBuilder.addAllLevels(levels);
            if (prices != null) requestBuilder.addAllPrices(prices);
            if (rating != null) requestBuilder.setRating(rating);

            com.hust.commonlibrary.grpc.SearchCourseResponse grpcResponse = searchServiceGrpcBlockingStub.searchCourses(requestBuilder.build());

            // Extract hits and highlights
            List<com.hust.commonlibrary.grpc.CourseHit> hits = grpcResponse.getHitsList();
            if (hits.isEmpty()) {
                return ListResponse.of(
                        Collections.emptyList(),
                        grpcResponse.getPage() + 1, // 0-indexed back to 1-indexed for FE
                        grpcResponse.getSize(),
                        grpcResponse.getTotalElements(),
                        grpcResponse.getTotalPages(),
                        grpcResponse.getPage() >= grpcResponse.getTotalPages() - 1
                );
            }

            List<String> ids = hits.stream().map(com.hust.commonlibrary.grpc.CourseHit::getCourseId).toList();

            // Fetch from MongoDB
            List<Course> courses = courseRepository.findAllById(ids);

            // Sort courses according to the order of ids returned by Elasticsearch
            Map<String, Course> courseMap = courses.stream()
                    .collect(Collectors.toMap(Course::getId, c -> c));

            List<Course> sortedCourses = new ArrayList<>();
            for (String id : ids) {
                Course c = courseMap.get(id);
                if (c != null) {
                    sortedCourses.add(c);
                }
            }

            // Map to response
            List<CourseResponse> responses = courseMapper.entityToResponse(sortedCourses);

            // Apply highlights to name and description if they exist
            Map<String, com.hust.commonlibrary.grpc.CourseHit> hitMap = hits.stream()
                    .collect(Collectors.toMap(com.hust.commonlibrary.grpc.CourseHit::getCourseId, h -> h));

            for (CourseResponse res : responses) {
                com.hust.commonlibrary.grpc.CourseHit hit = hitMap.get(res.getId());
                if (hit != null) {
                    if (hit.getNameHighlight() != null && !hit.getNameHighlight().isEmpty()) {
                        res.setName(hit.getNameHighlight());
                    }
                    if (hit.getDescriptionHighlight() != null && !hit.getDescriptionHighlight().isEmpty()) {
                        res.setDescription(hit.getDescriptionHighlight());
                    }
                }
            }

            enrichInstructors(responses);
            enrichCourseCounts(responses);

            // Map facets to meta
            Map<String, Object> metaMap = new HashMap<>();
            Map<String, Object> aggsMap = new HashMap<>();
            aggsMap.put("levels", grpcResponse.getLevelFacetsMap());
            aggsMap.put("categories", grpcResponse.getCategoryFacetsMap());
            aggsMap.put("prices", grpcResponse.getPriceFacetsMap());
            metaMap.put("aggregations", aggsMap);

            return ListResponse.of(
                    responses,
                    grpcResponse.getPage() + 1, // 0-indexed back to 1-indexed for FE
                    grpcResponse.getSize(),
                    grpcResponse.getTotalElements(),
                    grpcResponse.getTotalPages(),
                    grpcResponse.getPage() >= grpcResponse.getTotalPages() - 1,
                    metaMap
            );
        } catch (Exception e) {
            log.error("Failed to perform search via gRPC: ", e);
            log.warn("Falling back to local MongoDB search...");
            return fallbackSearch(q, authors, topics, levels, prices, rating, CourseStatus.PUBLISHED, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> suggestCourses(String q, int limit) {
        log.info("Requesting suggestions from search-service via gRPC client: q={}, limit={}", q, limit);
        try {
            com.hust.commonlibrary.grpc.SuggestCourseRequest suggestRequest = com.hust.commonlibrary.grpc.SuggestCourseRequest.newBuilder()
                    .setQ(q != null ? q : "")
                    .setLimit(limit > 0 ? limit : 5)
                    .build();

            com.hust.commonlibrary.grpc.SuggestCourseResponse suggestResponse = searchServiceGrpcBlockingStub.suggestCourses(suggestRequest);
            return suggestResponse.getSuggestionsList();
        } catch (Exception e) {
            log.error("Failed to fetch autocomplete suggestions via gRPC: ", e);
            return Collections.emptyList();
        }
    }

    private ListResponse<CourseResponse> fallbackSearch(
            String q,
            List<String> authors,
            List<String> topics,
            List<String> levels,
            List<String> prices,
            Double rating,
            CourseStatus status,
            Pageable pageable
    ) {
        Query mongoQuery = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (q != null && !q.trim().isEmpty()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex(q, "i"),
                    Criteria.where("subTitle").regex(q, "i"),
                    Criteria.where("code").regex(q, "i"),
                    Criteria.where("description").regex(q, "i")
            ));
        }

        if (authors != null && !authors.isEmpty()) {
            criteriaList.add(Criteria.where("instructorId").in(authors));
        }

        if (topics != null && !topics.isEmpty()) {
            criteriaList.add(Criteria.where("category").in(
                    topics.stream().map(ObjectId::new).toList()
            ));
        }

        if (levels != null && !levels.isEmpty()) {
            criteriaList.add(Criteria.where("level").in(levels));
        }

        if (prices != null && !prices.isEmpty()) {
            List<Criteria> priceCriteria = new ArrayList<>();
            for (String p : prices) {
                if ("Free".equalsIgnoreCase(p)) {
                    priceCriteria.add(Criteria.where("finalPrice").is(0.0));
                } else if ("Paid".equalsIgnoreCase(p)) {
                    priceCriteria.add(Criteria.where("finalPrice").gt(0.0));
                }
            }
            if (!priceCriteria.isEmpty()) {
                criteriaList.add(new Criteria().orOperator(priceCriteria.toArray(new Criteria[0])));
            }
        }

        if (rating != null) {
            criteriaList.add(Criteria.where("avgRatingStars").gte(rating));
        }

        if (status != null) {
            criteriaList.add(Criteria.where("status").is(status));
        }

        if (!criteriaList.isEmpty()) {
            mongoQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long totalRows = mongoTemplate.count(mongoQuery, Course.class);
        mongoQuery.with(pageable);

        List<Course> courses = mongoTemplate.find(mongoQuery, Course.class);
        List<CourseResponse> responses = courseMapper.entityToResponse(courses);
        enrichInstructors(responses);
        enrichCourseCounts(responses);
        Page<Course> coursePage = new PageImpl<>(courses, pageable, totalRows);
        return ListResponse.of(responses, coursePage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getPopularCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("views").descending());
        List<CourseResponse> responses = courseMapper.entityToResponse(courseRepository.findAllByStatus(CourseStatus.PUBLISHED, pageable).getContent());
        enrichInstructors(responses);
        enrichCourseCounts(responses);
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    @CustomCache(key = "'course:trending:' + #limit", ttl = 60, unit = TimeUnit.MINUTES)
    public List<CourseResponse> getTrendingCourses(int limit) {
        Aggregation aggregation = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("status").is(CourseStatus.PUBLISHED)),
            Aggregation.project("id", "code", "name", "subTitle", "thumbnail", "price", "finalPrice",
                                "description", "level", "status", "courseSlug", "instructorId",
                                "category", "requirements", "willLearns", "tags", "avgRatingStars",
                                "studentCount", "numOfReviews", "views", "finalExamId")
                .andExpression("studentCount * 10 + avgRatingStars * 20 + views * 0.1")
                .as("popularityScore"),
            Aggregation.sort(Sort.by(Sort.Direction.DESC, "popularityScore")),
            Aggregation.limit(limit)
        );

        List<Course> courses = mongoTemplate.aggregate(aggregation, "courses", Course.class).getMappedResults();
        List<CourseResponse> responses = courseMapper.entityToResponse(courses);
        enrichInstructors(responses);
        enrichCourseCounts(responses);
        return responses;
    }

    @Override
    @com.hust.commonlibrary.annotation.CustomCache(key = "'recommended_courses:' + (#enrolledIds != null ? #enrolledIds.toString() : 'guest')", ttl = 60)
    @Transactional(readOnly = true)
    public List<CourseResponse> getRecommendedCourses(List<String> enrolledIds, int limit) {
        if (enrolledIds == null || enrolledIds.isEmpty()) {
            return getTrendingCourses(limit);
        }

        Criteria criteria = Criteria.where("status").is(CourseStatus.PUBLISHED);
        criteria.and("id").nin(enrolledIds);

        List<Course> enrolledCourses = courseRepository.findAllById(enrolledIds);

        List<String> interestedCategoryIds = enrolledCourses.stream()
                .map(c -> c.getCategory() != null ? c.getCategory().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        List<String> interestedTags = enrolledCourses.stream()
                .filter(c -> c.getTags() != null)
                .flatMap(c -> c.getTags().stream())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        List<Criteria> orCriteriaList = new java.util.ArrayList<>();
        if (!interestedCategoryIds.isEmpty()) {
            orCriteriaList.add(Criteria.where("category.id").in(interestedCategoryIds));
        }
        if (!interestedTags.isEmpty()) {
            orCriteriaList.add(Criteria.where("tags").in(interestedTags));
        }

        if (!orCriteriaList.isEmpty()) {
            criteria.andOperator(new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0])));
        }

        Query query = new Query(criteria)
                .with(Sort.by(Sort.Direction.DESC, "avgRatingStars").and(Sort.by(Sort.Direction.DESC, "views")))
                .limit(limit);

        List<Course> recommended = mongoTemplate.find(query, Course.class);

        if (recommended.isEmpty()) {
            return getTrendingCourses(limit);
        }

        List<CourseResponse> responses = courseMapper.entityToResponse(recommended);
        enrichInstructors(responses);
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getRelatedCourses(String courseId, int limit) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null || course.getCategory() == null) return Collections.emptyList();

        // 1. Fetch same-category courses, sorted by rating, paginated (avoid loading ALL into memory)
        //    Request limit+1 to account for filtering out the current course
        Pageable categoryPageable = PageRequest.of(0, limit + 1,
                Sort.by("avgRatingStars").descending().and(Sort.by("views").descending()));
        List<Course> related = courseRepository.findAllByCategoryIdAndStatus(
                        course.getCategory().getId(), CourseStatus.PUBLISHED, categoryPageable)
                .getContent()
                .stream()
                .filter(c -> !c.getId().equals(courseId))
                .limit(limit)
                .toList();

        List<CourseResponse> responses = courseMapper.entityToResponse(related);
        enrichInstructors(responses);
        return responses;
    }

//    @Override
//    @Transactional(readOnly = true)
//    public CourseResponse getFullDetail(String id) {
//        return detail(id);
//    }



    @Override
    @Transactional
    @CustomCacheEvict(key = "'course:detail:' + #id")
    public void updateStatus(String id, CourseStatus status) {
        Course course = courseRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(AppConstants.Resource_Constants.COURSE, AppConstants.Field_Constants.ID, id));

        validateCourseOwnership(course);

        if (status != null) {
            CourseStatus currentStatus = course.getStatus();

            if (status == CourseStatus.PUBLISHED) {
                throw new AccessDeniedException("Giảng viên không có quyền tự xuất bản khóa học!");
            }
            if (status == CourseStatus.REJECTED) {
                throw new AccessDeniedException("Giảng viên không thể tự thiết lập trạng thái từ chối!");
            }
            if (status == CourseStatus.PENDING &&
                currentStatus != CourseStatus.DRAFT && currentStatus != CourseStatus.REJECTED) {
                throw new InvalidParamException("Chỉ có thể gửi yêu cầu duyệt từ trạng thái Bản nháp hoặc Bị từ chối!");
            }
            if (status == CourseStatus.ARCHIVED && currentStatus != CourseStatus.PUBLISHED) {
                throw new InvalidParamException("Chỉ có thể lưu trữ khóa học đã được xuất bản!");
            }
            if (status == CourseStatus.DRAFT &&
                currentStatus != CourseStatus.ARCHIVED && currentStatus != CourseStatus.REJECTED && currentStatus != CourseStatus.DRAFT) {
                throw new InvalidParamException("Chỉ có thể khôi phục/đưa về Bản nháp từ trạng thái Đã lưu trữ hoặc Bị từ chối!");
            }

            course.setStatus(status);
        }
        courseRepository.save(course);

        publishSearchSyncEvent(course, com.hust.commonlibrary.enums.SyncAction.UPDATE);

        logAction(course.getId(), ActionLogType.UPDATE, "Course status updated to " + status + " by instructor", FunctionType.COURSE);

        if (status == CourseStatus.PENDING || status == CourseStatus.PUBLISHED || status == CourseStatus.REJECTED) {
            eventPublisher.publishEvent(new com.hust.courseservice.event.CourseStatusChangedLocalEvent(course, status));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllActiveCourses() {
        List<CourseResponse> responses = courseMapper.entityToResponse(courseRepository.findAllByStatus(CourseStatus.PUBLISHED));
        enrichInstructors(responses);
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public ListResponse<ActionLogResponse> getHistories(String id, int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<ActionLog> logPage = actionLogRepository.findAllByCourseIdOrderByCreatedAtDesc(id, pageable);

        List<ActionLogResponse> responses = logPage.getContent().stream()
                .map(log -> ActionLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .createdByName(log.getCreatedByName())
                        .courseId(log.getCourseId())
                        .sectionId(log.getSectionId())
                        .lessonId(log.getLessonId())
                        .categoryId(log.getCategoryId())
                        .description(log.getDescription())
                        .type(log.getType())
                        .functionType(log.getFunctionType())
                        .createdAt(log.getCreatedAt())
                        .updatedAt(log.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        enrichActionLogInstructors(responses);

        return ListResponse.<ActionLogResponse, ActionLog>of(responses, logPage);
    }

    private void enrichActionLogInstructors(List<ActionLogResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<String> instructorIds = responses.stream()
                .map(ActionLogResponse::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (instructorIds.isEmpty()) return;

        List<String> redisKeys = instructorIds.stream()
                .map(RedisPrefixConstants::getSharedUserProfileKey)
                .toList();

        try {
            List<Object> cachedProfiles = redisService.multiGet(redisKeys);
            Map<String, UserSharedProfile> profileMap = new java.util.HashMap<>();

            for (int i = 0; i < instructorIds.size(); i++) {
                String instId = instructorIds.get(i);
                Object val = cachedProfiles != null && i < cachedProfiles.size() ? cachedProfiles.get(i) : null;
                if (val != null) {
                    if (val instanceof UserSharedProfile profile) {
                        profileMap.put(instId, profile);
                    } else {
                        UserSharedProfile profile = new com.fasterxml.jackson.databind.ObjectMapper()
                                .convertValue(val, UserSharedProfile.class);
                        profileMap.put(instId, profile);
                    }
                }
            }

            for (ActionLogResponse res : responses) {
                if (res.getUserId() != null) {
                    res.setInstructor(profileMap.get(res.getUserId()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich action log instructor profiles from Redis: {}", e.getMessage());
        }
    }

    private void logAction(String courseId,
                          ActionLogType type,
                          String description,
                          FunctionType functionType) {
        String userId = SecurityUtils.getCurrentUserId().orElse("SYSTEM");

        ActionLog actionLog = ActionLog.builder()
                .courseId(courseId)
                .userId(userId)
                .type(type)
                .description(description)
                .functionType(functionType)
                .build();

        actionLogRepository.save(actionLog);
    }

    private String toSlug(String input) {
        if (input == null || input.isBlank()) return "";
        // 1. Normalize NFD to decompose Vietnamese diacritics (e.g., "ă" -> "a" + combining mark)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        // 2. Strip all combining diacritical marks (\p{M} matches combining marks)
        String stripped = Pattern.compile("\\p{M}").matcher(normalized).replaceAll("");
        // 3. Replace "đ/Đ" explicitly (not decomposed by NFD)
        stripped = stripped.replace('đ', 'd').replace('Đ', 'D');
        // 4. Replace whitespace with hyphens
        String nowhitespace = Pattern.compile("\\s+").matcher(stripped).replaceAll("-");
        // 5. Remove any remaining non-word, non-hyphen characters
        String slug = Pattern.compile("[^\\w-]").matcher(nowhitespace).replaceAll("");
        // 6. Collapse multiple consecutive hyphens
        slug = Pattern.compile("-{2,}").matcher(slug).replaceAll("-");
        // 7. Trim leading/trailing hyphens
        slug = slug.replaceAll("^-|-$", "");
        return slug.toLowerCase(Locale.ENGLISH);
    }

    private void validateCourseOwnership(Course course) {
        String currentUserId = SecurityUtils.getCurrentUserIdOrThrow();
        if (!currentUserId.equals(course.getInstructorId())) {
            throw new AccessDeniedException("You do not have permission to perform this action on this course!");
        }
    }

    private void validatePrice(Double price, Double finalPrice) {
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Course price cannot be negative");
        }
        if (finalPrice != null && finalPrice < 0) {
            throw new IllegalArgumentException("Course final price cannot be negative");
        }
        if (price != null && finalPrice != null && finalPrice > price) {
            throw new IllegalArgumentException("Final price cannot be greater than original price");
        }
    }

    private void publishSearchSyncEvent(Course course, com.hust.commonlibrary.enums.SyncAction action) {
        try {
            com.hust.commonlibrary.event.CourseSearchSyncEvent event =
                com.hust.commonlibrary.event.CourseSearchSyncEvent.builder()
                    .id(course.getId())
                    .name(course.getName())
                    .subTitle(course.getSubTitle())
                    .description(course.getDescription())
                    .price(course.getPrice())
                    .finalPrice(course.getFinalPrice())
                    .avgRatingStars(course.getAvgRatingStars())
                    .studentCount(course.getStudentCount())
                    .numOfReviews(course.getNumOfReviews())
                    .level(course.getLevel() != null ? course.getLevel().name() : null)
                    .status(course.getStatus() != null ? com.hust.commonlibrary.enums.CourseSyncStatus.valueOf(course.getStatus().name()) : null)
                    .instructorId(course.getInstructorId())
                    .categoryId(course.getCategory() != null ? course.getCategory().getId() : null)
                    .categoryName(course.getCategory() != null ? course.getCategory().getName() : null)
                    .action(action)
                    .createdAt(course.getCreatedAt())
                    .build();
            eventPublisher.publishEvent(event);
            log.info("Published Spring Application Event for CourseSearchSyncEvent: courseId={}, action={}", course.getId(), action);
        } catch (Exception e) {
            log.error("Failed to publish Spring Application Event for CourseSearchSyncEvent: courseId={}, error={}", course.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCoursesForInstructor() {
        String instructorId = SecurityUtils.getCurrentUserIdOrThrow();
        log.info("Fetching all courses for instructor: {}", instructorId);
        List<Course> courses = courseRepository.findAllByInstructorId(instructorId);
        List<CourseResponse> responses = courseMapper.entityToResponse(courses);
        enrichInstructors(responses);
        enrichCourseCounts(responses);
        return responses;
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'course:detail:' + #id")
    public void adminUpdateStatus(String id, CourseStatus status, String note) {
        Course course = courseRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException(AppConstants.Resource_Constants.COURSE, AppConstants.Field_Constants.ID, id));

        if (status == null) {
            throw new InvalidParamException("Target status cannot be null");
        }

        CourseStatus currentStatus = course.getStatus();

        // 1. Chỉ cho phép Duyệt xuất bản (PUBLISHED) khi khóa học đang ở trạng thái Chờ duyệt (PENDING)
        if (status == CourseStatus.PUBLISHED && currentStatus != CourseStatus.PENDING) {
            throw new InvalidParamException(
                    "Chỉ có thể duyệt xuất bản khóa học đang ở trạng thái Chờ duyệt! Trạng thái hiện tại: " + currentStatus);
        }

        // 2. Chỉ cho phép Từ chối/Gỡ bỏ (REJECTED) khi khóa học đang ở trạng thái Chờ duyệt (PENDING) hoặc Đã xuất bản (PUBLISHED)
        if (status == CourseStatus.REJECTED && currentStatus != CourseStatus.PENDING && currentStatus != CourseStatus.PUBLISHED) {
            throw new InvalidParamException(
                    "Chỉ có thể từ chối hoặc gỡ bỏ khóa học đang ở trạng thái Chờ duyệt hoặc Đã xuất bản! Trạng thái hiện tại: " + currentStatus);
        }

        course.setStatus(status);
        if (status == CourseStatus.REJECTED) {
            course.setNote(note);
        } else {
            course.setNote(null);
        }
        courseRepository.save(course);

        publishSearchSyncEvent(course, com.hust.commonlibrary.enums.SyncAction.UPDATE);

        logAction(course.getId(), ActionLogType.UPDATE, "Course status updated to " + status + " by admin" + (status == CourseStatus.REJECTED ? " with note: " + note : ""), FunctionType.COURSE);

        if (status == CourseStatus.PENDING || status == CourseStatus.PUBLISHED || status == CourseStatus.REJECTED) {
            eventPublisher.publishEvent(new com.hust.courseservice.event.CourseStatusChangedLocalEvent(course, status));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getBulkViews(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Map.of();
        }

        // Initialize all requested keys to 0 to prevent missing keys on caller's side
        Map<String, Integer> viewsMap = new HashMap<>();
        for (String id : courseIds) {
            if (id != null) {
                viewsMap.put(id, 0);
            }
        }

        // 1. Fetch from MongoDB
        List<Course> courses = courseRepository.findAllById(courseIds);
        for (Course course : courses) {
            viewsMap.put(course.getId(), course.getViews() != null ? course.getViews() : 0);
        }

        // 2. Fetch from Redis
        List<String> redisKeys = courseIds.stream()
                .map(id -> "course:views:count:" + id)
                .toList();

        List<Object> redisValues = redisService.multiGet(redisKeys);

        // 3. Merge views (MongoDB views + unsynced Redis views)
        for (int i = 0; i < courseIds.size(); i++) {
            String courseId = courseIds.get(i);
            if (courseId == null) {
                continue;
            }
            Object val = redisValues.get(i);
            if (val != null) {
                int unsyncedViews = 0;
                if (val instanceof Number) {
                    unsyncedViews = ((Number) val).intValue();
                } else {
                    try {
                        unsyncedViews = Integer.parseInt(val.toString());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid redis view count format for course {}: {}", courseId, val);
                    }
                }
                int currentViews = viewsMap.getOrDefault(courseId, 0);
                viewsMap.put(courseId, currentViews + unsyncedViews);
            }
        }

        return viewsMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getBulkCourses(List<String> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Course> courses = courseRepository.findAllById(courseIds);
        if (courses.isEmpty()) {
            return Collections.emptyList();
        }

        List<CourseResponse> responses = courseMapper.entityToResponse(courses);
        enrichInstructors(responses);
        enrichCourseCounts(responses);

        Map<String, CourseResponse> responseMap = responses.stream()
                .collect(Collectors.toMap(CourseResponse::getId, r -> r));

        return courseIds.stream()
                .map(responseMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private void enrichCourseCounts(List<CourseResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<String> courseIds = responses.stream()
                .map(CourseResponse::getId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (courseIds.isEmpty()) return;

        Map<String, Integer> viewsMap = getBulkViews(courseIds);

        List<Lesson> allLessons = lessonRepository.findAllByCourseIdIn(courseIds);
        Map<String, List<Lesson>> lessonsByCourseId = allLessons.stream()
                .filter(l -> l.getCourseId() != null)
                .collect(Collectors.groupingBy(Lesson::getCourseId));

        for (CourseResponse res : responses) {
            String cid = res.getId();
            res.setViews(viewsMap.getOrDefault(cid, res.getViews() != null ? res.getViews() : 0));

            List<Lesson> courseLessons = lessonsByCourseId.getOrDefault(cid, Collections.emptyList());
            res.setLessonCount(courseLessons.size());

            double totalLength = courseLessons.stream()
                    .filter(l -> l.getVideoLength() != null)
                    .mapToDouble(Lesson::getVideoLength)
                    .sum();
            res.setTotalVideosLength(totalLength);
        }
    }

    private void enrichInstructors(List<CourseResponse> responses) {
        if (responses == null || responses.isEmpty()) return;

        List<String> instructorIds = responses.stream()
                .map(CourseResponse::getInstructorId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        if (instructorIds.isEmpty()) return;

        List<String> redisKeys = instructorIds.stream()
                .map(RedisPrefixConstants::getSharedUserProfileKey)
                .toList();

        try {
            List<Object> cachedProfiles = redisService.multiGet(redisKeys);
            Map<String, UserSharedProfile> profileMap = new java.util.HashMap<>();

            for (int i = 0; i < instructorIds.size(); i++) {
                String instId = instructorIds.get(i);
                Object val = cachedProfiles != null && i < cachedProfiles.size() ? cachedProfiles.get(i) : null;
                if (val != null) {
                    if (val instanceof UserSharedProfile profile) {
                        profileMap.put(instId, profile);
                    } else {
                        UserSharedProfile profile = new com.fasterxml.jackson.databind.ObjectMapper()
                                .convertValue(val, UserSharedProfile.class);
                        profileMap.put(instId, profile);
                    }
                }
            }

            for (CourseResponse res : responses) {
                if (res.getInstructorId() != null) {
                    res.setInstructor(profileMap.get(res.getInstructorId()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to enrich course instructor profiles from Redis: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AdminCourseStatsResponse getAdminCourseStats() {
        List<AdminCourseStatsResponse> results;
        if (SecurityUtils.isAdmin()) {
            results = courseRepository.getAdminCourseStatsAggregation();
        } else {
            String instructorId = SecurityUtils.getCurrentUserIdOrThrow();
            results = courseRepository.getInstructorCourseStatsAggregation(instructorId);
        }
        if (results == null || results.isEmpty()) {
            return AdminCourseStatsResponse.builder().build();
        }
        return results.get(0);
    }

    @Override
    @Transactional
    @CustomCacheEvict(key = "'course:detail:' + #courseId")
    public CourseResponse linkFinalExam(String courseId, String finalExamId, String instructorId) {
        log.info("Linking final exam: courseId={}, finalExamId={}, instructorId={}", courseId, finalExamId, instructorId);
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (!course.getInstructorId().equals(instructorId) && !SecurityUtils.isAdmin()) {
            throw new AccessDeniedException("You are not the instructor of this course");
        }

        if (finalExamId == null || finalExamId.trim().isEmpty()) {
            throw new InvalidParamException("Final exam ID must not be empty");
        }

        if (course.getFinalExamId() != null && !course.getFinalExamId().isEmpty()) {
            if (course.getFinalExamId().equals(finalExamId)) {
                return courseMapper.entityToResponse(course);
            }
            throw new InvalidParamException("Khóa học này đã liên kết đề thi cuối khóa và không thể thay đổi!");
        }

        course.setFinalExamId(finalExamId);
        Course saved = courseRepository.save(course);

        eventPublisher.publishEvent(CourseFinalExamLinkedEvent.builder()
                .courseId(courseId)
                .finalExamId(finalExamId)
                .action(com.hust.commonlibrary.enums.LinkAction.LINK)
                .build());

        return courseMapper.entityToResponse(saved);
    }

    @Override
    public List<CourseResponse> getDiscountedCourses(java.time.Instant currentTime) {
        java.time.Instant startDate = currentTime.minus(30, java.time.temporal.ChronoUnit.DAYS);
        List<Course> courses = courseRepository.findDiscountedCoursesWithinTime(startDate, currentTime);

        // Sort by discount percentage descending, limit to top 10
        List<Course> topDiscounted = courses.stream()
                .sorted((a, b) -> {
                    double discountA = (a.getPrice() != null && a.getPrice() > 0 && a.getFinalPrice() != null)
                            ? (a.getPrice() - a.getFinalPrice()) / a.getPrice() : 0;
                    double discountB = (b.getPrice() != null && b.getPrice() > 0 && b.getFinalPrice() != null)
                            ? (b.getPrice() - b.getFinalPrice()) / b.getPrice() : 0;
                    return Double.compare(discountB, discountA);
                })
                .limit(10)
                .toList();

        List<CourseResponse> responses = courseMapper.entityToResponse(topDiscounted);
        enrichInstructors(responses);
        return responses;
    }
}

