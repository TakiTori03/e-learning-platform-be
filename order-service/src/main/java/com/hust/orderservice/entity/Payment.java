package com.hust.orderservice.entity;

import com.hust.commonlibrary.entity.BaseEntity;

import com.hust.orderservice.constant.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_transaction_id", columnList = "transactionId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity<String> {

    @PrePersist
    public void ensureId() {
        if (this.getId() == null) {
            this.setId(java.util.UUID.randomUUID().toString());
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private String transactionId; // Mã giao dịch của Gateway (vnp_TransactionNo)
    private String method;        // Phương thức: vnpay, paypal, v.v.
    
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;    // Tiền tệ dùng BigDecimal để đảm bảo độ chính xác
    
    private String bankCode;
    private String bankTranNo;
    private String cardType;
    
    @Column(length = 255)
    private String orderInfo;
    
    private String vnpTxnRef;     // Mã tham chiếu đơn hàng gửi sang VNPay
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PaymentStatus status;

    private Instant payDate;      // Sử dụng Instant để đồng bộ Timezone UTC
}
