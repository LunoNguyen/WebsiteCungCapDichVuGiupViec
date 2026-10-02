package com.example.Controller;

import com.example.DTO.view.DanhMucView;
import com.example.Service.CatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Các trang công khai của website: giới thiệu, dịch vụ & bảng giá, khuyến mãi, trở thành đối tác.
 * Danh mục dịch vụ ("danhMucs") đã được SiteModelAdvice nạp sẵn vào model.
 */
@Controller
public class SiteController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    private final CatalogService catalogService;

    public SiteController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/gioi-thieu")
    public String gioiThieu(Model model) {
        try {
            model.addAttribute("khuVucs", catalogService.getKhuVucTheoTinhThanh());
            model.addAttribute("soLieu", catalogService.getSoLieuCongKhai());
        } catch (Exception e) {
            log.warn("Trang giới thiệu: không tải được dữ liệu: {}", e.getMessage());
            model.addAttribute("khuVucs", new LinkedHashMap<String, List<String>>());
        }
        return "site/gioi-thieu";
    }

    @GetMapping("/dich-vu")
    public String dichVu() {
        return "site/dich-vu";
    }

    /** Chi tiết một nhóm dịch vụ kèm bảng giá. */
    @GetMapping("/dich-vu/{id}")
    @SuppressWarnings("unchecked")
    public String chiTietDichVu(@PathVariable Integer id, Model model) {
        // "danhMucs" đã có sẵn trong model nhờ SiteModelAdvice (lấy từ cache Redis)
        List<DanhMucView> danhMucs = (List<DanhMucView>) model.getAttribute("danhMucs");
        DanhMucView danhMuc = CatalogService.timDanhMuc(danhMucs, id);
        if (danhMuc == null) {
            return "redirect:/dich-vu";
        }
        model.addAttribute("danhMuc", danhMuc);
        return "site/dich-vu-chi-tiet";
    }

    @GetMapping("/khuyen-mai")
    public String khuyenMai(Model model) {
        try {
            model.addAttribute("khuyenMais", catalogService.getKhuyenMaiDangChay());
        } catch (Exception e) {
            log.warn("Trang khuyến mãi: không tải được dữ liệu: {}", e.getMessage());
        }
        return "site/khuyen-mai";
    }

    /** Đăng ký làm cộng tác viên (UC: Đăng ký làm cộng tác viên). */
    @GetMapping("/tro-thanh-doi-tac")
    public String troThanhDoiTac(Model model) {
        try {
            model.addAttribute("khuVucList", catalogService.getKhuVucChoDangKy());
        } catch (Exception e) {
            log.warn("Trang đối tác: không tải được khu vực: {}", e.getMessage());
        }
        return "site/tro-thanh-doi-tac";
    }
}
