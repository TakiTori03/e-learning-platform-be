package com.hust.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Cart {

    @Id
    @Column(name = "user_id", nullable = false)
    private String userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cart_items",
            joinColumns = @JoinColumn(name = "cart_id")
    )
    @Column(name = "course_id", nullable = false)
    @Builder.Default
    private Set<String> courseIds = new HashSet<>();

    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
