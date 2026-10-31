package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Đơn trong pool mà CTV đã bấm "Bỏ qua", lưu trên Redis (không thêm bảng vào CSDL).
 * Đơn bỏ qua không hiện lại trong danh sách việc mới của CTV đó; key tự hết hạn sau
 * {@code app.bo-qua-don.ttl-gio} (mặc định 3 ngày – lâu hơn hạn nhận của đơn chờ duyệt).
 *
 * Key: neatify:bo-qua-don:{congTacVienId}:{donDatId}  →  lý do
 */
@Component
public class BoQuaDonStore {

    private static final Logger log = LoggerFactory.getLogger(BoQuaDonStore.class);

    private final StringRedisTemplate redis;
    private final String keyPrefix;
    private final Duration ttl;

    public BoQuaDonStore(StringRedisTemplate redis,
                         @Value("${app.cache.key-prefix:neatify:}") String prefix,
                         @Value("${app.bo-qua-don.ttl-gio:72}") long ttlGio) {
        this.redis = redis;
        this.keyPrefix = prefix + "bo-qua-don:";
        this.ttl = Duration.ofHours(ttlGio);
    }

    public void boQua(Integer congTacVienId, Integer donDatId, String lyDo) {
        try {
            redis.opsForValue().set(keyPrefix + congTacVienId + ":" + donDatId, lyDo == null ? "" : lyDo, ttl);
        } catch (RuntimeException e) {
            log.warn("Không lưu được đơn {} CTV {} bỏ qua lên Redis: {}", donDatId, congTacVienId, e.getMessage());
            throw new IllegalStateException("Máy chủ chưa lưu được, vui lòng thử lại.");
        }
    }

    /** Redis lỗi thì coi như chưa bỏ qua – đơn vẫn hiện, CTV bỏ qua lại được. */
    public boolean daBoQua(Integer congTacVienId, Integer donDatId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(keyPrefix + congTacVienId + ":" + donDatId));
        } catch (RuntimeException e) {
            log.warn("Không đọc được đơn bỏ qua của CTV {} từ Redis: {}", congTacVienId, e.getMessage());
            return false;
        }
    }
}
