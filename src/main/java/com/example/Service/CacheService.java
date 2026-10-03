package com.example.Service;

import com.example.Config.CacheNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Xóa cache Redis khi dữ liệu gốc trong MySQL thay đổi,
 * để lần đọc kế tiếp nạp lại dữ liệu mới.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /** Danh mục, dịch vụ, bảng giá thay đổi. */
    public void xoaCacheDichVu() {
        xoa(CacheNames.DANH_MUC_DICH_VU, CacheNames.DICH_VU, CacheNames.LOAI_DICH_VU);
    }

    public void xoaCacheKhuVuc() {
        xoa(CacheNames.KHU_VUC);
    }

    public void xoaCacheKhuyenMai() {
        xoa(CacheNames.KHUYEN_MAI);
    }

    public void xoaCacheDanhGia() {
        xoa(CacheNames.DANH_GIA);
    }

    public void xoaCacheThongKe() {
        xoa(CacheNames.THONG_KE);
    }

    public void xoa(String... tenCache) {
        for (String ten : tenCache) {
            try {
                Cache cache = cacheManager.getCache(ten);
                if (cache != null) {
                    cache.clear();
                    log.debug("Đã xóa vùng cache {}", ten);
                }
            } catch (RuntimeException e) {
                log.warn("Không xóa được vùng cache {}: {}", ten, e.getMessage());
            }
        }
    }
}
