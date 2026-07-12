package com.hust.commonlibrary.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sự kiện phát ra từ worker-service khi đã hoàn thành sinh PDF chứng chỉ và upload lên MinIO.
 * learning-service lắng nghe để cập nhật đường dẫn lưu trữ công khai.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateGeneratedEvent {
    private String certificateId;
    private String certificateUrl;
    private Boolean isSuccess;
}
