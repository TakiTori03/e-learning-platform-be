import time
import sys
import uuid
import requests

# ── CẤU HÌNH THÔNG TIN KẾT NỐI ──
GATEWAY_URL = "http://localhost:8080"
COURSE_API_URL = f"{GATEWAY_URL}/course/courses"

def check_sync_latency(token):
    # 1. Sinh dữ liệu khóa học ngẫu nhiên với ID duy nhất
    unique_id = str(uuid.uuid4())[:8]
    course_name = f"Kafka Performance Test Course {unique_id}"
    
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }
    
    course_payload = {
        "name": course_name,
        "subTitle": "This is a temporary course to measure event-driven sync lag.",
        "description": f"Testing the event convergence delay between MongoDB and Elasticsearch. Unique ID: {unique_id}",
        "price": 120.0,
        "finalPrice": 99.0,
        "level": "BEGINNER",
        "category": "6689d02dbf0f353160bb073f"  // Sử dụng một ID danh mục hợp lệ bất kỳ
    }
    
    print(f"🚀 [Bước 1] Tiến hành gọi API tạo khóa học mới...")
    t0 = time.time() * 1000  # Đổi sang milliseconds
    
    try:
        create_res = requests.post(COURSE_API_URL, json=course_payload, headers=headers)
        if create_res.status_code not in (200, 201):
            print(f"❌ Không thể tạo khóa học. HTTP Status: {create_res.status_code}")
            print(f"Chi tiết lỗi: {create_res.text}")
            return
    except Exception as e:
        print(f"❌ Lỗi kết nối đến API Gateway: {str(e)}")
        return
        
    print("✅ Khóa học đã được lưu vào cơ sở dữ liệu MongoDB.")
    print("🔄 [Bước 2] Liên tục gửi yêu cầu truy vấn qua Elasticsearch để tính thời gian hội tụ...")
    
    # 2. Liên tục poll tìm kiếm Elasticsearch qua gRPC (endpoint search-elasticsearch)
    search_url = f"{COURSE_API_URL}/search-elasticsearch"
    params = {"q": unique_id}
    
    found = False
    t1 = 0
    max_retries = 100  # Giới hạn 5 giây (100 * 50ms)
    
    for i in range(max_retries):
        time.sleep(0.05)  # Chờ 50ms trước khi kiểm tra lại
        try:
            search_res = requests.get(search_url, params=params, headers=headers)
            if search_res.status_code == 200:
                data = search_res.json()
                content = data.get("payload", {}).get("content", [])
                if len(content) > 0:
                    t1 = time.time() * 1000
                    found = True
                    break
        except Exception as e:
            # Bỏ qua lỗi kết nối tạm thời
            pass
            
    if found:
        latency = t1 - t0
        print("\n🏆 KẾT QUẢ THỰC NGHIỆM ĐỒNG BỘ:")
        print(f"⏱️ Thời điểm tạo (MongoDB): {time.strftime('%H:%M:%S', time.localtime(t0/1000))}.{int(t0%1000):03d}")
        print(f"⏱️ Thời điểm xuất hiện (ES): {time.strftime('%H:%M:%S', time.localtime(t1/1000))}.{int(t1%1000):03d}")
        print(f"⚡ Độ trễ đồng bộ (Convergence Latency): {latency:.2f} ms")
        print("💡 Kết quả này phản ánh tổng thời gian xử lý sự kiện qua Kafka và chỉ mục hóa trên Elasticsearch.")
    else:
        print("❌ Quá thời gian chờ (5 giây) nhưng khóa học chưa được đồng bộ sang Elasticsearch index.")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("💡 Cách chạy: python sync-latency-test.py <ACCESS_TOKEN_JWT>")
        sys.exit(1)
    
    jwt_token = sys.argv[1]
    check_sync_latency(jwt_token)
