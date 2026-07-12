package com.hust.commonlibrary.resolver;

/**
 * Interface cầu nối cho phép AOP tự động tra cứu Course ID tương ứng với ID bản ghi cục bộ.
 * Mỗi microservice sẽ tự triển khai Interface này cho từng thực thể (Entity) của họ.
 */
public interface CourseIdResolver {
    /**
     * Tìm kiếm Course ID thật từ Database bằng ID cục bộ.
     * @param domainId ID của Assignment, Discussion, v.v.
     * @return Course ID nếu tìm thấy, null nếu không hợp lệ.
     */
    String resolveCourseId(String domainId);
}
