import http from 'k6/http';
import { check, sleep } from 'k6';

// ── CẤU HÌNH THỜI GIAN VÀ SỐ LƯỢNG NGƯỜI DÙNG GIẢ LẬP ──
export const options = {
    stages: [
        { duration: '15s', target: 50 },   // Tăng nhanh lên 50 VUs (Virtual Users) trong 15s
        { duration: '30s', target: 50 },   // Giữ tải ở 50 VUs trong 30s
        { duration: '15s', target: 100 },  // Tăng vọt lên 100 VUs trong 15s (Stress test nhẹ)
        { duration: '30s', target: 100 },  // Giữ tải ở 100 VUs trong 30s
        { duration: '15s', target: 0 },    // Giảm tải về 0 VUs trong 15s
    ],
    thresholds: {
        // Chỉ tiêu chất lượng (SLA): p95 phải nhỏ hơn 15ms và p99 phải nhỏ hơn 30ms
        http_req_duration: ['p(95)<15', 'p(99)<30'],
    },
};

// URL của API Gateway hoặc gọi trực tiếp course-service
const BASE_URL = __ENV.API_URL || 'http://localhost:8080/course';

export default function () {
    // Danh sách từ khóa ngẫu nhiên để mô phỏng tìm kiếm thực tế của học viên
    const queries = ['java', 'spring', 'python', 'react', 'node', 'web', 'microservices'];
    const query = queries[Math.floor(Math.random() * queries.length)];

    // 1. Giả lập tính năng gợi ý tự động (Search-as-you-type) khi học viên đang gõ chữ
    const typingLength = Math.floor(Math.random() * query.length) + 1;
    const partialQuery = query.substring(0, typingLength);

    const suggestRes = http.get(`${BASE_URL}/courses/suggest?q=${partialQuery}`);
    check(suggestRes, {
        'suggest status is 200': (r) => r.status === 200,
        'suggest response is successful': (r) => r.json('success') === true,
    });

    sleep(0.2); // Học viên mất khoảng 200ms để đọc gợi ý

    // 2. Giả lập bấm Enter để thực hiện truy vấn tìm kiếm đầy đủ
    const searchRes = http.get(`${BASE_URL}/courses/search-elasticsearch?q=${partialQuery}`);
    check(searchRes, {
        'search status is 200': (r) => r.status === 200,
        'search response is successful': (r) => r.json('success') === true,
    });

    sleep(1.5); // Thời gian chờ của học viên trước khi thực hiện thao tác tiếp theo
}
