package com.example.Controller;
import com.example.Model.DanhGia;
import java.util.List;
import com.example.DTO.*;
import com.example.Model.ThongBao;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Marketing module controllers - UC-MKT01 to UC-MKT04
 * Gọi dữ liệu trực tiếp từ CSDL thay vì gán tĩnh.
 */
@Controller
@RequestMapping("/marketing")
public class MarketingController {

    private final ThongKeService thongKeService;
    private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
    private final MaKhuyenMaiRepository maKhuyenMaiRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final DanhGiaRepository danhGiaRepository;

    public MarketingController(
            ThongKeService thongKeService,
            ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository,
            MaKhuyenMaiRepository maKhuyenMaiRepository,
            ThongBaoRepository thongBaoRepository,
            DanhGiaRepository danhGiaRepository) {
        this.thongKeService = thongKeService;
        this.chuongTrinhKhuyenMaiRepository = chuongTrinhKhuyenMaiRepository;
        this.maKhuyenMaiRepository = maKhuyenMaiRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.danhGiaRepository = danhGiaRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var mktStats = thongKeService.getMarketingStats();
        model.addAttribute("marketingStats", mktStats);
        model.addAttribute("tongKhuyenMai", mktStats.getTongKhuyenMai());
        model.addAttribute("tongCoupons", mktStats.getTongCoupons());
        model.addAttribute("tongKhachHang", thongKeService.getTongKhachHang());
        model.addAttribute("diemDanhGiaTB", thongKeService.getDiemDanhGiaTrungBinh());
        model.addAttribute("tongDoanhThuTrieu", thongKeService.getDoanhThuTrieuDong());
        model.addAttribute("phanPhoiDanhGia", thongKeService.getPhanPhoiDanhGia());
        model.addAttribute("topDichVu", thongKeService.getTopDichVu());
        model.addAttribute("recentReviews", danhGiaRepository.findAll());
        model.addAttribute("khuyenMais", chuongTrinhKhuyenMaiRepository.findAll());
        model.addAttribute("coupons", maKhuyenMaiRepository.findAll());
        return "marketing/dashboard";
    }

    // UC-MKT01 – Quản lý khuyến mãi
    @GetMapping("/khuyen-mai")
    public String khuyenMai(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String trangThai,
            Model model) {
            
        model.addAttribute("marketingStats", thongKeService.getMarketingStats());
        
        // Lấy toàn bộ mã khuyến mãi
        List<com.example.Model.MaKhuyenMai> coupons = maKhuyenMaiRepository.findAll();
        // TỰ ĐỘNG QUÉT VÀ ĐỔI TRẠNG THÁI NẾU QUÁ HẠN
        java.time.LocalDate today = java.time.LocalDate.now();
        for (com.example.Model.MaKhuyenMai cp : coupons) {
            if (cp.getChuongTrinhKhuyenMai() != null
                && cp.getChuongTrinhKhuyenMai().getNgayBatDau() != null
                && cp.getChuongTrinhKhuyenMai().getNgayKetThuc() != null
                && !"HetLuot".equals(cp.getTrangThai())) { // không đụng tới mã đã hết lượt dùng

                java.time.LocalDate start = cp.getChuongTrinhKhuyenMai().getNgayBatDau();
                java.time.LocalDate end = cp.getChuongTrinhKhuyenMai().getNgayKetThuc();
                String trangThaiDung;

                if (end.isBefore(today)) {
                    trangThaiDung = "HetHan";
                } else if (start.isAfter(today)) {
                    trangThaiDung = "SapDienRa";
                } else {
                    trangThaiDung = "HoatDong";
                }

                if (!trangThaiDung.equals(cp.getTrangThai())) {
                    cp.setTrangThai(trangThaiDung);
                    maKhuyenMaiRepository.save(cp);
                }
            }
        }
        // Xử lý Tìm kiếm theo Keyword (Mã coupon hoặc Tên chương trình)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            coupons = coupons.stream()
                .filter(c -> c.getCodeKhuyenMai().toLowerCase().contains(kw) || 
                            (c.getChuongTrinhKhuyenMai() != null && c.getChuongTrinhKhuyenMai().getTenChuongTrinh().toLowerCase().contains(kw)))
                .toList();
            model.addAttribute("keyword", keyword);
        }
        
        // Xử lý Lọc theo Trạng thái
        if (trangThai != null && !trangThai.isEmpty()) {
            coupons = coupons.stream()
                .filter(c -> c.getTrangThai().equalsIgnoreCase(trangThai))
                .toList();
            model.addAttribute("trangThai", trangThai);
        }
        
        // Sắp xếp ID mới nhất lên đầu
        coupons = new java.util.ArrayList<>(coupons);
        coupons.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        
        model.addAttribute("coupons", coupons);
        return "marketing/khuyen-mai";
    }

    // UC-MKT02 – Quản lý thông báo (gửi đến KH/CTV)
    @GetMapping("/thong-bao")
    public String thongBao(Model model) {
        model.addAttribute("thongBaos", thongBaoRepository.findAll());
        
        // Add dynamic stats for the Stats Grid
        model.addAttribute("tongChienDich", thongKeService.getTongChiengDich());
        
        // Format the total recipients nicely (e.g., "15.2K" instead of "15200")
        long tongLuotNhan = thongKeService.getTongLuotNhanThongBao();
        String formattedLuotNhan = tongLuotNhan > 1000 ? String.format("%.1fK", tongLuotNhan / 1000.0) : String.valueOf(tongLuotNhan);
        model.addAttribute("tongLuotNhanThongBao", formattedLuotNhan);
        
        model.addAttribute("trungBinhCTR", thongKeService.getTrungBinhCTR());
        model.addAttribute("tongDonDatTuThongBao", thongKeService.getTongDonDatTuThongBao());
        
        return "marketing/thong-bao";
    }

    // UC-MKT03 – Phân tích & báo cáo marketing
   // UC-MKT03 – Phân tích & báo cáo marketing
    @GetMapping("/phan-tich")
    public String phanTich(Model model) {
        var mktStats = thongKeService.getMarketingStats();
        model.addAttribute("marketingStats", mktStats);
        model.addAttribute("tongKhuyenMai", mktStats.getTongKhuyenMai());
        model.addAttribute("tongCoupons", mktStats.getTongCoupons());
        model.addAttribute("tongKhachHang", thongKeService.getTongKhachHang());
        
        // Danh sách chiến dịch
        model.addAttribute("khuyenMais", chuongTrinhKhuyenMaiRepository.findAll());
        model.addAttribute("tongDoanhThuTrieu", thongKeService.getDoanhThuTrieuDong());

        // Truyền Data Biểu đồ
        model.addAttribute("nguonKhachHang", thongKeService.getNguonKhachHangData());
        model.addAttribute("topCoupons", thongKeService.getTopMaKhuyenMaiDoanhThu());

        return "marketing/phan-tich";
    }
    // UC-MKT04 – Quản lý đánh giá (marketing view)
    @GetMapping("/danh-gia")
    public String danhGia(Model model) {
        // Danh sách đánh giá (Sắp xếp mới nhất lên đầu)
        List<DanhGia> danhGias = danhGiaRepository.findAll();
        danhGias.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        model.addAttribute("danhGias", danhGias);

        // Các chỉ số Stats Grid
        model.addAttribute("diemDanhGiaTB", thongKeService.getDiemDanhGiaTrungBinh());
        model.addAttribute("tongLuotDanhGia", thongKeService.getTongLuotDanhGia());
        model.addAttribute("chiSoNPS", thongKeService.getChiSoNPS());
        model.addAttribute("tyLeCSAT", thongKeService.getTyLeCSAT());
        model.addAttribute("tongTestimonial", thongKeService.getTongTestimonial());

        // Dữ liệu cho Biểu đồ
        model.addAttribute("diemCSATTheoDichVu", thongKeService.getDiemCSATTheoDichVu());
        model.addAttribute("tyLePhanBoSao", thongKeService.getTyLePhanBoSao());

        return "marketing/danh-gia";
    }
    // Xử lý nút Ghim/Bỏ ghim đánh giá lên trang chủ
    @PostMapping("/danh-gia/cap-nhat-trang-thai")
    public String capNhatTrangThaiDanhGia(@RequestParam Integer id, @RequestParam String trangThai) {
        danhGiaRepository.findById(id).ifPresent(dg -> {
            dg.setTrangThai(trangThai);
            danhGiaRepository.save(dg);
        });
        return "redirect:/marketing/danh-gia";
    }
@PostMapping("/thong-bao/tao-moi")
    public String taoMoiChienDich(
            @RequestParam String tieuDe,
            @RequestParam String nhomNhan,
            @RequestParam String noiDung) {
        
        ThongBao tb = new ThongBao();
        // Tạo mã thông báo ngẫu nhiên dựa trên thời gian
        tb.setMaThongBao("TB-" + (System.currentTimeMillis() % 100000)); 
        tb.setTieuDe(tieuDe);
        tb.setNoiDung(noiDung);
        tb.setNhomNhan(nhomNhan);
        tb.setNguoiGui("Phòng Marketing");
        tb.setThoiGianGui(java.time.LocalDateTime.now());
        
        // CHỐT LUỒNG: Gán trạng thái "ChuaGui" để chuyển sang cho Giám đốc duyệt
        tb.setTrangThai("ChuaGui");
        
        thongBaoRepository.save(tb);
        
        return "redirect:/marketing/thong-bao";
    }
@PostMapping("/khuyen-mai/tao-moi")
    public String taoMoiKhuyenMai(
            @RequestParam String tenChuongTrinh, @RequestParam String maCoupon,
            @RequestParam String loaiGiamGia, @RequestParam java.math.BigDecimal mucGiam,
            @RequestParam java.math.BigDecimal dieuKienToiThieu, @RequestParam Integer gioiHanLuot,
            @RequestParam String ngayBatDau, @RequestParam String ngayKetThuc,
            RedirectAttributes redirectAttributes) {
            
        // Kiểm tra trùng Mã Coupon
        boolean isExist = maKhuyenMaiRepository.findAll().stream()
                .anyMatch(c -> c.getCodeKhuyenMai().equalsIgnoreCase(maCoupon.trim()));
        if (isExist) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon '" + maCoupon + "' đã tồn tại!");
            return "redirect:/marketing/khuyen-mai";
        }
        
        com.example.Model.ChuongTrinhKhuyenMai ct = new com.example.Model.ChuongTrinhKhuyenMai();
        ct.setMaChuongTrinh("CT-" + (System.currentTimeMillis() % 100000));
        ct.setTenChuongTrinh(tenChuongTrinh);
        ct.setLoaiGiam(loaiGiamGia);
        ct.setGiaTriGiam(mucGiam);
        ct.setDieuKienToiThieu(dieuKienToiThieu);
        
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
            java.time.LocalDate start = java.time.LocalDate.parse(ngayBatDau, formatter);
            java.time.LocalDate end = java.time.LocalDate.parse(ngayKetThuc, formatter);
            
            if (end.isBefore(start)) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Ngày kết thúc không được nhỏ hơn ngày bắt đầu!");
                return "redirect:/marketing/khuyen-mai";
            }
            ct.setNgayBatDau(start); 
            ct.setNgayKetThuc(end);
        } catch (Exception e) {
            ct.setNgayBatDau(java.time.LocalDate.now()); 
            ct.setNgayKetThuc(java.time.LocalDate.now().plusMonths(1));
        }

        // ĐƯA KHỞI TẠO MK LÊN ĐÂY ĐỂ TRÁNH LỖI BIÊN DỊCH
        com.example.Model.MaKhuyenMai mk = new com.example.Model.MaKhuyenMai();

        // Tự động gán trạng thái chiến dịch dựa vào mốc thời gian thực tế
        java.time.LocalDate today = java.time.LocalDate.now();
        if (ct.getNgayKetThuc().isBefore(today)) {
            ct.setTrangThai("DaKetThuc");
            mk.setTrangThai("HetHan"); // Mã tự động hết hạn nếu ngày kết thúc < hôm nay
        } else if (ct.getNgayBatDau().isAfter(today)) {
            ct.setTrangThai("SapDienRa");
             mk.setTrangThai("SapDienRa"); 
        } else {
            ct.setTrangThai("DangHoatDong");
            mk.setTrangThai("HoatDong");
        }
        
        chuongTrinhKhuyenMaiRepository.save(ct);
        
        mk.setMaKhuyenMai("MKM-" + (System.currentTimeMillis() % 100000));
        mk.setCodeKhuyenMai(maCoupon.trim().toUpperCase());
        mk.setSoLuotToiDa(gioiHanLuot); 
        mk.setSoLuotDaDung(0);
        mk.setChuongTrinhKhuyenMai(ct); 
        maKhuyenMaiRepository.save(mk);
        
        redirectAttributes.addFlashAttribute("success", "Đã phát hành thành công mã " + maCoupon.toUpperCase());
        return "redirect:/marketing/khuyen-mai";
    }
    // ==========================================
    // 2. XÓA (CÓ KIỂM TRA RÀNG BUỘC ĐÃ SỬ DỤNG)
    // ==========================================
    @PostMapping("/khuyen-mai/xoa")
    public String xoaKhuyenMai(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            maKhuyenMaiRepository.findById(id).ifPresent(mk -> {
                if (mk.getSoLuotDaDung() != null && mk.getSoLuotDaDung() > 0) {
                    throw new RuntimeException("IN_USE"); // Bắn lỗi ra catch để xử lý
                }
                // Nếu chưa ai dùng, cho phép xóa hoàn toàn
                Integer ctId = mk.getChuongTrinhKhuyenMai().getId();
                maKhuyenMaiRepository.delete(mk);
                chuongTrinhKhuyenMaiRepository.deleteById(ctId);
            });
            redirectAttributes.addFlashAttribute("success", "Đã xóa mã khuyến mãi thành công!");
        } catch (Exception e) {
            if (e.getMessage().equals("IN_USE")) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Không thể xóa! Đã có khách hàng sử dụng mã này.");
            } else {
                redirectAttributes.addFlashAttribute("error", "Lỗi ràng buộc hệ thống. Hãy đổi trạng thái thành Hết hạn thay vì xóa.");
            }
        }
        return "redirect:/marketing/khuyen-mai";
    }
    // ==========================================
    // 3. CẬP NHẬT (SỬA) KHUYẾN MÃI
    // ==========================================
    @PostMapping("/khuyen-mai/cap-nhat")
    public String capNhatKhuyenMai(
            @RequestParam Integer id, 
            @RequestParam String tenChuongTrinh, @RequestParam String maCoupon,
            @RequestParam String loaiGiamGia, @RequestParam java.math.BigDecimal mucGiam,
            @RequestParam java.math.BigDecimal dieuKienToiThieu, @RequestParam Integer gioiHanLuot,
            @RequestParam String ngayBatDau, @RequestParam String ngayKetThuc,
            // ĐÃ XÓA @RequestParam String trangThai VÌ KHÔNG CHO NGƯỜI DÙNG CHỈNH TAY NỮA!
            RedirectAttributes redirectAttributes) {

        try {
            boolean isExist = maKhuyenMaiRepository.findAll().stream()
                    .anyMatch(c -> c.getCodeKhuyenMai().equalsIgnoreCase(maCoupon.trim()) && !c.getId().equals(id));
            
            if (isExist) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon '" + maCoupon + "' bị trùng với mã khác!");
                return "redirect:/marketing/khuyen-mai";
            }

            maKhuyenMaiRepository.findById(id).ifPresent(mk -> {
                com.example.Model.ChuongTrinhKhuyenMai ct = mk.getChuongTrinhKhuyenMai();

                ct.setTenChuongTrinh(tenChuongTrinh);
                ct.setLoaiGiam(loaiGiamGia);
                ct.setGiaTriGiam(mucGiam);
                ct.setDieuKienToiThieu(dieuKienToiThieu);

                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                java.time.LocalDate start = java.time.LocalDate.parse(ngayBatDau, formatter);
                java.time.LocalDate end = java.time.LocalDate.parse(ngayKetThuc, formatter);
                
                if (end.isBefore(start)) {
                    throw new RuntimeException("INVALID_DATE");
                }
                
                ct.setNgayBatDau(start);
                ct.setNgayKetThuc(end);

                // ========================================================
                // TỰ ĐỘNG TÍNH TOÁN TRẠNG THÁI 100% DỰA VÀO HÔM NAY (23/09/2026)
                // ========================================================
                java.time.LocalDate today = java.time.LocalDate.now();
                
                if (end.isBefore(today)) {
                    // Ngày kết thúc nhỏ hơn hôm nay -> Hết hạn / Đã kết thúc
                    ct.setTrangThai("DaKetThuc");
                    mk.setTrangThai("HetHan");
                } else if (start.isAfter(today)) {
                    // Ngày bắt đầu lớn hơn hôm nay -> Sắp diễn ra
                    ct.setTrangThai("SapDienRa");
                     mk.setTrangThai("SapDienRa"); 
                } else {
                    // Nằm trong khoảng thời gian hiện tại -> Đang hoạt động
                    ct.setTrangThai("DangHoatDong");
                    mk.setTrangThai("HoatDong");
                }
                // ========================================================

                chuongTrinhKhuyenMaiRepository.save(ct); 

                mk.setCodeKhuyenMai(maCoupon.trim().toUpperCase());
                mk.setSoLuotToiDa(gioiHanLuot);
                maKhuyenMaiRepository.save(mk); 
            });

            redirectAttributes.addFlashAttribute("success", "Đã cập nhật mã " + maCoupon.toUpperCase() + " thành công!");
        } catch (Exception e) {
            if ("INVALID_DATE".equals(e.getMessage())) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Ngày kết thúc không được nhỏ hơn ngày bắt đầu!");
            } else {
                redirectAttributes.addFlashAttribute("error", "Lỗi định dạng dữ liệu khi cập nhật!");
            }
        }
        
        return "redirect:/marketing/khuyen-mai";
    }
}