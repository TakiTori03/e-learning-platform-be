import os
import sys
import tempfile
import shutil
import time
import threading
import json
import warnings
import logging

# Tắt log cảnh báo phiền phức từ các thư viện AI và FastAPI
warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", category=UserWarning)
logging.getLogger("huggingface_hub").setLevel(logging.ERROR)

from dotenv import load_dotenv
load_dotenv()


from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse
from pydantic import BaseModel
import boto3
from botocore.client import Config as BotoConfig
from confluent_kafka import Consumer, Producer, KafkaError

app = FastAPI(
    title="GPU Speech-to-Text Service (faster-whisper)",
    description="Dịch vụ bóc băng giọng nói tăng tốc GPU sử dụng faster-whisper (CTranslate2).",
    version="1.0.0"
)

# ==============================================================================
# CẤU HÌNH TỪ BIẾN MÔI TRƯỜNG
# ==============================================================================
MINIO_ENDPOINT = os.getenv("MINIO_ENDPOINT")
MINIO_ACCESS_KEY = os.getenv("MINIO_ACCESS_KEY")
MINIO_SECRET_KEY = os.getenv("MINIO_SECRET_KEY")
MINIO_BUCKET = os.getenv("MINIO_BUCKET")

WHISPER_MODEL = os.getenv("WHISPER_MODEL")
WHISPER_DEVICE = os.getenv("WHISPER_DEVICE")
WHISPER_COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE")

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID")
STT_REQUESTS_TOPIC = os.getenv("STT_REQUESTS_TOPIC")
STT_RESULTS_TOPIC = os.getenv("STT_RESULTS_TOPIC")

# Kiểm tra các biến bắt buộc
required_vars = [
    "MINIO_ENDPOINT", "MINIO_ACCESS_KEY", "MINIO_SECRET_KEY", "MINIO_BUCKET",
    "WHISPER_MODEL", "WHISPER_DEVICE", "WHISPER_COMPUTE_TYPE",
    "KAFKA_BOOTSTRAP_SERVERS", "KAFKA_GROUP_ID", "STT_REQUESTS_TOPIC", "STT_RESULTS_TOPIC"
]
missing_vars = [var for var in required_vars if not os.getenv(var)]
if missing_vars:
    raise RuntimeError(f"❌ Thiếu các biến môi trường bắt buộc: {', '.join(missing_vars)}")


# ==============================================================================
# KHỞI TẠO MINIO CLIENT (S3-Compatible)
# ==============================================================================
s3_client = boto3.client(
    "s3",
    endpoint_url=MINIO_ENDPOINT,
    aws_access_key_id=MINIO_ACCESS_KEY,
    aws_secret_access_key=MINIO_SECRET_KEY,
    region_name="us-east-1",
    config=BotoConfig(signature_version="s3v4"),
)
print(f"✅ MinIO Client khởi tạo thành công: {MINIO_ENDPOINT}/{MINIO_BUCKET}")

# ==============================================================================
# KHỞI TẠO FASTER-WHISPER MODEL (GPU → CPU FALLBACK)
# ==============================================================================
model = None

def load_whisper_model():
    global model
    print(f"🧠 Đang tải mô hình Whisper '{WHISPER_MODEL}' trên thiết bị '{WHISPER_DEVICE}'...")
    try:
        from faster_whisper import WhisperModel
        model = WhisperModel(WHISPER_MODEL, device=WHISPER_DEVICE, compute_type=WHISPER_COMPUTE_TYPE)
        print(f"🚀 Mô hình Whisper tải thành công trên {WHISPER_DEVICE.upper()}!")
    except Exception as gpu_err:
        print(f"⚠️ Không thể tải trên {WHISPER_DEVICE}: {gpu_err}")
        print("🔄 Tự động chuyển sang CPU (compute_type=int8)...")
        try:
            from faster_whisper import WhisperModel
            model = WhisperModel(WHISPER_MODEL, device="cpu", compute_type="int8")
            print("✅ Mô hình Whisper tải thành công trên CPU (fallback).")
        except Exception as cpu_err:
            print(f"❌ Lỗi nghiêm trọng: Không thể tải mô hình Whisper: {cpu_err}")
            model = None

load_whisper_model()

# ==============================================================================
# ĐỊNH DẠNG THỜI GIAN WEBVTT
# ==============================================================================
def format_timestamp(seconds: float) -> str:
    """Chuyển đổi giây (float) sang định dạng WebVTT: HH:MM:SS.mmm"""
    hours = int(seconds // 3600)
    minutes = int((seconds % 3600) // 60)
    secs = int(seconds % 60)
    millis = int((seconds - int(seconds)) * 1000)
    return f"{hours:02d}:{minutes:02d}:{secs:02d}.{millis:03d}"

# ==============================================================================
# REQUEST / RESPONSE MODELS
# ==============================================================================
class TranscribeRequest(BaseModel):
    audio_key: str          # MinIO object key, ví dụ: "temp-stt/abc-123/audio.wav"
    language: str = "auto"  # "auto" = tự phát hiện, hoặc "vi", "en", "ja"...

# ==============================================================================
# API ENDPOINTS
# ==============================================================================
@app.post("/api/v1/stt/transcribe")
async def transcribe(request: TranscribeRequest):
    """
    Bóc băng file âm thanh từ MinIO bằng faster-whisper GPU.
    
    Flow:
    1. Tải file âm thanh từ MinIO (qua object key nội bộ, không qua HTTP body).
    2. Chạy mô hình faster-whisper trên GPU (hoặc CPU fallback).
    3. Chuyển đổi kết quả sang định dạng WebVTT.
    4. Trả về nội dung VTT dạng text.
    """
    if model is None:
        raise HTTPException(status_code=503, detail="Mô hình Whisper chưa sẵn sàng.")

    temp_dir = tempfile.mkdtemp(prefix="stt-")
    start_time = time.time()

    try:
        # 1. Tải file âm thanh từ MinIO
        local_audio_path = os.path.join(temp_dir, "audio.wav")
        print(f"📥 Đang tải file âm thanh từ MinIO: {request.audio_key}")
        
        try:
            s3_client.download_file(MINIO_BUCKET, request.audio_key, local_audio_path)
        except Exception as download_err:
            print(f"❌ Lỗi tải file từ MinIO: {download_err}")
            raise HTTPException(
                status_code=404,
                detail=f"Không thể tải file từ MinIO: {request.audio_key}. Lỗi: {str(download_err)}"
            )

        file_size_mb = os.path.getsize(local_audio_path) / (1024 * 1024)
        print(f"✅ Đã tải thành công: {file_size_mb:.1f} MB")

        # 2. Bóc băng bằng faster-whisper
        lang = None if request.language == "auto" else request.language
        print(f"🎙️ Bắt đầu bóc băng (language={request.language})...")

        segments, info = model.transcribe(
            local_audio_path,
            language=lang,
            beam_size=5,
            vad_filter=True,             # Lọc khoảng lặng tự động (Voice Activity Detection)
            vad_parameters=dict(
                min_silence_duration_ms=500,  # Khoảng lặng tối thiểu 500ms
            ),
        )

        # 3. Xây dựng nội dung WebVTT
        vtt_content = "WEBVTT\n\n"
        segment_count = 0

        for segment in segments:
            start = format_timestamp(segment.start)
            end = format_timestamp(segment.end)
            text = segment.text.strip()

            if text:  # Bỏ qua các đoạn trống
                vtt_content += f"{start} --> {end}\n{text}\n\n"
                segment_count += 1

        elapsed = time.time() - start_time
        print(f"✅ Bóc băng hoàn tất! {segment_count} đoạn, ngôn ngữ: {info.language}, "
              f"thời lượng audio: {info.duration:.1f}s, thời gian xử lý: {elapsed:.1f}s")

        return JSONResponse(status_code=200, content={
            "success": True,
            "vtt": vtt_content,
            "language": info.language,
            "duration": info.duration,
            "segments": segment_count,
            "processingTimeMs": int(elapsed * 1000),
        })

    except HTTPException:
        raise
    except Exception as e:
        elapsed = time.time() - start_time
        print(f"❌ Lỗi bóc băng sau {elapsed:.1f}s: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi bóc băng: {str(e)}")

    finally:
        # Dọn dẹp file tạm
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir, ignore_errors=True)


@app.get("/health")
async def health_check():
    """Kiểm tra trạng thái dịch vụ và mô hình."""
    return {
        "status": "ok" if model is not None else "degraded",
        "model": WHISPER_MODEL,
        "device": WHISPER_DEVICE,
        "minio_endpoint": MINIO_ENDPOINT,
        "minio_bucket": MINIO_BUCKET,
    }


def kafka_consumer_loop():
    print("🚀 Khởi chạy background Kafka Consumer loop cho STT...")
    conf = {
        'bootstrap.servers': KAFKA_BOOTSTRAP_SERVERS,
        'group.id': KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': False, # Commit thủ công để đảm bảo không mất mát tin nhắn
        'max.poll.interval.ms': 900000, # 15 minutes processing timeout
    }
    
    consumer = Consumer(conf)
    consumer.subscribe([STT_REQUESTS_TOPIC])
    
    producer = Producer({'bootstrap.servers': KAFKA_BOOTSTRAP_SERVERS})
    
    while True:
        try:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                else:
                    print(f"❌ Kafka Error: {msg.error()}")
                    continue
            
            payload_str = msg.value().decode('utf-8')
            print(f"📥 [STT Kafka] Nhận tin nhắn mới: {payload_str}")
            try:
                payload = json.loads(payload_str)
            except Exception as parse_err:
                print(f"⚠️ Lỗi parse JSON payload: {parse_err}")
                consumer.commit(msg) # Commit tin nhắn hỏng để qua tin tiếp theo
                continue
                
            media_id = payload.get("mediaId")
            course_id = payload.get("courseId")
            lesson_id = payload.get("lessonId")
            audio_key = payload.get("audioKey")
            language = payload.get("language", "auto")
            
            if not audio_key:
                print("⚠️ Thiếu thông tin audioKey trong payload.")
                consumer.commit(msg)
                continue
                
            temp_dir = tempfile.mkdtemp(prefix="stt-kafka-")
            local_audio_path = os.path.join(temp_dir, "audio.wav")
            
            try:
                # 1. Tải file âm thanh từ MinIO
                print(f"📥 [STT Kafka] Đang tải file WAV từ MinIO key: {audio_key}")
                s3_client.download_file(MINIO_BUCKET, audio_key, local_audio_path)
                
                # 2. Bóc băng
                lang = None if language == "auto" else language
                print(f"🎙️ [STT Kafka] Đang bóc băng bằng GPU/CPU cho key: {audio_key} (lang={language})")
                segments, info = model.transcribe(
                    local_audio_path,
                    language=lang,
                    beam_size=5,
                    vad_filter=True,
                    vad_parameters=dict(min_silence_duration_ms=500),
                )
                
                vtt_content = "WEBVTT\n\n"
                segment_count = 0
                for segment in segments:
                    start = format_timestamp(segment.start)
                    end = format_timestamp(segment.end)
                    text = segment.text.strip()
                    if text:
                        vtt_content += f"{start} --> {end}\n{text}\n\n"
                        segment_count += 1
                
                duration = info.duration
                
                result_payload = {
                    "mediaId": media_id,
                    "courseId": course_id,
                    "lessonId": lesson_id,
                    "vttContent": vtt_content,
                    "hlsFolderName": audio_key.split("/")[1] if "/" in audio_key else "",
                    "duration": duration,
                    "success": True,
                    "errorMessage": None
                }
                print(f"✅ [STT Kafka] Bóc băng xong. Độ dài: {duration}s, {segment_count} segments.")
                
            except Exception as proc_err:
                print(f"❌ Lỗi khi xử lý bóc băng Kafka: {proc_err}")
                result_payload = {
                    "mediaId": media_id,
                    "courseId": course_id,
                    "lessonId": lesson_id,
                    "vttContent": "",
                    "hlsFolderName": audio_key.split("/")[1] if "/" in audio_key else "",
                    "duration": 0.0,
                    "success": False,
                    "errorMessage": str(proc_err)
                }
            finally:
                if os.path.exists(temp_dir):
                    shutil.rmtree(temp_dir, ignore_errors=True)
            
            # Gửi kết quả về stt-results-topic
            try:
                producer.produce(
                    STT_RESULTS_TOPIC,
                    key=lesson_id.encode('utf-8') if lesson_id else None,
                    value=json.dumps(result_payload).encode('utf-8')
                )
                producer.flush()
                print(f"✅ [STT Kafka] Đã đẩy kết quả về topic: {STT_RESULTS_TOPIC}")
                consumer.commit(msg)
            except Exception as prod_err:
                print(f"❌ Lỗi gửi kết quả về Kafka: {prod_err}")
                
        except Exception as e:
            print(f"❌ Lỗi nghiêm trọng trong vòng lặp Kafka Consumer: {e}")
            time.sleep(2)


@app.on_event("startup")
def startup_event():
    kafka_thread = threading.Thread(target=kafka_consumer_loop, daemon=True)
    kafka_thread.start()
    print("🚀 Background Thread cho STT Kafka Consumer đã khởi chạy thành công!")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app:app", host="0.0.0.0", port=8091, reload=True)
