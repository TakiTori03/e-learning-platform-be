# Hướng Dẫn Sử Dụng Bộ Công Cụ Kiểm Thử Hiệu Năng & Đồng Bộ

Thư mục này chứa 2 kịch bản đo đạc phục vụ cho báo cáo thực nghiệm (Mục 5.8.3):

1. **`k6-load-test.js`**: Kịch bản kiểm thử tải (Load Test) đo đếm Latency tìm kiếm ngữ nghĩa và suggest.
2. **`sync-latency-test.py`**: Kịch bản đo độ trễ đồng bộ hướng sự kiện (Convergence Latency) từ MongoDB sang Elasticsearch qua Kafka.

---

## ⚡ 1. Kiểm thử tải tìm kiếm (`k6-load-test.js`)

Kịch bản này sử dụng công cụ **k6** để giả lập số lượng học viên thực hiện tìm kiếm đồng thời thông qua API Gateway.

### Hướng dẫn chạy:
1. Cài đặt k6 trên máy:
   - Trên Windows (dùng winget hoặc choco):
     ```bash
     winget install grafana.k6
     ```
2. Chạy thử nghiệm bằng lệnh:
   ```bash
   k6 run k6-load-test.js
   ```
3. Xem kết quả đo đếm:
   - Chú ý vào dòng `http_req_duration` ở kết quả đầu ra:
     - `p(95)`: Độ trễ của 95% yêu cầu (trong báo cáo là `< 15ms`).
     - `p(99)`: Độ trễ của 99% yêu cầu (trong báo cáo là `< 30ms`).

---

## 🔄 2. Đo đạc độ trễ đồng bộ sự kiện (`sync-latency-test.py`)

Kịch bản này tạo một khóa học ngẫu nhiên qua HTTP POST của API Gateway, sau đó liên tục thăm dò (poll) qua API Elasticsearch mỗi 50ms cho đến khi thấy kết quả, ghi nhận chênh lệch thời gian ($T_1 - T_0$).

### Hướng dẫn chạy:
1. Đăng nhập hệ thống trên trình duyệt Frontend, mở cửa sổ Developer Tools (F12) -> tab Network hoặc Storage và lấy chuỗi **Access Token JWT** của tài khoản Giảng viên hoặc Admin.
2. Cài đặt thư viện Python cần thiết:
   ```bash
   pip install requests
   ```
3. Chạy kiểm tra:
   ```bash
   python sync-latency-test.py <ACCESS_TOKEN_JWT_CỦA_BẠN>
   ```
4. Mẫu kết quả in ra:
   ```text
   🚀 [Bước 1] Tiến hành gọi API tạo khóa học mới...
   ✅ Khóa học đã được lưu vào cơ sở dữ liệu MongoDB.
   🔄 [Bước 2] Liên tục gửi yêu cầu truy vấn qua Elasticsearch để tính thời gian hội tụ...

   🏆 KẾT QUẢ THỰC NGHIỆM ĐỒNG BỘ:
   ⏱️ Thời điểm tạo (MongoDB): 03:04:15.120
   ⏱️ Thời điểm xuất hiện (ES): 03:04:15.340
   ⚡ Độ trễ đồng bộ (Convergence Latency): 220.00 ms
   ```
