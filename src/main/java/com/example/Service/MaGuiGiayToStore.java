package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Mã dùng một lần để ứng viên gửi giấy tờ (CCCD, ảnh chân dung, chứng chỉ) SAU KHI hồ sơ đăng ký CTV đã lưu thành công.
 * File chỉ được tải lên MinIO sau khi gửi hồ sơ thành công, nên cần mã này để gắn file vào đúng hồ sơ
 * mà không cho người lạ gắn file vào hồ sơ của người khác. Lưu trên Redis (không thêm cột vào CSDL).
 *
 * Key: neatify:gui-giay-to:{ma}  →  congTacVienId
 */
@Component
public class MaGuiGiayToStore {

    private static final Logger log = LoggerFactory.getLogger(MaGuiGiayToStore.class);

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final Duration ttl;

    public MaGuiGiayToStore(StringRedisTemplate redis,
                            @Value("${app.cache.key-prefix:neatify:}") String prefix,
                            @Value("${app.gui-giay-to.ttl-phut:120}") long ttlPhut) {
        this.redis = redis;
        this.keyPrefix = prefix + "gui-giay-to:";
        this.ttl = Duration.ofMinutes(ttlPhut);
    }

    /** @return mã gửi giấy tờ, hoặc null nếu Redis lỗi (hồ sơ vẫn lưu, HCNS bổ sung giấy tờ sau) */
    public String taoMa(Integer congTacVienId) {
        String ma = UUID.randomUUID().toString().replace("-", "");
        try {
            redis.opsForValue().set(keyPrefix + ma, congTacVienId.toString(), ttl);
            return ma;
        } catch (RuntimeException e) {
            log.warn("Không tạo được mã gửi giấy tờ cho CTV {}: {}", congTacVienId, e.getMessage());
            return null;
        }
    }

    public Optional<Integer> layCtvId(String ma) {
        if (ma == null || ma.isBlank()) return Optional.empty();
        try {
            String v = redis.opsForValue().get(keyPrefix + ma.trim());
            return v == null ? Optional.empty() : Optional.of(Integer.valueOf(v));
        } catch (RuntimeException e) {
            log.warn("Không đọc được mã gửi giấy tờ: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void xoa(String ma) {
        try {
            redis.delete(keyPrefix + ma.trim());
        } catch (RuntimeException e) {
            log.warn("Không xoá được mã gửi giấy tờ: {}", e.getMessage());
        }
    }
}
