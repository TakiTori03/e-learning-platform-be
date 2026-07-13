FROM python:3.10-slim
ENV PYTHONUNBUFFERED=1

# Cài đặt các thư viện hệ thống cần thiết
RUN apt-get update && apt-get install -y \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8091

ENV WHISPER_MODEL=base
ENV WHISPER_DEVICE=cpu
ENV WHISPER_COMPUTE_TYPE=int8
ENV MINIO_ENDPOINT=http://minio:9000
ENV MINIO_ACCESS_KEY=minioadmin
ENV MINIO_SECRET_KEY=minioadmin
ENV MINIO_BUCKET=elearning-media
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:29092
ENV KAFKA_GROUP_ID=stt-service-group
ENV STT_REQUESTS_TOPIC=stt-requests-topic
ENV STT_RESULTS_TOPIC=stt-results-topic

CMD ["uvicorn", "app:app", "--host", "0.0.0.0", "--port", "8091"]
