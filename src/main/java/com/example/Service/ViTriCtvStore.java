package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vị trí GPS gần nhất của cộng tác viên, lưu trên Redis và có fallback bộ nhớ trong (ConcurrentHashMap).
 * App CTV gửi vị trí mỗi 5 giây; khi Redis tắt hoặc mất mạng, hệ thống vẫn lưu tạm trong bộ nhớ
 * để không làm gián đoạn theo dõi đơn và phân công việc.
 *
 * Key: neatify:vi-tri-ctv:{congTacVienId}  →  "viDo|kinhDo|epochMillis"
 * (redis-cli: GET neatify:vi-tri-ctv:1)
 */
@Component
public class ViTriCtvStore {

    private static final Logger log = LoggerFactory.getLogger(ViTriCtvStore.class);

    public record ViTri(BigDecimal viDo, BigDecimal kinhDo, String quanHuyen, String tinhThanh, String diaChi, LocalDateTime thoiGian) {}

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final Duration ttl;
    private final Map<Integer, ViTri> inMemoryStore = new ConcurrentHashMap<>();

    public ViTriCtvStore(StringRedisTemplate redis,
                         @Value("${app.cache.key-prefix:neatify:}") String prefix,
                         @Value("${app.vi-tri-ctv.ttl-giay:300}") long ttlGiay) {
        this.redis = redis;
        this.keyPrefix = prefix + "vi-tri-ctv:";
        this.ttl = Duration.ofSeconds(ttlGiay);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace("|", "/").trim();
    }

    /** Cập nhật vị trí GPS đầy đủ kèm Quận/Huyện, Tỉnh/Thành lên Redis & bộ nhớ trong */
    public ViTri update(Integer congTacVienId, BigDecimal viDo, BigDecimal kinhDo, String quanHuyen, String tinhThanh, String diaChi) {
        long now = System.currentTimeMillis();
        String qh = safe(quanHuyen);
        String tt = safe(tinhThanh);
        String dc = safe(diaChi);
        ViTri vt = new ViTri(viDo, kinhDo, qh, tt, dc, toLocal(now));
        // Luôn lưu vào bộ nhớ trong trước để sẵn sàng phục vụ tức thì kể cả khi Redis gặp sự cố
        inMemoryStore.put(congTacVienId, vt);

        String val = viDo.toPlainString() + "|" + kinhDo.toPlainString() + "|" + qh + "|" + tt + "|" + dc + "|" + now;
        try {
            redis.opsForValue().set(keyPrefix + congTacVienId, val, ttl);
            try {
                redis.opsForGeo().add(keyPrefix + "geo",
                        new org.springframework.data.geo.Point(kinhDo.doubleValue(), viDo.doubleValue()),
                        congTacVienId.toString());
            } catch (Exception ignored) {}
        } catch (RuntimeException e) {
            log.warn("Không lưu được vị trí CTV {} lên Redis (đã lưu bộ nhớ trong fallback): {}", congTacVienId, e.getMessage());
        }
        return vt;
    }

    /** Overload tương thích */
    public ViTri update(Integer congTacVienId, BigDecimal viDo, BigDecimal kinhDo) {
        return update(congTacVienId, viDo, kinhDo, "", "", "");
    }

    public Optional<ViTri> get(Integer congTacVienId) {
        try {
            String v = redis.opsForValue().get(keyPrefix + congTacVienId);
            if (v != null) {
                String[] p = v.split("\\|", -1);
                if (p.length >= 6) {
                    ViTri vt = new ViTri(
                            new BigDecimal(p[0]),
                            new BigDecimal(p[1]),
                            p[2],
                            p[3],
                            p[4],
                            toLocal(Long.parseLong(p[5]))
                    );
                    inMemoryStore.put(congTacVienId, vt);
                    return Optional.of(vt);
                } else if (p.length >= 3) {
                    ViTri vt = new ViTri(
                            new BigDecimal(p[0]),
                            new BigDecimal(p[1]),
                            "",
                            "",
                            "",
                            toLocal(Long.parseLong(p[2]))
                    );
                    inMemoryStore.put(congTacVienId, vt);
                    return Optional.of(vt);
                }
            }
        } catch (RuntimeException e) {
            log.warn("Không đọc được vị trí CTV {} từ Redis, đọc từ bộ nhớ trong fallback: {}", congTacVienId, e.getMessage());
        }

        // Fallback đọc từ bộ nhớ trong
        ViTri mem = inMemoryStore.get(congTacVienId);
        if (mem != null) {
            long diffSec = Duration.between(mem.thoiGian(), LocalDateTime.now()).abs().getSeconds();
            // Cho phép giữ vị trí trong bộ nhớ tối thiểu 15 phút hoặc 3 lần TTL
            if (diffSec <= Math.max(ttl.getSeconds() * 3, 900)) {
                return Optional.of(mem);
            }
        }
        return Optional.empty();
    }

    private static LocalDateTime toLocal(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
