package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.text.Normalizer;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Đổi địa chỉ dạng chữ thành toạ độ bằng OpenStreetMap Nominatim và dự phòng toạ độ các quận TP.HCM (TamQuanHcm).
 * Có bộ nhớ đệm kép (In-Memory + Redis) và cơ chế ngắt mạch (circuit breaker) để tránh làm nghẽn
 * luồng máy chủ khi không có kết nối internet hoặc dịch vụ bên ngoài phản hồi chậm.
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
    private final Map<String, Optional<ToaDo>> memCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private TamQuanHcm tamQuanHcm;

    // Ngắt mạch: khi gọi Nominatim bị lỗi/timeout thì tạm ngưng gọi mạng trong 60 giây để dùng fallback ngay
    private volatile long networkCooldownUntil = 0;

    public GeocodingService(StringRedisTemplate redis,
                            @Value("${app.cache.key-prefix:neatify:}") String prefix) {
        this.redis = redis;
        this.keyPrefix = prefix + "geocode:";
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofMillis(1500));
        f.setReadTimeout(Duration.ofMillis(2000));
        this.http = new RestTemplate(f);
    }

    public Optional<ToaDo> timToaDo(String diaChi) {
        if (diaChi == null || diaChi.isBlank()) return Optional.empty();
        String q = diaChi.trim();
        String cacheKey = q.toLowerCase();

        // 1. Kiểm tra cache trong bộ nhớ (nhanh nhất, 0ms)
        Optional<ToaDo> mem = memCache.get(cacheKey);
        if (mem != null) return mem;

        // 2. Kiểm tra cache Redis
        String key = keyPrefix + sha1(cacheKey);
        String cached = docCache(key);
        if (cached != null) {
            Optional<ToaDo> parsed = parse(cached);
            memCache.put(cacheKey, parsed);
            return parsed;
        }

        // 3. Chuẩn bị toạ độ dự phòng từ tâm quận (TamQuanHcm) nếu địa chỉ thuộc TP.HCM
        ToaDo fallbackQuan = doanToaDoTheoTamQuan(q);

        // 4. Nếu mạng không bị ngắt mạch, thử gọi OpenStreetMap Nominatim
        ToaDo kq = null;
        if (System.currentTimeMillis() >= networkCooldownUntil) {
            String thu = q;
            for (int i = 0; i < 2 && kq == null && thu != null; i++) {
                kq = goiNominatim(thu);
                int phay = thu.indexOf(',');
                thu = phay > 0 ? thu.substring(phay + 1).trim() : null;
            }
        }

        // 5. Nếu không tra được bằng Nominatim thì dùng toạ độ tâm quận
        if (kq == null && fallbackQuan != null) {
            kq = fallbackQuan;
        }

        Optional<ToaDo> result = Optional.ofNullable(kq);
        memCache.put(cacheKey, result);
        ghiCache(key, kq != null ? kq.viDo().toPlainString() + "|" + kq.kinhDo().toPlainString() : KHONG_THAY,
                kq != null ? Duration.ofDays(30) : Duration.ofHours(6));
        return result;
    }

    /** Tra toạ độ nhanh trực tiếp theo tên quận/huyện TP.HCM */
    public Optional<ToaDo> timToaDoTheoKhuVuc(String tenKhuVuc) {
        if (tenKhuVuc == null || tenKhuVuc.isBlank() || tamQuanHcm == null) return Optional.empty();
        String clean = boDau(tenKhuVuc).toLowerCase();
        for (TamQuanHcm.Tam t : tamQuanHcm.danhSach()) {
            String tenTam = boDau(t.ten()).toLowerCase();
            if (clean.contains(tenTam) || tenTam.contains(clean)) {
                return Optional.of(new ToaDo(BigDecimal.valueOf(t.viDo()), BigDecimal.valueOf(t.kinhDo())));
            }
        }
        return Optional.empty();
    }

    private ToaDo doanToaDoTheoTamQuan(String diaChi) {
        if (tamQuanHcm == null || diaChi == null || diaChi.isBlank()) return null;
        String clean = boDau(diaChi).toLowerCase();
        for (TamQuanHcm.Tam t : tamQuanHcm.danhSach()) {
            String tenTam = boDau(t.ten()).toLowerCase();
            // Khớp ví dụ: "quan 1", "binh thanh", "thu duc", "go vap", "phu nhuan"
            if (clean.contains(tenTam)) {
                return new ToaDo(BigDecimal.valueOf(t.viDo()), BigDecimal.valueOf(t.kinhDo()));
            }
            // Khớp viết tắt dạng "q.1", "q1", "q. 1"
            if (t.ten().startsWith("Quận ")) {
                String soQuan = t.ten().substring(5).trim();
                if (clean.matches(".*\\b(q\\.?\\s*" + Pattern.quote(soQuan) + ")\\b.*")) {
                    return new ToaDo(BigDecimal.valueOf(t.viDo()), BigDecimal.valueOf(t.kinhDo()));
                }
            }
        }
        return null;
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
            h.set(HttpHeaders.USER_AGENT, "bTaskee/1.0 (luan-an)");
            h.set(HttpHeaders.ACCEPT_LANGUAGE, "vi");
            List<?> body = http.exchange(uri, HttpMethod.GET, new HttpEntity<>(h), List.class).getBody();
            if (body == null || body.isEmpty()) return null;
            Map<?, ?> first = (Map<?, ?>) body.get(0);
            return new ToaDo(new BigDecimal(first.get("lat").toString()), new BigDecimal(first.get("lon").toString()));
        } catch (RuntimeException e) {
            log.warn("Không kết nối được dịch vụ toạ độ mạng cho '{}' (chuyển sang chế độ offline): {}", q, e.getMessage());
            // Kích hoạt ngắt mạch trong 60 giây để không làm chậm các request kế tiếp
            networkCooldownUntil = System.currentTimeMillis() + 60_000;
            return null;
        }
    }

    private String docCache(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            return null;
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

    private static String boDau(String str) {
        if (str == null) return "";
        String nfd = Normalizer.normalize(str, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(nfd)
                .replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }
}
