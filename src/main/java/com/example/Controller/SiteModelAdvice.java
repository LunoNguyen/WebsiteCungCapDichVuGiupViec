package com.example.Controller;

import com.example.DTO.view.DanhMucView;
import com.example.Service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

/**
 * Nạp danh mục dịch vụ cho menu và chân trang của mọi trang công khai
 * (trang chủ, giới thiệu, dịch vụ, khuyến mãi, trở thành đối tác, đăng nhập).
 */
@ControllerAdvice(assignableTypes = {LandingController.class, SiteController.class, KhachHangWebController.class})
public class SiteModelAdvice {

    private static final Logger log = LoggerFactory.getLogger(SiteModelAdvice.class);

    private final CatalogService catalogService;

    public SiteModelAdvice(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /** Link tải ứng dụng di động (app.download-url); để trống nếu chưa phát hành. */
    @Value("${app.download-url:}")
    private String appDownloadUrl;

    @ModelAttribute("appDownloadUrl")
    public String appDownloadUrl() {
        return appDownloadUrl;
    }

    @ModelAttribute("danhMucs")
    public List<DanhMucView> danhMucs() {
        try {
            return catalogService.getDanhMuc();
        } catch (Exception e) {
            // CSDL lỗi: trang vẫn hiển thị, chỉ thiếu danh sách dịch vụ
            log.warn("Không tải được danh mục dịch vụ: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
