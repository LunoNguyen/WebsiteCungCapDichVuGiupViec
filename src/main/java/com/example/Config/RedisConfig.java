package com.example.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cấu hình Redis làm bộ nhớ đệm (cache) dữ liệu.
 *
 * - Key lưu dạng chuỗi dễ đọc: neatify:<tên cache>::<khóa>  (xem bằng redis-cli: KEYS neatify:*)
 * - Value lưu bằng Java serialization: giữ nguyên kiểu dữ liệu (Long, BigDecimal, LocalDate,
 *   Map có khóa số...) nên dữ liệu đọc lại từ cache giống hệt dữ liệu gốc.
 *   Vì vậy mọi đối tượng đưa vào cache phải implements Serializable.
 * - Mỗi vùng cache có thời gian sống (TTL) riêng, chỉnh trong application.properties.
 * - Redis tắt hoặc lỗi: ghi log cảnh báo, tạm bỏ qua cache và đọc thẳng từ MySQL (CacheManagerAnToan).
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${app.cache.key-prefix:neatify:}")
    private String keyPrefix;

    @Value("${app.cache.ttl.mac-dinh:10}")
    private long ttlMacDinh;

    @Value("${app.cache.ttl.danh-muc-dich-vu:60}")
    private long ttlDanhMuc;

    @Value("${app.cache.ttl.khu-vuc:360}")
    private long ttlKhuVuc;

    @Value("${app.cache.ttl.khuyen-mai:10}")
    private long ttlKhuyenMai;

    @Value("${app.cache.ttl.danh-gia:10}")
    private long ttlDanhGia;

    @Value("${app.cache.ttl.thong-ke:5}")
    private long ttlThongKe;

    /** Số giây bỏ qua Redis sau khi gặp lỗi kết nối, trước khi thử lại. */
    @Value("${app.cache.tam-ngung-khi-loi:30}")
    private long tamNgungKhiLoi;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Dùng class loader của ứng dụng để không lỗi ClassCastException khi DevTools restart
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        RedisCacheConfiguration macDinh = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(ttlMacDinh))
                .disableCachingNullValues()
                .prefixCacheNameWith(keyPrefix)
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        new JdkSerializationRedisSerializer(classLoader)));

        Map<String, RedisCacheConfiguration> cauHinh = new LinkedHashMap<>();
        cauHinh.put(CacheNames.DANH_MUC_DICH_VU, macDinh.entryTtl(Duration.ofMinutes(ttlDanhMuc)));
        cauHinh.put(CacheNames.DICH_VU, macDinh.entryTtl(Duration.ofMinutes(ttlDanhMuc)));
        cauHinh.put(CacheNames.LOAI_DICH_VU, macDinh.entryTtl(Duration.ofMinutes(ttlDanhMuc)));
        cauHinh.put(CacheNames.KHU_VUC, macDinh.entryTtl(Duration.ofMinutes(ttlKhuVuc)));
        cauHinh.put(CacheNames.KHUYEN_MAI, macDinh.entryTtl(Duration.ofMinutes(ttlKhuyenMai)));
        cauHinh.put(CacheNames.DANH_GIA, macDinh.entryTtl(Duration.ofMinutes(ttlDanhGia)));
        cauHinh.put(CacheNames.THONG_KE, macDinh.entryTtl(Duration.ofMinutes(ttlThongKe)));

        log.info("Redis cache: {} vùng cache, tiền tố key '{}'", cauHinh.size(), keyPrefix);

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(macDinh)
                .withInitialCacheConfigurations(cauHinh)
                .build();
        redisCacheManager.afterPropertiesSet();

        // Bọc lại để Redis tắt/lỗi không làm chậm hay làm hỏng trang (xem CacheManagerAnToan)
        return new CacheManagerAnToan(redisCacheManager, tamNgungKhiLoi);
    }

}
