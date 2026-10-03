package com.example.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bọc CacheManager của Redis để hệ thống vẫn chạy bình thường khi Redis tắt hoặc lỗi.
 *
 * Khi một thao tác cache gặp lỗi, cache được tạm ngưng một khoảng ngắn: trong thời gian đó
 * mọi lần đọc coi như "không có trong cache" (dữ liệu lấy thẳng từ MySQL) và không gọi tới Redis,
 * nên người dùng không phải chờ hết thời gian timeout ở từng lần đọc. Hết thời gian tạm ngưng,
 * hệ thống tự thử lại Redis.
 */
public class CacheManagerAnToan implements CacheManager {

    private static final Logger log = LoggerFactory.getLogger(CacheManagerAnToan.class);

    private final CacheManager goc;
    private final long tamNgungMs;
    private final AtomicLong tamNgungDen = new AtomicLong(0);
    private final Map<String, Cache> daBoc = new ConcurrentHashMap<>();

    public CacheManagerAnToan(CacheManager goc, long tamNgungGiay) {
        this.goc = goc;
        this.tamNgungMs = tamNgungGiay * 1000;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = goc.getCache(name);
        if (cache == null) {
            return null;
        }
        return daBoc.computeIfAbsent(name, k -> new CacheAnToan(cache));
    }

    @Override
    public Collection<String> getCacheNames() {
        return goc.getCacheNames();
    }

    private boolean dangTamNgung() {
        return System.currentTimeMillis() < tamNgungDen.get();
    }

    private void ghiNhanLoi(String thaoTac, String tenCache, RuntimeException e) {
        tamNgungDen.set(System.currentTimeMillis() + tamNgungMs);
        log.warn("Redis lỗi khi {} cache '{}': {}. Tạm đọc thẳng từ CSDL trong {} giây.",
                thaoTac, tenCache, e.getMessage(), tamNgungMs / 1000);
    }

    private final class CacheAnToan implements Cache {

        private final Cache cache;

        private CacheAnToan(Cache cache) {
            this.cache = cache;
        }

        @Override
        public String getName() {
            return cache.getName();
        }

        @Override
        public Object getNativeCache() {
            return cache.getNativeCache();
        }

        @Override
        public ValueWrapper get(Object key) {
            if (dangTamNgung()) {
                return null;
            }
            try {
                return cache.get(key);
            } catch (RuntimeException e) {
                ghiNhanLoi("đọc", getName(), e);
                return null;
            }
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            if (dangTamNgung()) {
                return null;
            }
            try {
                return cache.get(key, type);
            } catch (RuntimeException e) {
                ghiNhanLoi("đọc", getName(), e);
                return null;
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T get(Object key, Callable<T> valueLoader) {
            ValueWrapper coSan = get(key);
            if (coSan != null) {
                return (T) coSan.get();
            }
            T giaTri;
            try {
                giaTri = valueLoader.call();
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
            put(key, giaTri);
            return giaTri;
        }

        @Override
        public void put(Object key, Object value) {
            if (dangTamNgung()) {
                return;
            }
            try {
                cache.put(key, value);
            } catch (RuntimeException e) {
                ghiNhanLoi("ghi", getName(), e);
            }
        }

        @Override
        public void evict(Object key) {
            if (dangTamNgung()) {
                return;
            }
            try {
                cache.evict(key);
            } catch (RuntimeException e) {
                ghiNhanLoi("xóa", getName(), e);
            }
        }

        @Override
        public void clear() {
            if (dangTamNgung()) {
                return;
            }
            try {
                cache.clear();
            } catch (RuntimeException e) {
                ghiNhanLoi("xóa", getName(), e);
            }
        }
    }
}
