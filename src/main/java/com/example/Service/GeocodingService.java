package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Đổi địa chỉ dạng chữ thành toạ độ bằng OpenStreetMap Nominatim (miễn phí, không cần API key).
 * CSDL không lưu toạ độ địa chỉ nên kết quả được cache trên Redis để không gọi lại nhiều lần
 * (Nominatim giới hạn khoảng 1 yêu cầu/giây).
 *
 * Key: neatify:geocode:{sha1(địa chỉ)} → "viDo|kinhDo" hoặc "-" (không tìm thấy)
 */
@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);
    private static final String NOMINATIM = "https://nominatim.openstreetmap.org/search";
    private static final String KHONG_THAY = "-";

    public record ToaDo(BigDecimal viDo, BigDecimal kinhDo) {}

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final RestTemplate http;

    public GeocodingService(StringRedisTemplate redis,
                            @Value("${app.cache.key-prefix:neatify:}") String prefix) {
        this.redis = redis;
        this.keyPrefix = prefix + "geocode:";
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(3));
        f.setReadTimeout(Duration.ofSeconds(5));
        this.http = new RestTemplate(f);
    }

    public Optional<ToaDo> timToaDo(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) return Optional.empty();
        String q = diaChi.trim();
        String key = keyPrefix + sha1(q.toLowerCase());

        String cached = docCache(key);
        if (cached != null) return parse(cached);

        // Thử cả địa chỉ đầy đủ, rồi bỏ dần phần đầu (số nhà/ngõ) nếu không tìm thấy
        ToaDo kq = null;
        String thu = q;
        for (int i = 0; i < 3 && kq == null && thu != null; i++) {
            kq = goiNominatim(thu);
            int phay = thu.indexOf(',');
            thu = phay > 0 ? thu.substring(phay + 1).trim() : null;
        }
        ghiCache(key, kq != null ? kq.viDo().toPlainString() + "|" + kq.kinhDo().toPlainString() : KHONG_THAY,
                kq != null ? Duration.ofDays(30) : Duration.ofHours(6));
        return Optional.ofNullable(kq);
    }

    private ToaDo goiNominatim(String q) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(NOMINATIM)
                    .queryParam("format", "json")
                    .queryParam("limit", 1)
                    .queryParam("countrycodes", "vn")
                    .queryParam("q", q)
                    .encode(StandardCharsets.UTF_8)
                    .build().toUri();
            HttpHeaders h = new HttpHeaders();
            h.set(HttpHeaders.USER_AGENT, "Neatify/1.0 (luan-an)"); // Nominatim yêu cầu User-Agent riêng
            h.set(HttpHeaders.ACCEPT_LANGUAGE, "vi");
            List<?> body = http.exchange(uri, HttpMethod.GET, new HttpEntity<>(h), List.class).getBody();
            if (body == null || body.isEmpty()) return null;
            Map<?, ?> first = (Map<?, ?>) body.get(0);
            return new ToaDo(new BigDecimal(first.get("lat").toString()), new BigDecimal(first.get("lon").toString()));
        } catch (RuntimeException e) {
            log.warn("Không tra được toạ độ cho '{}': {}", q, e.getMessage());
            return null;
        }
    }

    private String docCache(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            return null; // Redis tắt: tra trực tiếp
        }
    }

    private void ghiCache(String key, String value, Duration ttl) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (RuntimeException ignored) {
        }
    }

    private static Optional<ToaDo> parse(String v) {
        if (KHONG_THAY.equals(v)) return Optional.empty();
        String[] p = v.split("\\|");
        return Optional.of(new ToaDo(new BigDecimal(p[0]), new BigDecimal(p[1])));
    }

    private static String sha1(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
