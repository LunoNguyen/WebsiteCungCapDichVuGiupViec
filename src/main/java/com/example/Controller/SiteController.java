package com.example.Controller;

import com.example.DTO.view.DanhMucView;
import com.example.Service.CatalogService;
import com.example.Service.CustomerApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Các trang công khai của website: giới thiệu, dịch vụ & bảng giá, khuyến mãi, trở thành đối tác.
 * Danh mục dịch vụ ("danhMucs") đã được SiteModelAdvice nạp sẵn vào model.
 */
@Controller
public class SiteController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    private final CatalogService catalogService;
    private final CustomerApiService customerApiService;

    public SiteController(CatalogService catalogService, CustomerApiService customerApiService) {
        this.catalogService = catalogService;
        this.customerApiService = customerApiService;
    }

    /** Khoảng giá dùng cho bộ lọc tìm kiếm: mã → {từ, đến} (null = không giới hạn). */
    private static final Map<String, BigDecimal[]> KHOANG_GIA = new LinkedHashMap<>();
    static {
        KHOANG_GIA.put("duoi-300", new BigDecimal[]{null, BigDecimal.valueOf(300_000)});
        KHOANG_GIA.put("300-700", new BigDecimal[]{BigDecimal.valueOf(300_000), BigDecimal.valueOf(700_000)});
        KHOANG_GIA.put("700-2000", new BigDecimal[]{BigDecimal.valueOf(700_000), BigDecimal.valueOf(2_000_000)});
        KHOANG_GIA.put("tren-2000", new BigDecimal[]{BigDecimal.valueOf(2_000_000), null});
    }

    /**
     * Tìm và lọc dịch vụ (không cần đăng nhập): từ khóa (không dấu cũng được), nhóm dịch vụ,
     * hình thức (theo lần / gói tháng), khoảng giá, sắp xếp theo giá.
     */
    @GetMapping("/tim-dich-vu")
    @SuppressWarnings("unchecked")
    public String timDichVu(@RequestParam(required = false) String q,
                            @RequestParam(required = false) Integer nhom,
                            @RequestParam(required = false) String hinhThuc,
                            @RequestParam(required = false) String gia,
                            @RequestParam(defaultValue = "phu-hop") String sapXep,
                            Model model) {
        String loaiHinh = "TheoLan".equals(hinhThuc) || "GoiThang".equals(hinhThuc) ? hinhThuc : null;
        BigDecimal[] khoang = gia != null ? KHOANG_GIA.get(gia) : null;
        List<Map<String, Object>> ketQua;
        try {
            ketQua = new ArrayList<>(customerApiService.getServices(nhom, null, loaiHinh, q,
                    khoang != null ? khoang[0] : null, khoang != null ? khoang[1] : null));
        } catch (Exception e) {
            log.warn("Tìm dịch vụ: không tải được: {}", e.getMessage());
            ketQua = new ArrayList<>();
            model.addAttribute("loiTai", true);
        }
        Comparator<Map<String, Object>> theoGia = Comparator.comparing(m -> (BigDecimal) m.get("donGiaThamKhao"),
                Comparator.nullsLast(Comparator.naturalOrder()));
        if ("gia-tang".equals(sapXep)) ketQua.sort(theoGia);
        else if ("gia-giam".equals(sapXep)) ketQua.sort(theoGia.reversed());

        // Ảnh nhóm cho từng dòng kết quả
        Map<Integer, String> anhNhom = new HashMap<>();
        List<DanhMucView> danhMucs = (List<DanhMucView>) model.getAttribute("danhMucs");
        if (danhMucs != null) danhMucs.forEach(dm -> anhNhom.put(dm.getId(), dm.getHinhAnhUrl()));

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("anhNhom", anhNhom);
        model.addAttribute("q", q);
        model.addAttribute("nhom", nhom);
        model.addAttribute("hinhThuc", loaiHinh);
        model.addAttribute("gia", khoang != null ? gia : null);
        model.addAttribute("sapXep", sapXep);
        model.addAttribute("coLoc", (q != null && !q.isBlank()) || nhom != null || loaiHinh != null || khoang != null);
        return "site/tim-dich-vu";
    }

    /** Thông tin một dịch vụ (không cần đăng nhập): giá, thời lượng, số buổi gói, mô tả, đánh giá. */
    @GetMapping("/dich-vu/chi-tiet/{id}")
    @SuppressWarnings("unchecked")
    public String thongTinDichVu(@PathVariable Integer id, Model model) {
        try {
            Map<String, Object> dv = customerApiService.getServiceDetail(id);
            model.addAttribute("dv", dv);
            BigDecimal gia = (BigDecimal) dv.get("giaHienTai");
            if (gia == null || gia.signum() <= 0) {
                List<Map<String, Object>> bangGias = (List<Map<String, Object>>) dv.get("bangGias");
                if (bangGias != null && !bangGias.isEmpty()) gia = (BigDecimal) bangGias.get(0).get("donGia");
            }
            model.addAttribute("gia", gia);
        } catch (IllegalArgumentException e) {
            return "redirect:/tim-dich-vu";
        }
        return "site/dich-vu-thong-tin";
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
    public String troThanhDoiTac() {
        return "site/tro-thanh-doi-tac";
    }
}
