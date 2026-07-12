package com.hust.orderservice.repository.projection;

import java.math.BigDecimal;

public interface RevenueProjection {
    String getPeriod();
    BigDecimal getRevenue();
}
