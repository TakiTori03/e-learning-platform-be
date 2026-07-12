package com.hust.orderservice.constant;

public enum OrderStatus {
    PENDING,            // Đang chờ thanh toán
    PAYMENT_SUCCESS,    // Đã nhận thông báo thanh toán thành công, chờ mở khóa học
    COMPLETED,          // Đã thanh toán và đã mở khóa học thành công (Trạng thái cuối cùng)
    CANCELLED,          // Người dùng chủ động hủy hoặc quá hạn thanh toán
    FAILED,             // Lỗi trong quá trình xử lý mở khóa học (Cần can thiệp)
    REFUNDED            // Đã hoàn tiền cho khách hàng
}
