FROM python:3.10-slim

# Cài đặt các thư viện hệ thống cần thiết cho OpenCV/PaddleOCR
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8090

# Tắt CUDA cho PaddleOCR vì VPS dùng CPU
ENV PADDLE_WITH_CUDA=OFF

# MinIO Configuration
ENV MINIO_ENDPOINT=http://localhost:9100
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=minioadmin
ENV MINIO_BUCKET=elearning-media

# Kafka Configuration
ENV KAFKA_BOOTSTRAP_SERVERS=localhost:9092
ENV KAFKA_GROUP_ID=pdf-parser-service-group
ENV PDF_PARSER_REQUESTS_TOPIC=pdf-parser-requests-topic
ENV PDF_PARSER_RESULTS_TOPIC=pdf-parser-results-topic

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8090"]
