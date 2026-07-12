package com.hust.orderservice.constant;

public enum PaymentStatus {
    PENDING,    // Giao dịch vừa khởi tạo ở phía chúng ta
    SUCCESS,    // VNPay/PayPal xác nhận trừ tiền thành công (Result 00)
    FAILED,     // VNPay xác nhận lỗi (Mất kết nối ngân hàng, sai OTP...)
    REFUNDED    // Giao dịch đã được đảo (hoàn tiền) về ví người dùng
}
