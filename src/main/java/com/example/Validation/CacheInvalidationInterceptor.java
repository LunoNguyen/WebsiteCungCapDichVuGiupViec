package com.example.Validation;

import com.example.Service.CacheService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Sau mỗi thao tác ghi (POST/PUT/DELETE) thành công, xóa các vùng cache Redis liên quan.
 * Đặt ở một chỗ nên các controller thêm/sửa/xóa không cần tự gọi xóa cache.
 */
@Component
public class CacheInvalidationInterceptor implements HandlerInterceptor {

    private final CacheService cacheService;

    public CacheInvalidationInterceptor(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        if (ex != null || response.getStatus() >= 400) {
            return;
        }

        String path = request.getRequestURI();
        if (path == null) {
            return;
        }

        if (path.contains("/danh-muc-dich-vu") || path.contains("/dich-vu-bang-gia")) {
            cacheService.xoaCacheDichVu();
        }
        // Đăng ký CTV hoặc CSKH thêm địa chỉ khách hàng có thể tạo khu vực mới
        if (path.contains("/collaborators/register") || path.contains("/khach-hang")) {
            cacheService.xoaCacheKhuVuc();
        }
        if (path.contains("/khuyen-mai")) {
            cacheService.xoaCacheKhuyenMai();
        }
        if (path.contains("/danh-gia") || path.contains("/reviews")) {
            cacheService.xoaCacheDanhGia();
        }
        // Đăng nhập, gửi OTP, tính giá, kiểm tra mã... không làm thay đổi số liệu thống kê
        boolean khongDoiSoLieu = path.startsWith("/v1/auth/login")
                || path.startsWith("/v1/auth/otp")
                || path.endsWith("/calculate-price")
                || path.endsWith("/promotions/validate")
                || path.startsWith("/login");
        if (!khongDoiSoLieu) {
            cacheService.xoaCacheThongKe();
        }
    }
}
