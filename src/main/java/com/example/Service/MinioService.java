package com.example.Service;

import io.minio.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO Storage Service
 * Cung cấp các chức năng upload, download, xóa file
 * cho toàn hệ thống Neatify.
 *
 * Bucket: giup-viec-storage
 * Các thư mục (prefix) theo loại file:
 *   - ctv/chung-chi/         → Chứng chỉ CTV
 *   - ctv/avatar/            → Ảnh đại diện CTV
 *   - khach-hang/avatar/     → Ảnh đại diện khách hàng
 *   - don-dat/tai-lieu/      → Tài liệu đính kèm đơn dịch vụ
 *   - khieu-nai/tai-lieu/    → Tài liệu khiếu nại
 *   - ho-so/                 → Hồ sơ nhân viên
 */
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // ── Upload file ────────────────────────────────────────────────────────
    /**
     * Upload file lên MinIO, tự sinh UUID tên file để tránh trùng.
     *
     * @param file       MultipartFile từ form
     * @param folderPath Thư mục lưu (VD: "ctv/chung-chi/")
     * @return Object name đã lưu trong bucket (dùng để download sau)
     */
    public String uploadFile(MultipartFile file, String folderPath) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.'))
                : "";
        String objectName = folderPath + UUID.randomUUID() + ext;

        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType() != null
                                    ? file.getContentType() : "application/octet-stream")
                            .build()
            );
            log.info("MinIO upload OK: {}/{}", bucketName, objectName);
            return objectName;

        } catch (Exception e) {
            log.error("MinIO upload thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể upload file lên MinIO: " + e.getMessage(), e);
        }
    }

    // ── Upload file với tên tùy chỉnh ────────────────────────────────────
    public String uploadFile(MultipartFile file, String folderPath, String customFileName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File không được rỗng");
        }
        String objectName = folderPath + customFileName;
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            .contentType(file.getContentType() != null
                                    ? file.getContentType() : "application/octet-stream")
                            .build()
            );
            log.info("MinIO upload OK (custom name): {}/{}", bucketName, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("MinIO upload thất bại: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể upload file: " + e.getMessage(), e);
        }
    }

    // ── Download file (lấy InputStream) ──────────────────────────────────
    /**
     * Lấy InputStream của file trong MinIO để stream về client.
     *
     * @param objectName Object name (đường dẫn trong bucket)
     * @return InputStream
     */
    public InputStream downloadFile(String objectName) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO download thất bại ({}): {}", objectName, e.getMessage(), e);
            throw new RuntimeException("Không thể tải file từ MinIO: " + e.getMessage(), e);
        }
    }

    // ── Lấy URL truy cập công khai tạm thời (Presigned URL) ─────────────
    /**
     * Tạo presigned URL có hiệu lực trong 1 giờ để client truy cập file.
     *
     * @param objectName Object name trong bucket
     * @return URL có thể truy cập trong 1 giờ
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO presigned URL thất bại ({}): {}", objectName, e.getMessage(), e);
            return endpoint + "/" + bucketName + "/" + objectName;
        }
    }

    // ── Lấy URL công khai (bucket phải ở chế độ public) ─────────────────
    public String getPublicUrl(String objectName) {
        if (objectName == null || objectName.isBlank()) return null;
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    // ── Xóa file ─────────────────────────────────────────────────────────
    /**
     * Xóa file khỏi MinIO bucket.
     *
     * @param objectName Object name cần xóa
     */
    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isBlank()) return;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("MinIO xóa OK: {}/{}", bucketName, objectName);
        } catch (Exception e) {
            log.error("MinIO xóa thất bại ({}): {}", objectName, e.getMessage(), e);
        }
    }

    // ── Kiểm tra file tồn tại ────────────────────────────────────────────
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Folder prefix constants ──────────────────────────────────────────
    /** Prefix cho các loại file thường dùng */
    public static final class Folder {
        public static final String CTV_CHUNG_CHI    = "ctv/chung-chi/";
        public static final String CTV_AVATAR        = "ctv/avatar/";
        public static final String KH_AVATAR         = "khach-hang/avatar/";
        public static final String DON_DAT_TAILIEU   = "don-dat/tai-lieu/";
        public static final String KHIEU_NAI_TAILIEU = "khieu-nai/tai-lieu/";
        public static final String HO_SO             = "ho-so/";
        public static final String NHAN_VIEN_AVATAR  = "nhan-vien/avatar/";

        private Folder() {}
    }
}
