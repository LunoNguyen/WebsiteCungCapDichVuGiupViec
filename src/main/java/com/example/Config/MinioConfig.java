package com.example.Config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO Configuration
 * Kết nối tới MinIO Object Storage tại port 9001
 * Bucket: giup-viec-storage
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // Tự động tạo bucket nếu chưa tồn tại
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO: Đã tạo bucket '{}'", bucketName);
            } else {
                log.info("MinIO: Bucket '{}' đã sẵn sàng.", bucketName);
            }
            return client;

        } catch (Exception e) {
            // Log cảnh báo nhưng KHÔNG crash app nếu MinIO chưa khởi động
            log.warn("MinIO chưa kết nối được ({}). Upload file sẽ không hoạt động cho đến khi MinIO sẵn sàng: {}",
                    endpoint, e.getMessage());
            // Trả về bean với config cơ bản (lazy connect)
            return MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
    }
}
