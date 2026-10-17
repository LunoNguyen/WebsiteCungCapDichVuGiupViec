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
import java.util.Optional;

/**
 * Vị trí GPS gần nhất của cộng tác viên, lưu trên Redis (không thêm cột vào CSDL).
 * App CTV gửi vị trí mỗi 5 giây; key tự hết hạn sau {@code app.vi-tri-ctv.ttl-giay}
 * nên CTV tắt app / mất mạng thì coi như không còn vị trí.
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

    /** Cập nhật vị trí GPS đầy đủ kèm Quận/Huyện, Tỉnh/Thành lên Redis */
    public ViTri update(Integer congTacVienId, BigDecimal viDo, BigDecimal kinhDo, String quanHuyen, String tinhThanh, String diaChi) {
        long now = System.currentTimeMillis();
        String qh = safe(quanHuyen);
        String tt = safe(tinhThanh);
        String dc = safe(diaChi);
        String val = viDo.toPlainString() + "|" + kinhDo.toPlainString() + "|" + qh + "|" + tt + "|" + dc + "|" + now;
        try {
            redis.opsForValue().set(keyPrefix + congTacVienId, val, ttl);
            try {
                redis.opsForGeo().add(keyPrefix + "geo",
                        new org.springframework.data.geo.Point(kinhDo.doubleValue(), viDo.doubleValue()),
                        congTacVienId.toString());
            } catch (Exception ignored) {}
        } catch (RuntimeException e) {
            log.warn("Không lưu được vị trí CTV {} lên Redis: {}", congTacVienId, e.getMessage());
            throw new IllegalStateException("Máy chủ chưa lưu được vị trí, sẽ thử lại.");
        }
        return new ViTri(viDo, kinhDo, qh, tt, dc, toLocal(now));
    }

    /** Overload tương thích */
    public ViTri update(Integer congTacVienId, BigDecimal viDo, BigDecimal kinhDo) {
        return update(congTacVienId, viDo, kinhDo, "", "", "");
    }

    public Optional<ViTri> get(Integer congTacVienId) {
        try {
            String v = redis.opsForValue().get(keyPrefix + congTacVienId);
            if (v == null) return Optional.empty();
            String[] p = v.split("\\|", -1);
            if (p.length >= 6) {
                return Optional.of(new ViTri(
                        new BigDecimal(p[0]),
                        new BigDecimal(p[1]),
                        p[2],
                        p[3],
                        p[4],
                        toLocal(Long.parseLong(p[5]))
                ));
            } else if (p.length >= 3) {
                return Optional.of(new ViTri(
                        new BigDecimal(p[0]),
                        new BigDecimal(p[1]),
                        "",
                        "",
                        "",
                        toLocal(Long.parseLong(p[2]))
                ));
            }
            return Optional.empty();
        } catch (RuntimeException e) {
            log.warn("Không đọc được vị trí CTV {} từ Redis: {}", congTacVienId, e.getMessage());
            return Optional.empty();
        }
    }

    private static LocalDateTime toLocal(long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
