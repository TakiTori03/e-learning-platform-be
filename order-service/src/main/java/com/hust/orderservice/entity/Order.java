package com.hust.orderservice.entity;

import com.hust.commonlibrary.entity.BaseEntity;

import com.hust.orderservice.constant.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_user_id", columnList = "userId"),
    @Index(name = "idx_order_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity<String> {

    @PrePersist
    public void ensureId() {
        if (this.getId() == null) {
            this.setId(java.util.UUID.randomUUID().toString());
        }
    }

    @Column(nullable = false)
    private String userId;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalPrice;
    
    @Column(precision = 19, scale = 2)
    private BigDecimal vatFee;
    
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private OrderStatus status;

    private String transactionMethod;
    private String transactionNo;

    @ElementCollection
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items;
}
