package com.example.Config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 * - Cache TTL mặc định: 10 phút
 * - Cache riêng cho từng domain với TTL tuỳ chỉnh
 * - Serialization: JSON (không dùng Java native)
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    // ── ObjectMapper với Java 8 Time support ──────────────────────────────
    @Bean(name = "redisObjectMapper")
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Lưu type info để deserialize đúng class
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }

    // ── Redis Serializer ──────────────────────────────────────────────────
    @Bean(name = "redisSerializer")
    public GenericJackson2JsonRedisSerializer redisSerializer(ObjectMapper redisObjectMapper) {
        return new GenericJackson2JsonRedisSerializer(redisObjectMapper);
    }

    // ── RedisTemplate ─────────────────────────────────────────────────────
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(redisSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(redisSerializer);
        template.afterPropertiesSet();

        log.info("Redis: RedisTemplate đã cấu hình - {}:{}", redisHost, redisPort);
        return template;
    }

    // ── CacheManager với TTL theo từng cache ─────────────────────────────
    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            GenericJackson2JsonRedisSerializer redisSerializer) {

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))          // TTL mặc định: 10 phút
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(redisSerializer));

        // TTL tuỳ chỉnh theo từng cache name
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("khachHang",    defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put("donDatDichVu", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("dichVu",       defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigs.put("congTacVien",  defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("danhGia",      defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("thongKe",      defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigs.put("lichLamViec",  defaultConfig.entryTtl(Duration.ofMinutes(3)));

        log.info("Redis: CacheManager đã cấu hình với {} caches tuỳ chỉnh", cacheConfigs.size());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .transactionAware()
                .build();
    }
}
