package com.example.Config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình kết nối MinIO Object Storage.
 *
 * Lưu ý cổng: MinIO có 2 cổng khác nhau
 *   - 9000: cổng API (S3) – ứng dụng phải kết nối vào cổng này
 *   - 9001: cổng Console (giao diện quản trị trên trình duyệt)
 *
 * Bean chỉ khởi tạo client, chưa kết nối mạng, nên ứng dụng vẫn chạy được khi MinIO chưa bật.
 * Việc kiểm tra/tạo bucket nằm trong MinioService.
 */
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
