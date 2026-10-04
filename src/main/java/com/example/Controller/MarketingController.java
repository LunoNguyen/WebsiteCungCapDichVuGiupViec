package com.example.Controller;
import com.example.Model.DanhGia;
import com.example.Model.LichSuSuDungKhuyenMai;
import com.example.Model.MaKhuyenMai;
import java.util.List;
import com.example.DTO.*;
import com.example.Model.ThongBao;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import com.example.Service.ThongBaoPhatService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
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
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final ThongBaoPhatService thongBaoPhatService;
    private final DanhGiaRepository danhGiaRepository;
    private final LichSuSuDungKhuyenMaiRepository lichSuSuDungKhuyenMaiRepository;

    public MarketingController(
            ThongKeService thongKeService,
            ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository,
            MaKhuyenMaiRepository maKhuyenMaiRepository,
            ThongBaoRepository thongBaoRepository,
            ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
            ThongBaoPhatService thongBaoPhatService,
            DanhGiaRepository danhGiaRepository,
            LichSuSuDungKhuyenMaiRepository lichSuSuDungKhuyenMaiRepository) {
        this.thongKeService = thongKeService;
        this.chuongTrinhKhuyenMaiRepository = chuongTrinhKhuyenMaiRepository;
        this.maKhuyenMaiRepository = maKhuyenMaiRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.thongBaoPhatService = thongBaoPhatService;
        this.danhGiaRepository = danhGiaRepository;
        this.lichSuSuDungKhuyenMaiRepository = lichSuSuDungKhuyenMaiRepository;
    }

    // UC-MKT05 – Lịch sử sử dụng khuyến mãi
    @GetMapping("/lich-su-khuyen-mai")
    public String lichSuKhuyenMai(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String maCode,
            Model model) {

        List<LichSuSuDungKhuyenMai> lichSuList = lichSuSuDungKhuyenMaiRepository.findAll();

        // Sắp xếp mới nhất lên đầu
        lichSuList = new java.util.ArrayList<>(lichSuList);
        lichSuList.sort((a, b) -> b.getNgaySuDung().compareTo(a.getNgaySuDung()));

        // Lọc theo tên khách hàng
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            lichSuList = lichSuList.stream()
                .filter(ls -> ls.getKhachHang() != null &&
                              ls.getKhachHang().getHoTen().toLowerCase().contains(kw))
                .toList();
            model.addAttribute("keyword", keyword);
        }

        // Lọc theo mã code khuyến mãi
        if (maCode != null && !maCode.trim().isEmpty()) {
            lichSuList = lichSuList.stream()
                .filter(ls -> ls.getKhuyenMai() != null &&
                              ls.getKhuyenMai().getCodeKhuyenMai().equalsIgnoreCase(maCode.trim()))
                .toList();
            model.addAttribute("maCode", maCode);
        }

        model.addAttribute("lichSuList", lichSuList);
        model.addAttribute("tongLuotSuDung", lichSuSuDungKhuyenMaiRepository.count());
        model.addAttribute("danhSachCode", maKhuyenMaiRepository.findAll());
        model.addAttribute("marketingStats", thongKeService.getMarketingStats());
        return "marketing/lich-su-khuyen-mai";
    }

    @GetMapping("/dashboard")
public String dashboard(
        @RequestParam(required = false) Integer nam,
        Model model) {

    int namChon = (nam != null) ? nam : java.time.LocalDate.now().getYear();

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

    // Bộ lọc năm — chỉ gọi 1 lần với namChon, KHÔNG cần dòng gọi getDoanhThu12Thang() không tham số nữa
    model.addAttribute("doanhThu12Thang", thongKeService.getDoanhThu12Thang(namChon));
    model.addAttribute("danhSachNam", thongKeService.getDanhSachNamCoDuLieu());
    model.addAttribute("namDangChon", namChon);

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
        
        // Tra cứu trạng thái thông báo và thời gian tạo cho từng coupon
        List<ThongBao> allTB = thongBaoRepository.findAll();
        java.util.Map<String, ThongBao> couponNotifMap = new java.util.HashMap<>();
        for (ThongBao tb : allTB) {
            if (tb.getTieuDe() != null && tb.getTieuDe().contains("Ưu đãi mới: Tặng bạn mã giảm giá ")) {
                String code = tb.getTieuDe().replace("Ưu đãi mới: Tặng bạn mã giảm giá ", "").trim().toUpperCase();
                couponNotifMap.put(code, tb);
            }
        }

        java.util.Map<Integer, Boolean> notifiedCoupons = new java.util.HashMap<>();
        java.util.Map<Integer, String> createdTimeCoupons = new java.util.HashMap<>();

        for (MaKhuyenMai cp : coupons) {
            boolean isNotified = false;
            String timeStr = "";

            String code = cp.getCodeKhuyenMai() != null ? cp.getCodeKhuyenMai().toUpperCase() : "";
            if (couponNotifMap.containsKey(code)) {
                isNotified = true;
                ThongBao tb = couponNotifMap.get(code);
                if (tb.getThoiGianGui() != null) {
                    timeStr = tb.getThoiGianGui().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy"));
                }
            }

            if (cp.getChuongTrinhKhuyenMai() != null && cp.getChuongTrinhKhuyenMai().getMoTa() != null) {
                String moTa = cp.getChuongTrinhKhuyenMai().getMoTa();
                if (moTa.contains("[DA_THONG_BAO]")) {
                    isNotified = true;
                }
                if (moTa.startsWith("Tạo lúc ")) {
                    timeStr = moTa.replace("Tạo lúc ", "").replace(" [DA_THONG_BAO]", "").trim();
                }
            }

            if (timeStr.isEmpty() && cp.getChuongTrinhKhuyenMai() != null && cp.getChuongTrinhKhuyenMai().getNgayBatDau() != null) {
                timeStr = cp.getChuongTrinhKhuyenMai().getNgayBatDau().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }

            notifiedCoupons.put(cp.getId(), isNotified);
            createdTimeCoupons.put(cp.getId(), timeStr);
        }

        model.addAttribute("notifiedCoupons", notifiedCoupons);
        model.addAttribute("createdTimeCoupons", createdTimeCoupons);
        model.addAttribute("coupons", coupons);
        return "marketing/khuyen-mai";
    }

    // UC-MKT02 – Quản lý thông báo (gửi đến KH/CTV)
    @GetMapping("/thong-bao")
    public String thongBao(Model model) {
        List<ThongBao> allThongBao = new java.util.ArrayList<>(thongBaoRepository.findAll());
        // Sắp xếp thời gian gửi mới nhất lên đầu, nếu null thì theo ID mới nhất lên đầu
        allThongBao.sort((a, b) -> {
            if (b.getThoiGianGui() != null && a.getThoiGianGui() != null) {
                return b.getThoiGianGui().compareTo(a.getThoiGianGui());
            }
            if (b.getThoiGianGui() != null) return 1;
            if (a.getThoiGianGui() != null) return -1;
            return Integer.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0);
        });
        model.addAttribute("thongBaos", allThongBao);
        
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
    @GetMapping("/phan-tich")
    public String phanTich(
            @RequestParam(required = false) Integer thang,
            @RequestParam(required = false) Integer nam,
            Model model) {

        int namChon = (nam != null) ? nam : java.time.LocalDate.now().getYear();

        var mktStats = thongKeService.getMarketingStats();
        model.addAttribute("marketingStats", mktStats);
        model.addAttribute("tongKhuyenMai", mktStats.getTongKhuyenMai());
        model.addAttribute("tongCoupons", mktStats.getTongCoupons());

        model.addAttribute("thangDangChon", thang);
        model.addAttribute("namDangChon", namChon);
        model.addAttribute("danhSachNam", thongKeService.getDanhSachNamCoDuLieu());

        // Tổng khách hàng MỚI trong kỳ (dữ liệu thật theo NgayDangKy)
        model.addAttribute("tongKhachHang", thongKeService.getTongKhachHangMoiTheoThangNam(thang, namChon));

        // Bảng chiến dịch: lọc theo ngày bắt đầu thật
        var khuyenMaiLoc = chuongTrinhKhuyenMaiRepository.findAll().stream()
                .filter(km -> km.getNgayBatDau() != null && km.getNgayBatDau().getYear() == namChon)
                .filter(km -> thang == null || km.getNgayBatDau().getMonthValue() == thang)
                .toList();
        model.addAttribute("khuyenMais", khuyenMaiLoc);

        model.addAttribute("tongDoanhThuTrieu", thongKeService.getDoanhThuTrieuDong());

        // Nguồn khách hàng: tổng thật theo kỳ, tỷ lệ từng kênh là ước lượng
        model.addAttribute("nguonKhachHang", thongKeService.getNguonKhachHangTheoThangNam(thang, namChon));

        // Top mã khuyến mãi theo doanh thu THẬT trong kỳ
        model.addAttribute("topCoupons", thongKeService.getTopMaKhuyenMaiTheoThangNam(thang, namChon));

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
            @RequestParam String noiDung,
            RedirectAttributes redirectAttributes) {
        
        ThongBao tb = new ThongBao();
        // Tạo mã thông báo ngẫu nhiên dựa trên thời gian
        tb.setMaThongBao(com.example.Service.MaSinh.tao("TB-")); 
        tb.setTieuDe(tieuDe);
        tb.setNoiDung(noiDung);
        tb.setNhomNhan(nhomNhan);
        tb.setNguoiGui("Phòng Marketing");
        tb.setThoiGianGui(java.time.LocalDateTime.now());
        
        // CHỐT LUỒNG: Gán trạng thái "ChuaGui" để chuyển sang cho Giám đốc duyệt
        tb.setTrangThai("ChuaGui");
        
        thongBaoRepository.save(tb);
        
        redirectAttributes.addFlashAttribute("success", "Đã tạo chiến dịch thành công! Vui lòng đợi Giám đốc duyệt.");
        return "redirect:/marketing/thong-bao";
    }
@PostMapping("/khuyen-mai/tao-moi")
    public String taoMoiKhuyenMai(
           @RequestParam String tenChuongTrinh, @RequestParam String maCoupon,
            @RequestParam String loaiGiamGia, @RequestParam java.math.BigDecimal mucGiam,
            @RequestParam java.math.BigDecimal dieuKienToiThieu, @RequestParam Integer gioiHanLuot,
            @RequestParam(required = false) java.math.BigDecimal soTienGiamToiDa,
            @RequestParam String ngayBatDau, @RequestParam String ngayKetThuc,
            @RequestParam(required = false, defaultValue = "false") Boolean guiThongBao,
            RedirectAttributes redirectAttributes) {
            
        // 1. Kiểm tra trùng Mã Coupon
        boolean isExist = maKhuyenMaiRepository.findAll().stream()
                .anyMatch(c -> c.getCodeKhuyenMai().equalsIgnoreCase(maCoupon.trim()));
        if (isExist) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon '" + maCoupon + "' đã tồn tại!");
            return "redirect:/marketing/khuyen-mai";
        } 
                // Kiểm tra Giảm tối đa: không âm và không vượt quá đơn hàng tối thiểu
        if (soTienGiamToiDa != null) {
            if (soTienGiamToiDa.compareTo(java.math.BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Số tiền giảm tối đa không được là số âm!");
                return "redirect:/marketing/khuyen-mai";
            }
            if (dieuKienToiThieu != null && soTienGiamToiDa.compareTo(dieuKienToiThieu) > 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Số tiền giảm tối đa không được vượt quá đơn hàng tối thiểu!");
                return "redirect:/marketing/khuyen-mai";
            }
        }

        // 2. Kiểm tra ràng buộc Mức giảm (Bao quát cả % và tiền mặt)
        if ("PhanTram".equals(loaiGiamGia)) {
            if (mucGiam.compareTo(java.math.BigDecimal.ZERO) <= 0 || mucGiam.compareTo(new java.math.BigDecimal("100")) > 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Mức giảm phần trăm phải từ 1% đến 100%!");
                return "redirect:/marketing/khuyen-mai";
            }
        } else {
            if (mucGiam.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Số tiền giảm phải lớn hơn 0đ!");
                return "redirect:/marketing/khuyen-mai";
            }
        }
        // Kiểm tra Tên chương trình (Chặn nhập toàn dấu cách)
        String tenChuongTrinhClean = (tenChuongTrinh == null) ? "" : tenChuongTrinh.trim().replaceAll("\\s+", " ");
        
        if (tenChuongTrinhClean.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Tên chương trình không được để trống!");
            return "redirect:/marketing/khuyen-mai";
        }

        // Kiểm tra Mã Coupon (Bắt buộc chỉ chứa chữ và số, KHÔNG dấu cách, KHÔNG ký tự đặc biệt)
        String maKhuyenMaiClean = maCoupon.trim();
        if (maKhuyenMaiClean.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon không được để trống!");
            return "redirect:/marketing/khuyen-mai";
        }
        if (!maKhuyenMaiClean.matches("^[a-zA-Z0-9]+$")) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon viết liền không dấu, chỉ gồm chữ cái và số!");
            return "redirect:/marketing/khuyen-mai";
        }
        // 3. Kiểm tra giới hạn lượt dùng
        if (gioiHanLuot <= 0) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Giới hạn số lượt dùng phải lớn hơn 0!");
            return "redirect:/marketing/khuyen-mai";
        }
        // 2. Kiểm tra đơn hàng tối thiểu không được âm
        if (dieuKienToiThieu != null && dieuKienToiThieu.compareTo(java.math.BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Đơn hàng tối thiểu không được là số âm!");
            return "redirect:/marketing/khuyen-mai";
        }
        
        com.example.Model.ChuongTrinhKhuyenMai ct = new com.example.Model.ChuongTrinhKhuyenMai();
        ct.setMaChuongTrinh(com.example.Service.MaSinh.tao("CT-"));
        ct.setTenChuongTrinh(tenChuongTrinhClean);
        ct.setLoaiGiam(loaiGiamGia);
        ct.setGiaTriGiam(mucGiam);
        ct.setDieuKienToiThieu(dieuKienToiThieu);
        ct.setSoTienGiamToiDa(soTienGiamToiDa);
        String thoiGianTaoStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy"));
        ct.setMoTa("Tạo lúc " + thoiGianTaoStr + (Boolean.TRUE.equals(guiThongBao) ? " [DA_THONG_BAO]" : ""));
        
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
        
        mk.setMaKhuyenMai(com.example.Service.MaSinh.tao("MKM-"));
        mk.setCodeKhuyenMai(maCoupon.trim().toUpperCase());
        mk.setSoLuotToiDa(gioiHanLuot); 
        mk.setSoLuotDaDung(0);
        mk.setChuongTrinhKhuyenMai(ct); 
        maKhuyenMaiRepository.save(mk);
        
        // Tự động phát thông báo khuyến mãi gửi tới toàn thể Khách hàng nếu MKT tích chọn
        if (Boolean.TRUE.equals(guiThongBao)) {
            try {
                String giamStr = "PhanTram".equals(loaiGiamGia)
                        ? (mucGiam.stripTrailingZeros().toPlainString() + "%")
                        : (String.format("%,d", mucGiam.longValue()) + "đ");
                String dieuKienStr = (dieuKienToiThieu != null && dieuKienToiThieu.compareTo(java.math.BigDecimal.ZERO) > 0)
                        ? (" cho đơn từ " + String.format("%,d", dieuKienToiThieu.longValue()) + "đ") : "";
                String hanDungStr = (ct.getNgayKetThuc() != null)
                        ? (" đến hết ngày " + ct.getNgayKetThuc().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))) : "";

                ThongBao tbKM = ThongBao.builder()
                        .maThongBao(com.example.Service.MaSinh.tao("TB-KM-"))
                        .tieuDe("Ưu đãi mới: Tặng bạn mã giảm giá " + maCoupon.trim().toUpperCase())
                        .noiDung("Neatify gửi tặng bạn mã ưu đãi " + maCoupon.trim().toUpperCase() + " giảm ngay " + giamStr + dieuKienStr + ". Áp dụng" + hanDungStr + ". Nhập mã ngay khi đặt dịch vụ để nhận ưu đãi!")
                        .nguoiGui("Phòng Marketing")
                        .nhomNhan("KhachHang")
                        .thoiGianGui(java.time.LocalDateTime.now())
                        .trangThai("DaGui")
                        .build();
                tbKM = thongBaoRepository.save(tbKM);
                thongBaoPhatService.phatChoNhom(tbKM.getId(), "KhachHang");
            } catch (Exception ignored) {}
        }

        if (Boolean.TRUE.equals(guiThongBao)) {
            redirectAttributes.addFlashAttribute("success", "Tạo mã " + maCoupon.trim().toUpperCase() + " thành công, đã gửi thông báo cho khách hàng!");
        } else {
            redirectAttributes.addFlashAttribute("success", "Tạo mã " + maCoupon.trim().toUpperCase() + " thành công!");
        }
        return "redirect:/marketing/khuyen-mai";
    }
    // ==========================================
    // 2. XÓA (CÓ KIỂM TRA RÀNG BUỘC ĐÃ SỬ DỤNG VÀ THU HỒI THÔNG BÁO)
    // ==========================================
    @Transactional
    @PostMapping("/khuyen-mai/xoa")
    public String xoaKhuyenMai(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            maKhuyenMaiRepository.findById(id).ifPresent(mk -> {
                if (mk.getSoLuotDaDung() != null && mk.getSoLuotDaDung() > 0) {
                    throw new RuntimeException("IN_USE"); // Bắn lỗi ra catch để xử lý
                }

                String code = (mk.getCodeKhuyenMai() != null) ? mk.getCodeKhuyenMai().trim().toUpperCase() : "";

                // 1. Tự động tìm và xóa toàn bộ thông báo đã gửi cho khách hàng liên quan đến mã này
                if (!code.isEmpty()) {
                    List<ThongBao> allTB = thongBaoRepository.findAll();
                    for (ThongBao tb : allTB) {
                        boolean matchCode = false;
                        if (tb.getTieuDe() != null && (
                                tb.getTieuDe().equalsIgnoreCase("Ưu đãi mới: Tặng bạn mã giảm giá " + code) ||
                                tb.getTieuDe().toUpperCase().contains("MÃ GIẢM GIÁ " + code) ||
                                tb.getTieuDe().toUpperCase().contains(" " + code) ||
                                tb.getTieuDe().toUpperCase().endsWith(code)
                        )) {
                            matchCode = true;
                        }

                        if (matchCode) {
                            // Xóa các bản ghi thông báo trong hộp thư người dùng trước (tránh khóa ngoại)
                            List<com.example.Model.ThongBaoNguoiDung> tbndList = thongBaoNguoiDungRepository.findByThongBao_Id(tb.getId());
                            if (tbndList != null && !tbndList.isEmpty()) {
                                thongBaoNguoiDungRepository.deleteAll(tbndList);
                            }
                            // Xóa bản ghi thông báo chiến dịch
                            thongBaoRepository.delete(tb);
                        }
                    }
                }

                // 2. Nếu chưa ai dùng, cho phép xóa hoàn toàn mã & chương trình
                Integer ctId = (mk.getChuongTrinhKhuyenMai() != null) ? mk.getChuongTrinhKhuyenMai().getId() : null;
                maKhuyenMaiRepository.delete(mk);
                if (ctId != null) {
                    chuongTrinhKhuyenMaiRepository.deleteById(ctId);
                }
            });
            redirectAttributes.addFlashAttribute("success", "Đã xóa mã khuyến mãi và thu hồi thông báo thành công!");
        } catch (Exception e) {
            if ("IN_USE".equals(e.getMessage())) {
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
            @RequestParam(required = false) java.math.BigDecimal soTienGiamToiDa,
            @RequestParam java.math.BigDecimal dieuKienToiThieu, @RequestParam Integer gioiHanLuot,
            @RequestParam String ngayBatDau, @RequestParam String ngayKetThuc,
            RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra Tên chương trình (Chặn nhập toàn dấu cách)
        String tenChuongTrinhClean = (tenChuongTrinh == null) ? "" : tenChuongTrinh.trim().replaceAll("\\s+", " ");
        
        if (tenChuongTrinhClean.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Tên chương trình không được để trống!");
            return "redirect:/marketing/khuyen-mai";
        }
        // Kiểm tra Giảm tối đa: không âm và không vượt quá đơn hàng tối thiểu
        if (soTienGiamToiDa != null) {
            if (soTienGiamToiDa.compareTo(java.math.BigDecimal.ZERO) < 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Số tiền giảm tối đa không được là số âm!");
                return "redirect:/marketing/khuyen-mai";
            }
            if (dieuKienToiThieu != null && soTienGiamToiDa.compareTo(dieuKienToiThieu) > 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Số tiền giảm tối đa không được vượt quá đơn hàng tối thiểu!");
                return "redirect:/marketing/khuyen-mai";
            }
        }
        // 2. Kiểm tra Mã Coupon (Bắt buộc chỉ chứa chữ và số, KHÔNG dấu cách, KHÔNG ký tự đặc biệt)
        String maKhuyenMaiClean = maCoupon.trim();
        if (maKhuyenMaiClean.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon không được để trống!");
            return "redirect:/marketing/khuyen-mai";
        }
        if (!maKhuyenMaiClean.matches("^[a-zA-Z0-9]+$")) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon viết liền không dấu, chỉ gồm chữ cái và số!");
            return "redirect:/marketing/khuyen-mai";
        }

        // 3. Kiểm tra trùng Mã Coupon (bỏ qua ID hiện tại đang sửa)
        boolean isExist = maKhuyenMaiRepository.findAll().stream()
                .anyMatch(c -> c.getCodeKhuyenMai().equalsIgnoreCase(maKhuyenMaiClean) && !c.getId().equals(id));
        if (isExist) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Mã Coupon '" + maKhuyenMaiClean + "' bị trùng với một mã khác!");
            return "redirect:/marketing/khuyen-mai";
        }

        // 4. Kiểm tra ràng buộc Mức giảm (Bao quát cả % và tiền mặt)
        if ("PhanTram".equals(loaiGiamGia)) {
            if (mucGiam.compareTo(java.math.BigDecimal.ZERO) <= 0 || mucGiam.compareTo(new java.math.BigDecimal("100")) > 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Mức giảm phần trăm phải từ 1% đến 100%!");
                return "redirect:/marketing/khuyen-mai";
            }
        } else {
            if (mucGiam.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Số tiền giảm phải lớn hơn 0đ!");
                return "redirect:/marketing/khuyen-mai";
            }
        }

        // 5. Kiểm tra giới hạn lượt dùng
        if (gioiHanLuot <= 0) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Giới hạn số lượt dùng phải lớn hơn 0!");
            return "redirect:/marketing/khuyen-mai";
        }

        // 6. Kiểm tra đơn hàng tối thiểu không được âm
        if (dieuKienToiThieu != null && dieuKienToiThieu.compareTo(java.math.BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: Đơn hàng tối thiểu không được là số âm!");
            return "redirect:/marketing/khuyen-mai";
        }

        // NẾU VƯỢT QUA TẤT CẢ RÀO CHẮN TRÊN, BẮT ĐẦU CẬP NHẬT DATABASE
        try {
            maKhuyenMaiRepository.findById(id).ifPresent(mk -> {
                com.example.Model.ChuongTrinhKhuyenMai ct = mk.getChuongTrinhKhuyenMai();

                ct.setTenChuongTrinh(tenChuongTrinhClean.trim());
                ct.setLoaiGiam(loaiGiamGia);
                ct.setGiaTriGiam(mucGiam);
                ct.setDieuKienToiThieu(dieuKienToiThieu);
                ct.setSoTienGiamToiDa(soTienGiamToiDa);

                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
                java.time.LocalDate start = java.time.LocalDate.parse(ngayBatDau, formatter);
                java.time.LocalDate end = java.time.LocalDate.parse(ngayKetThuc, formatter);
                
                // Tránh lỗi kết thúc trước bắt đầu
                if (end.isBefore(start)) {
                    throw new RuntimeException("INVALID_DATE");
                }
                
                ct.setNgayBatDau(start);
                ct.setNgayKetThuc(end);

                // ========================================================
                // TỰ ĐỘNG TÍNH TOÁN TRẠNG THÁI THEO THỜI GIAN THỰC TẾ
                // ========================================================
                java.time.LocalDate today = java.time.LocalDate.now();
                
                if (end.isBefore(today)) {
                    ct.setTrangThai("DaKetThuc");
                    mk.setTrangThai("HetHan");
                } else if (start.isAfter(today)) {
                    ct.setTrangThai("SapDienRa");
                    mk.setTrangThai("SapDienRa"); 
                } else {
                    ct.setTrangThai("DangHoatDong");
                    mk.setTrangThai("HoatDong");
                }
                // ========================================================

                chuongTrinhKhuyenMaiRepository.save(ct); 

                mk.setCodeKhuyenMai(maKhuyenMaiClean.toUpperCase());
                mk.setSoLuotToiDa(gioiHanLuot);
                maKhuyenMaiRepository.save(mk); 
            });

            redirectAttributes.addFlashAttribute("success", "Đã cập nhật mã " + maKhuyenMaiClean.toUpperCase() + " thành công!");
      } catch (Exception e) {
            e.printStackTrace(); 
            
            if ("INVALID_DATE".equals(e.getMessage())) {
                redirectAttributes.addFlashAttribute("error", "Lỗi: Ngày kết thúc không được nhỏ hơn ngày bắt đầu!");
            } else {
                // Nối thêm thông báo gốc của Java vào giao diện để biết nguyên nhân thật sự
                redirectAttributes.addFlashAttribute("error", "Lỗi hệ thống: " + e.getMessage());
            }
        }
        
        return "redirect:/marketing/khuyen-mai";
    }
}