package com.hust.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    
    private String courseId;
    
    private String name;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal finalPrice;
    
    private String thumbnail;
}
