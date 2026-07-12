package com.hust.workerservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VideoProcessingService {

    @Value("${app.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${app.ffprobe.path:ffprobe}")
    private String ffprobePath;

    @Value("${app.storage.local-path:./uploads}")
    private String baseStoragePath;

    public Double getVideoDuration(File inputFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    inputFile.getAbsolutePath()
            );
            Process process = pb.start();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null) {
                    return Double.parseDouble(line.trim());
                }
            }
            process.waitFor();
        } catch (Exception e) {
            log.warn("⚠️ Failed to parse duration using ffprobe: {}. Returning 0.0 as default.", e.getMessage());
        }
        return 0.0;
    }

    public String processToHls(File inputFile, String outputDirName) throws IOException, InterruptedException {
        Path outputDir = Paths.get(baseStoragePath, "hls", outputDirName);
        Files.createDirectories(outputDir);

        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 16); // 16 bytes for AES-128
        File keyFile = new File(outputDir.toFile(), "video.key");
        Files.write(keyFile.toPath(), key.getBytes());

        File keyInfoFile = createKeyInfoFile(outputDir.toFile(), keyFile.getAbsolutePath(), outputDirName);

        String m3u8Output = new File(outputDir.toFile(), "playlist.m3u8").getAbsolutePath();

        // Chạy trực tiếp mã hóa CPU siêu nhanh (Ultrafast Re-encode) cho cả Video và Audio
        // Điều này bắt buộc để FFmpeg tự động đồng bộ hóa mốc thời gian (timestamps) giữa luồng hình và tiếng,
        // khắc phục triệt để lỗi mất âm thanh và màn hình đen (mất đồng bộ A/V) khi chơi trên trình duyệt.
        boolean encodeSuccess = runFfmpegCmd(new String[]{
                ffmpegPath,
                "-nostdin",
                "-loglevel", "error",
                "-i", inputFile.getAbsolutePath(),
                "-c:v", "libx264",
                "-preset", "ultrafast",            // Tốc độ nén siêu tốc
                "-tune", "zerolatency",
                "-threads", "0",                    // Tận dụng toàn bộ luồng CPU (Ryzen 5800H 16 threads)
                "-c:a", "aac",                      // Ép âm thanh chuẩn AAC tương thích 100% web
                "-profile:v", "main",               // Profile phổ thông cho mọi thiết bị
                "-level", "3.0",
                "-start_number", "0",
                "-hls_time", "10",
                "-hls_list_size", "0",
                "-hls_key_info_file", keyInfoFile.getAbsolutePath(),
                m3u8Output
        }, 15, TimeUnit.MINUTES);

        if (!encodeSuccess) {
            log.error("❌ [HLS Transcode] Chuyển đổi HLS thất bại.");
            throw new RuntimeException("FFmpeg HLS Transcoding failed.");
        }
        log.info("✅ [HLS Transcode] Chuyển đổi HLS hoàn tất thành công!");

        // Post-process the m3u8 playlist to remove duplicate #EXT-X-KEY declarations
        cleanM3u8Playlist(m3u8Output);

        // Return the relative path to the playlist
        return "hls/" + outputDirName + "/playlist.m3u8";
    }

    private boolean runFfmpegCmd(String[] cmd, long timeout, TimeUnit unit) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("[FFmpeg Log] {}", line);
                }
            }

            boolean completed = process.waitFor(timeout, unit);
            if (!completed) {
                log.error("❌ FFmpeg command timed out.");
                process.destroyForcibly();
                return false;
            }

            return process.exitValue() == 0;
        } catch (Exception e) {
            log.error("❌ Error executing FFmpeg command: {}", e.getMessage());
            return false;
        }
    }

    private void cleanM3u8Playlist(String m3u8Path) {
        Path path = Paths.get(m3u8Path);
        if (!Files.exists(path)) {
            return;
        }
        try {
            java.util.List<String> lines = Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
            java.util.List<String> cleanedLines = new java.util.ArrayList<>();
            boolean hasKeyLine = false;
            for (String line : lines) {
                if (line.startsWith("#EXT-X-KEY")) {
                    if (!hasKeyLine) {
                        cleanedLines.add(line);
                        hasKeyLine = true;
                    }
                    // skip subsequent key lines
                } else {
                    cleanedLines.add(line);
                }
            }
            Files.write(path, cleanedLines, java.nio.charset.StandardCharsets.UTF_8);
            log.info("🧹 Successfully cleaned up repeated #EXT-X-KEY tags from: {}", m3u8Path);
        } catch (IOException e) {
            log.error("❌ Failed to clean playlist.m3u8 file: {}", e.getMessage(), e);
        }
    }

    public String extractAudio(File inputFile, String outputDirName) throws IOException, InterruptedException {
        Path outputDir = Paths.get(baseStoragePath, "hls", outputDirName);
        if (!Files.exists(outputDir)) Files.createDirectories(outputDir);

        // Thay đổi đuôi file trực tiếp sang wav
        String audioName = "audio.wav";
        Path outputPath = outputDir.resolve(audioName);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-nostdin",
                "-loglevel", "error", // Ép FFmpeg câm lặng, chỉ báo lỗi nghiêm trọng, xử lý triệt để đống log Late SEI rác
                "-i", inputFile.getAbsolutePath(),
                "-vn",
                "-acodec", "pcm_s16le", // Trích xuất trực tiếp sang PCM chuỗi gốc 16-bit
                "-ar", "16000",          // Đưa về tần số 16kHz
                "-ac", "1",              // Chuyển về hệ Mono
                outputPath.toAbsolutePath().toString()
        );

        // Không dùng pb.inheritIO() để tránh block luồng hệ thống Java
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Đọc sạch dữ liệu trong buffer ra để tránh treo tiến trình
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) { /* Do nothing - consume buffer */ }
        }

        boolean completed = process.waitFor(5, TimeUnit.MINUTES);
        if (!completed) {
            log.error("❌ FFmpeg audio extraction timed out.");
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg audio extraction timed out.");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed to extract audio, exit code: " + exitCode);
        }

        return "hls/" + outputDirName + "/" + audioName;
    }

    private File createKeyInfoFile(File dir, String keyFilePath, String outputDirName) throws IOException {
        String keyUri = "/api/v1/media/keys/" + outputDirName;
        File keyInfoFile = new File(dir, "keyinfo.txt");
        String normalizedPath = keyFilePath.replace("\\", "/");
        // IV tĩnh 128-bit (32 ký tự hex) để FFmpeg chỉ ghi 1 tag #EXT-X-KEY duy nhất ở đầu playlist
        String iv = "00000000000000000000000000000000";
        String content = keyUri + "\n" + normalizedPath + "\n" + iv;
        Files.write(keyInfoFile.toPath(), content.getBytes());
        return keyInfoFile;
    }
}
