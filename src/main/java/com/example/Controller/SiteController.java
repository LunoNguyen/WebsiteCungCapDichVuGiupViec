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
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Repository.DichVuRepository dichVuRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.BangGiaService bangGiaService;

    public SiteController(CatalogService catalogService, CustomerApiService customerApiService) {
        this.catalogService = catalogService;
        this.customerApiService = customerApiService;
    }

    /** Khoảng giá dùng cho bộ lọc tìm kiếm nhanh: mã → {từ, đến} (null = không giới hạn). */
    private static final Map<String, BigDecimal[]> KHOANG_GIA = new LinkedHashMap<>();
    static {
        KHOANG_GIA.put("duoi-300", new BigDecimal[]{null, BigDecimal.valueOf(300_000)});
        KHOANG_GIA.put("300-700", new BigDecimal[]{BigDecimal.valueOf(300_000), BigDecimal.valueOf(700_000)});
        KHOANG_GIA.put("700-2000", new BigDecimal[]{BigDecimal.valueOf(700_000), BigDecimal.valueOf(2_000_000)});
        KHOANG_GIA.put("tren-2000", new BigDecimal[]{BigDecimal.valueOf(2_000_000), null});
    }

    /**
     * Tìm và lọc dịch vụ (không cần đăng nhập): từ khóa mở rộng (tên, mô tả, giá tiền, thời lượng...),
     * nhóm dịch vụ, hình thức (theo lần / gói tháng), khoảng giá linh hoạt (giaTu, giaDen + thanh trượt),
     * thời lượng thực hiện, ưu đãi giảm giá, sắp xếp đa tiêu chí.
     */
    @GetMapping("/tim-dich-vu")
    @SuppressWarnings("unchecked")
    public String timDichVu(@RequestParam(required = false) String q,
                            @RequestParam(required = false) Integer nhom,
                            @RequestParam(required = false) String hinhThuc,
                            @RequestParam(required = false) String gia,
                            @RequestParam(required = false) BigDecimal giaTu,
                            @RequestParam(required = false) BigDecimal giaDen,
                            @RequestParam(required = false) String thoiLuong,
                            @RequestParam(required = false) Boolean giamGia,
                            @RequestParam(defaultValue = "phu-hop") String sapXep,
                            Model model) {
        String loaiHinh = "TheoLan".equals(hinhThuc) || "GoiThang".equals(hinhThuc) ? hinhThuc : null;
        BigDecimal[] khoang = gia != null ? KHOANG_GIA.get(gia) : null;

        BigDecimal minP = giaTu != null && giaTu.signum() > 0 ? giaTu : (khoang != null ? khoang[0] : null);
        BigDecimal maxP = giaDen != null && giaDen.signum() > 0 ? giaDen : (khoang != null ? khoang[1] : null);
        // Nếu người dùng nhập giá tối thiểu lớn hơn giá tối đa thì tự động đảo lại cho hợp lệ
        if (minP != null && maxP != null && minP.compareTo(maxP) > 0) {
            BigDecimal tmp = minP;
            minP = maxP;
            maxP = tmp;
        }

        String tlClean = ("duoi-2h".equals(thoiLuong) || "2h-3h".equals(thoiLuong) || "tren-3h".equals(thoiLuong))
                ? thoiLuong : null;
        boolean chiGiamGia = Boolean.TRUE.equals(giamGia);

        List<Map<String, Object>> ketQua;
        try {
            ketQua = new ArrayList<>(customerApiService.getServices(nhom, null, loaiHinh, q, minP, maxP));
        } catch (Exception e) {
            log.warn("Tìm dịch vụ: không tải được: {}", e.getMessage());
            ketQua = new ArrayList<>();
            model.addAttribute("loiTai", true);
        }

        // Lọc linh hoạt theo thời lượng mỗi buổi
        if (tlClean != null) {
            ketQua.removeIf(m -> {
                Integer phut = (Integer) m.get("thoiGianThucHienPhut");
                if (phut == null || phut <= 0) return true;
                return switch (tlClean) {
                    case "duoi-2h" -> phut > 120;
                    case "2h-3h" -> phut <= 120 || phut > 180;
                    case "tren-3h" -> phut <= 180;
                    default -> false;
                };
            });
        }

        // Lọc chỉ lấy dịch vụ đang có ưu đãi giảm giá (giaGoc > donGiaThamKhao)
        if (chiGiamGia) {
            ketQua.removeIf(m -> {
                BigDecimal gGoc = (BigDecimal) m.get("giaGoc");
                BigDecimal dGia = (BigDecimal) m.get("donGiaThamKhao");
                return gGoc == null || dGia == null || gGoc.compareTo(dGia) <= 0;
            });
        }

        // Các biến thể cùng mã (VD "Dọn dẹp nhà theo giờ (2 giờ)", "(3 giờ)"...) gộp thành một dịch vụ
        ketQua = gopTheoMaDichVu(ketQua);

        Comparator<Map<String, Object>> theoGia = Comparator.comparing(m -> (BigDecimal) m.get("donGiaThamKhao"),
                Comparator.nullsLast(Comparator.naturalOrder()));
        switch (sapXep) {
            case "gia-tang" -> ketQua.sort(theoGia);
            case "gia-giam" -> ketQua.sort(theoGia.reversed());
            case "ten-az" -> {
                java.text.Collator collator = java.text.Collator.getInstance(java.util.Locale.forLanguageTag("vi-VN"));
                ketQua.sort((a, b) -> collator.compare(
                        String.valueOf(a.getOrDefault("tenDichVu", "")),
                        String.valueOf(b.getOrDefault("tenDichVu", ""))));
            }
            case "thoi-luong-tang" -> ketQua.sort(Comparator.comparing(
                    m -> (Integer) m.get("thoiGianMinPhut"),
                    Comparator.nullsLast(Comparator.naturalOrder())));
            default -> {}
        }

        // Ảnh nhóm và tên nhóm đang chọn
        Map<Integer, String> anhNhom = new HashMap<>();
        String tenNhomDangChon = null;
        List<DanhMucView> danhMucs = (List<DanhMucView>) model.getAttribute("danhMucs");
        if (danhMucs != null) {
            for (DanhMucView dm : danhMucs) {
                anhNhom.put(dm.getId(), dm.getHinhAnhUrl());
                if (nhom != null && nhom.equals(dm.getId())) {
                    tenNhomDangChon = dm.getTenLoaiDichVu();
                }
            }
        }

        boolean coLocGia = minP != null || maxP != null;
        boolean coLoc = (q != null && !q.isBlank()) || nhom != null || loaiHinh != null
                || coLocGia || tlClean != null || chiGiamGia;

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("anhNhom", anhNhom);
        // Ảnh riêng theo mã dịch vụ (ưu tiên hơn ảnh nhóm)
        model.addAttribute("anhDichVu", CatalogService.anhTheoMaDichVu(danhMucs));
        model.addAttribute("q", q);
        model.addAttribute("nhom", nhom);
        model.addAttribute("tenNhomDangChon", tenNhomDangChon);
        model.addAttribute("hinhThuc", loaiHinh);
        model.addAttribute("gia", khoang != null ? gia : null);
        model.addAttribute("giaTu", minP != null ? minP.longValue() : null);
        model.addAttribute("giaDen", maxP != null ? maxP.longValue() : null);
        model.addAttribute("thoiLuong", tlClean);
        model.addAttribute("giamGia", chiGiamGia);
        model.addAttribute("sapXep", sapXep);
        model.addAttribute("coLoc", coLoc);
        return "site/tim-dich-vu";
    }

    /**
     * Gộp các dòng cùng mã dịch vụ thành một kết quả: tên chung (bỏ phần trong ngoặc), giá "từ" thấp nhất,
     * số lựa chọn; liên kết tới biến thể rẻ nhất, trang chi tiết cho chọn biến thể khác.
     */
    private static List<Map<String, Object>> gopTheoMaDichVu(List<Map<String, Object>> ds) {
        Map<Object, List<Map<String, Object>>> nhom = new LinkedHashMap<>();
        for (Map<String, Object> m : ds) {
            Object ma = m.get("maDichVu") != null ? m.get("maDichVu") : m.get("id");
            nhom.computeIfAbsent(ma, k -> new ArrayList<>()).add(m);
        }
        Comparator<Map<String, Object>> theoGia = Comparator.comparing(m -> (BigDecimal) m.get("donGiaThamKhao"),
                Comparator.nullsLast(Comparator.naturalOrder()));
        List<Map<String, Object>> ketQua = new ArrayList<>();
        for (List<Map<String, Object>> bienThe : nhom.values()) {
            Map<String, Object> reNhat = bienThe.stream().min(theoGia).orElse(bienThe.get(0));
            Map<String, Object> g = new HashMap<>(reNhat);
            String ten = String.valueOf(reNhat.get("tenDichVu"));
            int ngoac = ten.indexOf(" (");
            if (bienThe.size() > 1 && ngoac > 0) g.put("tenDichVu", ten.substring(0, ngoac));
            g.put("soLuaChon", bienThe.size());
            // Giá gốc của nhóm: giá "từ" khi chưa có đợt tạm; chỉ hiện khi khác giá "từ" hiện tại
            BigDecimal tuGoc = bienThe.stream()
                    .map(m -> m.get("giaGoc") != null ? (BigDecimal) m.get("giaGoc") : (BigDecimal) m.get("donGiaThamKhao"))
                    .filter(java.util.Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
            BigDecimal tu = (BigDecimal) reNhat.get("donGiaThamKhao");
            g.put("giaGoc", tuGoc != null && tu != null && tuGoc.compareTo(tu) != 0 ? tuGoc : null);
            boolean theoLan = bienThe.stream().anyMatch(m -> !"GoiThang".equals(m.get("loaiHinhDat")));
            boolean goiThang = bienThe.stream().anyMatch(m -> "GoiThang".equals(m.get("loaiHinhDat")));
            g.put("loaiHinhText", theoLan && goiThang ? "Theo lần, gói tháng" : goiThang ? "Gói tháng" : "Theo lần");
            Integer minPhut = bienThe.stream()
                    .map(m -> (Integer) m.get("thoiGianThucHienPhut"))
                    .filter(p -> p != null && p > 0)
                    .min(Comparator.naturalOrder()).orElse(null);
            g.put("thoiGianMinPhut", minPhut);
            if (bienThe.stream().map(m -> m.get("thoiGianThucHienPhut")).distinct().count() > 1) {
                g.put("thoiGianThucHienPhut", null); // thời lượng khác nhau giữa các lựa chọn
            }
            if (bienThe.size() > 1) g.remove("soBuoiGoi");
            ketQua.add(g);
        }
        return ketQua;
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
            model.addAttribute("giaGoc", dv.get("giaGoc"));

            // Các lựa chọn khác của cùng dịch vụ (cùng mã): theo lần trước, rồi theo giá
            Object ma = dv.get("maDichVu");

            // Ảnh đại diện: ảnh riêng của mã dịch vụ, chưa có thì ảnh danh mục
            List<DanhMucView> danhMucs = (List<DanhMucView>) model.getAttribute("danhMucs");
            String anh = ma != null ? CatalogService.anhTheoMaDichVu(danhMucs).get(ma.toString()) : null;
            if (anh == null && dv.get("loaiDichVuId") instanceof Integer loaiId) {
                DanhMucView dm = CatalogService.timDanhMuc(danhMucs, loaiId);
                if (dm != null) anh = dm.getHinhAnhUrl();
            }
            model.addAttribute("anhDichVu", anh);
            if (ma != null) {
                List<com.example.Model.DichVu> bienThe = new ArrayList<>(
                        dichVuRepository.findByMaDichVuAndTrangThai(ma.toString(), "HoatDong"));
                bienThe.sort(Comparator.comparing((com.example.Model.DichVu d) -> "GoiThang".equals(d.getLoaiHinhDat()))
                        .thenComparing(com.example.Model.DichVu::getGiaHienTai, Comparator.nullsLast(Comparator.naturalOrder())));
                if (bienThe.size() > 1) model.addAttribute("bienThe", bienThe);
                Map<Integer, BigDecimal> giaGocBienThe = new HashMap<>();
                bienThe.forEach(b -> { if (bangGiaService.giaGoc(b.getId()) != null) giaGocBienThe.put(b.getId(), bangGiaService.giaGoc(b.getId())); });
                model.addAttribute("giaGocBienThe", giaGocBienThe);
            }
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.ChinhSachService chinhSachService;

    /** Điều khoản sử dụng, Quy trình khiếu nại, Chính sách bảo mật (neo #dieu-khoan, #khieu-nai, #bao-mat). */
    @GetMapping("/chinh-sach")
    public String chinhSach(Model model) {
        model.addAttribute("taiLieu", chinhSachService.danhSach());
        return "site/chinh-sach";
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

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.KhuVucService khuVucService;

    @GetMapping("/gio-hang")
    public String gioHang() {
        return "site/gio-hang";
    }

    /** Đăng ký làm cộng tác viên (UC: Đăng ký làm cộng tác viên). */
    @GetMapping("/tro-thanh-doi-tac")
    public String troThanhDoiTac(org.springframework.ui.Model model) {
        model.addAttribute("khuVucs", khuVucService.danhSach());
        model.addAttribute("tinhThanhPhucVu", khuVucService.getTinhThanh());
        return "site/tro-thanh-doi-tac";
    }
}
