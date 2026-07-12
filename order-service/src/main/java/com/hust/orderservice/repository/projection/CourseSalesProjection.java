package com.hust.orderservice.repository.projection;

import java.math.BigDecimal;

public interface CourseSalesProjection {
    String getCourseId();
    String getCourseName();
    Long getSales();
    BigDecimal getRevenue();
}
