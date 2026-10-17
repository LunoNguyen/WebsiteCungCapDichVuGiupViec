package com.example.Controller;

import com.example.DTO.*;
import com.example.Model.*;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * CSKH module controllers – UC-CSKH01 to UC-CSKH06
 * Gọi dữ liệu trực tiếp từ CSDL thay vì gán tĩnh.
 * Layout đồng nhất sử dụng cskh-layout.html
 */
@Controller
@RequestMapping("/cskh")
public class CSKHController {

    private final ThongKeService thongKeService;
    private final KhachHangRepository khachHangRepository;
    private final DonDatDichVuRepository donDatDichVuRepository;
    private final KhieuNaiRepository khieuNaiRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final ThongBaoRepository thongBaoRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.ThongBaoService thongBaoService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.PhanCongService phanCongService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.DanhGiaService danhGiaService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.KhuVucCtvService khuVucCtvService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.DiaGioiService diaGioiService;
    private final CongTacVienRepository congTacVienRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final PhanCongCTVRepository phanCongCTVRepository;
    private final PhanHoiDanhGiaRepository phanHoiDanhGiaRepository;
    private final DichVuRepository dichVuRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final KhuVucRepository khuVucRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public CSKHController(
            ThongKeService thongKeService,
            KhachHangRepository khachHangRepository,
            DonDatDichVuRepository donDatDichVuRepository,
            KhieuNaiRepository khieuNaiRepository,
            DanhGiaRepository danhGiaRepository,
            ThongBaoRepository thongBaoRepository,
            CongTacVienRepository congTacVienRepository,
            LichLamViecRepository lichLamViecRepository,
            PhanCongCTVRepository phanCongCTVRepository,
            PhanHoiDanhGiaRepository phanHoiDanhGiaRepository,
            DichVuRepository dichVuRepository,
            DiaChiKhachHangRepository diaChiKhachHangRepository,
            KhuVucRepository khuVucRepository,
            ThongBaoNguoiDungRepository thongBaoNguoiDungRepository) {
        this.thongKeService = thongKeService;
        this.khachHangRepository = khachHangRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.phanHoiDanhGiaRepository = phanHoiDanhGiaRepository;
        this.dichVuRepository = dichVuRepository;
        this.diaChiKhachHangRepository = diaChiKhachHangRepository;
        this.khuVucRepository = khuVucRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
    }

    // ── Dashboard ────────────────────────────────────────────────────
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var allOrders    = donDatDichVuRepository.findAll();
        var allComplaints = khieuNaiRepository.findAll();

        long donChoDuyet     = allOrders.stream().filter(d -> "ChoDuyet".equalsIgnoreCase(d.getTrangThai())).count();
        long donHoanThanh    = allOrders.stream().filter(d -> "HoanThanh".equalsIgnoreCase(d.getTrangThai())).count();
        long donDangThucHien = allOrders.stream().filter(d -> "DangThucHien".equalsIgnoreCase(d.getTrangThai())).count();

        var donStats = thongKeService.getDonHangStats();
        var khStats  = thongKeService.getKhachHangStats();
        var knStats  = thongKeService.getKhieuNaiStats();

        model.addAttribute("donHangStats",    donStats);
        model.addAttribute("khachHangStats",  khStats);
        model.addAttribute("khieuNaiStats",   knStats);
        model.addAttribute("tongKhachHang",   khStats.getTongKhachHang());
        model.addAttribute("tongDonHang",     donStats.getTongDonHang());
        model.addAttribute("tongKhieuNai",    knStats.getTongKhieuNai());
        model.addAttribute("khieuNaiChuaXuLyCount", knStats.getChuaXuLyCount() + knStats.getDangXuLyCount());
        model.addAttribute("donChoDuyetCount",      donStats.getChoDuyetCount());
        model.addAttribute("donHoanThanhCount",     donStats.getHoanThanhCount());
        model.addAttribute("donDangThucHienCount",  donStats.getDangThucHienCount());
        model.addAttribute("diemDanhGiaTB",   thongKeService.getDiemDanhGiaTrungBinh());
        model.addAttribute("danhGiaChoDuyet", danhGiaService.demChoDuyet());
        model.addAttribute("recentOrders",    thongKeService.getDonHangGanDay(8));
        model.addAttribute("recentComplaints",thongKeService.getKhieuNaiGanDay(5));
        model.addAttribute("phanBoDon",       thongKeService.getPhanBoTrangThaiDon());
        return "cskh/dashboard";
    }

    // ── UC-CSKH01 – Quản lý khách hàng ──────────────────────────────
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.KhuVucService khuVucService;

    @GetMapping("/khach-hang")
    public String khachHang(Model model) {
        // Quận, huyện TP.HCM: cùng danh sách với sổ địa chỉ khách hàng và đăng ký CTV
        model.addAttribute("khuVucs", khuVucService.danhSach());
        model.addAttribute("tinhThanhPhucVu", khuVucService.getTinhThanh());
        model.addAttribute("khachHangStats", thongKeService.getKhachHangStats());
        var khachHangs = khachHangRepository.findAll();
        model.addAttribute("khachHangs", khachHangs);

        Map<Integer, DiaChiKhachHang> defaultAddressMap = new HashMap<>();
        Map<Integer, Long> orderCountMap = new HashMap<>();
        Map<Integer, BigDecimal> totalSpentMap = new HashMap<>();

        for (var kh : khachHangs) {
            var addresses = diaChiKhachHangRepository.findByKhachHang_Id(kh.getId());
            if (!addresses.isEmpty()) {
                var macDinh = addresses.stream()
                        .filter(d -> Boolean.TRUE.equals(d.getLaMacDinh()))
                        .findFirst()
                        .orElse(addresses.get(0));
                defaultAddressMap.put(kh.getId(), macDinh);
            }
            var orders = donDatDichVuRepository.findByKhachHang_IdOrderByNgayTaoDesc(kh.getId());
            long completedOrders = orders.stream().filter(o -> "HoanThanh".equalsIgnoreCase(o.getTrangThai())).count();
            BigDecimal spent = orders.stream()
                    .filter(o -> "HoanThanh".equalsIgnoreCase(o.getTrangThai()) && o.getThanhTien() != null)
                    .map(DonDatDichVu::getThanhTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            orderCountMap.put(kh.getId(), completedOrders);
            totalSpentMap.put(kh.getId(), spent);
        }

        model.addAttribute("defaultAddressMap", defaultAddressMap);
        model.addAttribute("orderCountMap", orderCountMap);
        model.addAttribute("totalSpentMap", totalSpentMap);
        return "cskh/khach-hang";
    }

    /**
     * Thêm khách hàng mới với địa chỉ được xác nhận qua API https://provinces.open-api.vn/api/v2
     * Tự động lưu hoặc liên kết thông tin vào bảng KhuVuc và bảng DiaChiKhachHang
     */
    @PostMapping("/khach-hang/them")
    @Transactional
    public Object themKhachHang(
            @RequestParam String hoTen,
            @RequestParam String soDienThoai,
            @RequestParam(required = false) String email,
            @RequestParam(required = false, defaultValue = "Khac") String gioiTinh,
            @RequestParam(required = false) String ngaySinh,
            @RequestParam(required = false) Integer maTinh,
            @RequestParam(required = false) Integer maPhuong,
            @RequestParam(required = false) Integer khuVucId,
            @RequestParam(required = false) String soNhaTenDuong,
            @RequestParam(required = false) BigDecimal dienTichNha,
            @RequestParam(required = false) String ghiChu,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        try {
            if (hoTen == null || hoTen.trim().isEmpty()) {
                throw new IllegalArgumentException("Họ và tên khách hàng không được để trống!");
            }
            if (soDienThoai == null || !soDienThoai.trim().matches("\\d{9,11}")) {
                throw new IllegalArgumentException("Số điện thoại không hợp lệ (cần 9-11 chữ số, ví dụ 0901234567)!");
            }

            // Kiểm tra số điện thoại đã tồn tại chưa
            var existingByPhone = khachHangRepository.findBySoDienThoai(soDienThoai.trim());
            if (existingByPhone.isPresent()) {
                throw new IllegalArgumentException("Số điện thoại '" + soDienThoai.trim() + "' đã được đăng ký cho khách hàng: " + existingByPhone.get().getHoTen());
            }

            // 1. Địa chỉ: tỉnh, phường (provinces.open-api.vn v2) + số nhà; khu vực (quận, huyện) suy ra từ phường
            com.example.Service.DiaGioiService.DiaChiDaXacDinh dcXacDinh =
                    diaGioiService.xacDinh(soNhaTenDuong, maTinh, maPhuong, khuVucId);
            KhuVuc kv = dcXacDinh.khuVuc(); // null = ngoài khu vực phục vụ: vẫn lưu, chưa đặt dịch vụ được

            // 2. Tạo bản ghi KhachHang
            String maKhachHang = com.example.Service.MaSinh.tao("KH-");
            int retryKh = 0;
            while (khachHangRepository.findByMaKhachHang(maKhachHang).isPresent() && retryKh < 20) {
                maKhachHang = com.example.Service.MaSinh.tao("KH-");
                retryKh++;
            }

            LocalDate ngaySinhDate = null;
            if (ngaySinh != null && !ngaySinh.isBlank()) {
                try {
                    ngaySinhDate = LocalDate.parse(ngaySinh.trim());
                } catch (Exception ignored) {}
            }

            KhachHang khachHang = KhachHang.builder()
                    .maKhachHang(maKhachHang)
                    .hoTen(hoTen.trim())
                    .soDienThoai(soDienThoai.trim())
                    .email(email != null && !email.isBlank() ? email.trim() : null)
                    .gioiTinh(gioiTinh != null && !gioiTinh.isBlank() ? gioiTinh.trim() : "Khac")
                    .ngaySinh(ngaySinhDate)
                    .ngayDangKy(LocalDate.now())
                    .trangThai("HoatDong")
                    .build();
            khachHang = khachHangRepository.save(khachHang);

            // 3. Tạo bản ghi DiaChiKhachHang
            String fullAddress = dcXacDinh.diaChiDayDu();

            String maDiaChi = com.example.Service.MaSinh.tao("DC-");
            int retryDc = 0;
            while (diaChiKhachHangRepository.findByMaDiaChi(maDiaChi).isPresent() && retryDc < 20) {
                maDiaChi = com.example.Service.MaSinh.tao("DC-");
                retryDc++;
            }

            DiaChiKhachHang diaChi = DiaChiKhachHang.builder()
                    .maDiaChi(maDiaChi)
                    .khachHang(khachHang)
                    .khuVuc(kv)
                    .diaChiChiTiet(fullAddress)
                    .dienTichNha(dienTichNha)
                    .luuYDacBiet(ghiChu != null && !ghiChu.isBlank() ? ghiChu.trim() : null)
                    .laMacDinh(true)
                    .trangThai("HoatDong")
                    .build();
            diaChiKhachHangRepository.save(diaChi);

            if (isAjax) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thêm khách hàng '" + khachHang.getHoTen() + "' thành công!",
                        "customerId", khachHang.getId(),
                        "maKhachHang", khachHang.getMaKhachHang()
                ));
            }

            redirectAttributes.addFlashAttribute("successMessage", "Thêm khách hàng '" + khachHang.getHoTen() + "' (" + khachHang.getMaKhachHang() + ") thành công!");
            return "redirect:/cskh/khach-hang";

        } catch (Exception ex) {
            // Hoàn tác phần đã lưu dở (khu vực, khách hàng...) khi có lỗi giữa chừng
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            if (isAjax) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", ex.getMessage() != null ? ex.getMessage() : "Có lỗi xảy ra khi lưu khách hàng!"
                ));
            }
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Có lỗi xảy ra khi lưu khách hàng!");
            return "redirect:/cskh/khach-hang";
        }
    }

    /**
     * Cập nhật trạng thái Khách hàng (HoatDong / BiKhoa)
     */
    @PostMapping("/khach-hang/doi-trang-thai")
    @Transactional
    public String doiTrangThaiKhachHang(
            @RequestParam Integer id,
            @RequestParam String trangThai,
            RedirectAttributes redirectAttributes) {
        var khOpt = khachHangRepository.findById(id);
        if (khOpt.isPresent()) {
            var kh = khOpt.get();
            kh.setTrangThai(trangThai);
            khachHangRepository.save(kh);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chuyển trạng thái khách hàng '" + kh.getHoTen() + "' thành " + ("HoatDong".equals(trangThai) ? "Hoạt động" : "Bị khóa") + "!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khách hàng!");
        }
        return "redirect:/cskh/khach-hang";
    }

    /**
     * Fallback proxy API lấy danh sách Tỉnh/Thành phố từ https://provinces.open-api.vn/api/v2/
     */
    @GetMapping(value = "/api/provinces", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getProvincesProxy() {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://provinces.open-api.vn/api/v2/p/"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("[]");
        }
    }

    /**
     * Fallback proxy API lấy danh sách Phường/Xã theo tỉnh thành từ https://provinces.open-api.vn/api/v2/w/?province=...
     */
    @GetMapping(value = "/api/wards", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<String> getWardsProxy(@RequestParam(defaultValue = "0") int province) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create("https://provinces.open-api.vn/api/v2/w/?province=" + province))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            var res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(res.statusCode()).body(res.body());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("[]");
        }
    }

    // ── UC-CSKH02 – Quản lý đơn đặt dịch vụ (CRUD) ─────────────────
    @GetMapping("/don-dat-dich-vu")
    @Transactional
    public String donDatDichVu(Model model) {
        model.addAttribute("donHangStats",   thongKeService.getDonHangStats());
        model.addAttribute("donDatDichVus",  donDatDichVuRepository.findAllWithDetails());
        model.addAttribute("dichVus",        dichVuRepository.findAll());
        model.addAttribute("khachHangs",     khachHangRepository.findAll());
        model.addAttribute("khuVucs",        khuVucService.danhSach());
        return "cskh/don-dat-dich-vu";
    }

    // ── UC-CSKH03 – Quản lý khiếu nại (level 1-2) ───────────────────
    @GetMapping("/khieu-nai")
    public String khieuNai(Model model) {
        model.addAttribute("khieuNaiStats", thongKeService.getKhieuNaiStats());
        model.addAttribute("khieuNais",     khieuNaiRepository.findAll());
        return "cskh/khieu-nai";
    }

    // ── UC-CSKH04 – Quản lý đánh giá (CRUD + trả lời) ───────────────
    @GetMapping("/danh-gia")
    public String danhGia(Model model, @RequestParam(required = false) String trangThai) {
        var danhGiaList = new java.util.ArrayList<>(danhGiaRepository.findAll());
        danhGiaList.sort((a, b) -> Integer.compare(b.getId(), a.getId()));

        long lowRating = danhGiaList.stream()
                .filter(dg -> (dg.getDiemChatLuong() + dg.getDiemThaiDo()) / 2.0 < 3)
                .count();
        // Chỉ tính đánh giá đã có nhân viên trả lời (bình luận của khách không tính)
        var daTraLoi = danhGiaService.daCoNhanVienTraLoi();
        long replied = daTraLoi.size();

        model.addAttribute("danhGias",       danhGiaList);
        model.addAttribute("diemDanhGiaTB",  thongKeService.getDiemDanhGiaTrungBinh());
        model.addAttribute("lowRatingCount", lowRating);
        model.addAttribute("repliedCount",   replied);
        model.addAttribute("daTraLoi",       daTraLoi);
        model.addAttribute("choDuyetCount",  danhGiaService.demChoDuyet());
        model.addAttribute("trangThaiLoc",   trangThai != null && com.example.Service.DanhGiaService.TRANG_THAI.contains(trangThai) ? trangThai : null);
        return "cskh/danh-gia";
    }

    /** Duyệt (HienThi), ẩn (An) hoặc đưa về chờ duyệt (ChoDuyet) một hoặc nhiều đánh giá. */
    @PostMapping("/danh-gia/trang-thai")
    @ResponseBody
    public ResponseEntity<?> doiTrangThaiDanhGia(@RequestParam(required = false) java.util.List<Integer> ids,
                                                 @RequestParam String trangThai) {
        try {
            return ResponseEntity.ok(Map.of("soLuong", danhGiaService.doiTrangThai(ids, trangThai)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Đánh giá kèm trao đổi giữa khách và CSKH (mở hộp thoại "Phản hồi đánh giá"). */
    @GetMapping("/danh-gia/{id}/trao-doi")
    @ResponseBody
    public ResponseEntity<?> traoDoiDanhGia(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(danhGiaService.chiTiet(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** Nhân viên CSKH trả lời khách: hiện ngay trong mục đánh giá của đơn trên web và app. */
    @PostMapping("/danh-gia/{id}/tra-loi")
    @ResponseBody
    public ResponseEntity<?> traLoiDanhGia(@PathVariable Integer id, @RequestParam String noiDung,
                                           jakarta.servlet.http.HttpSession session) {
        try {
            Integer taiKhoanId = session.getAttribute("authenticatedUserId") instanceof Number n ? n.intValue() : null;
            return ResponseEntity.ok(danhGiaService.traLoiCuaNhanVien(id, taiKhoanId, noiDung));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ── UC-CSKH05 – Lịch làm việc CTV (calendar view) ───────────────
    @GetMapping("/lich-phan-cong")
    public String lichPhanCong(Model model) {
        var congTacViens = congTacVienRepository.findAll();
        var donDatDichVus = donDatDichVuRepository.findAllWithDetails();
        var lichLamViecs = lichLamViecRepository.findAllWithDetails();

        var dtoList = lichLamViecs.stream()
                .map(this::toLichLamViecDTO)
                .toList();

        // Convert DTOs to JSON for FullCalendar initialization
        String dtoListJson = "[]";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            dtoListJson = mapper.writeValueAsString(dtoList);
        } catch (Exception e) {
            // fallback to empty array
        }

        // Tính toán trạng thái các ca trực hôm nay cho Bảng ca trực
        Map<Integer, Map<String, String>> ctvShiftsMap = new HashMap<>();
        LocalDate today = LocalDate.now();
        for (CongTacVien ctv : congTacViens) {
            Map<String, String> shifts = new HashMap<>();
            shifts.put("sang", "Sẵn sàng ca sáng");
            shifts.put("sangClass", "badge-success");
            shifts.put("chieu", "Sẵn sàng ca chiều");
            shifts.put("chieuClass", "badge-primary");
            shifts.put("toi", "Sẵn sàng ca tối");
            shifts.put("toiClass", "badge-secondary");

            for (LichLamViecDTO item : dtoList) {
                if (item.getCongTacVienId() != null && item.getCongTacVienId().equals(ctv.getId())
                        && item.getNgayLam() != null) {
                    if (item.getGioBatDau() != null) {
                        int hour = item.getGioBatDau().getHour();
                        String serviceName = item.getDichVuTen() != null ? item.getDichVuTen() : "Có ca trực";
                        if (serviceName.length() > 22) serviceName = serviceName.substring(0, 20) + "...";
                        String badgeText = "Có ca: " + serviceName;

                        if (hour < 12) {
                            shifts.put("sang", badgeText);
                            shifts.put("sangClass", "badge-warning");
                        } else if (hour < 17) {
                            shifts.put("chieu", badgeText);
                            shifts.put("chieuClass", "badge-warning");
                        } else {
                            shifts.put("toi", badgeText);
                            shifts.put("toiClass", "badge-warning");
                        }
                    }
                }
            }
            ctvShiftsMap.put(ctv.getId(), shifts);
        }

        model.addAttribute("lichLamViecsJson",  dtoListJson);
        model.addAttribute("ctvShiftsMap",      ctvShiftsMap);
        // Quận, huyện nhận việc của từng CTV: lọc và hiển thị trong bảng
        model.addAttribute("khuVucs",           khuVucService.danhSach());
        model.addAttribute("khuVucCtv",         khuVucCtvService.theoCtv());
        model.addAttribute("donDatDichVus",     donDatDichVus);
        model.addAttribute("congTacViens",      congTacViens);
        model.addAttribute("lichLamViecs",      lichLamViecs);
        model.addAttribute("congTacVienStats",  thongKeService.getCongTacVienStats());
        model.addAttribute("donHangStats",      thongKeService.getDonHangStats());
        return "cskh/lich-phan-cong";
    }

    @GetMapping("/api/lich-lam-viec")
    @ResponseBody
    public ResponseEntity<java.util.List<LichLamViecDTO>> getLichLamViecApi(
            @RequestParam(required = false) Integer ctvId) {
        var list = (ctvId != null)
                ? lichLamViecRepository.findByCongTacVienIdWithDetails(ctvId)
                : lichLamViecRepository.findAllWithDetails();
        return ResponseEntity.ok(list.stream().map(this::toLichLamViecDTO).toList());
    }

    @PostMapping("/phan-cong/luu")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> luuPhanCong(
            @RequestParam Integer ctvId,
            @RequestParam Integer orderId,
            @RequestParam String ngayLam,
            @RequestParam String gioBatDau,
            @RequestParam String gioKetThuc,
            @RequestParam(required = false) String ghiChu) {
        try {
            // Khóa CTV và đơn trong lúc phân công: hai nhân viên CSKH phân công cùng lúc sẽ được xử lý lần lượt,
            // người sau thấy kết quả của người trước nên không tạo phân công trùng hay xếp trùng giờ.
            CongTacVien ctv = congTacVienRepository.findByIdForUpdate(ctvId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CTV"));
            DonDatDichVu dd = donDatDichVuRepository.findByIdForUpdate(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Đơn đặt dịch vụ"));

            LocalDate date = LocalDate.parse(ngayLam);
            java.time.LocalTime start = java.time.LocalTime.parse(gioBatDau);
            java.time.LocalTime end = java.time.LocalTime.parse(gioKetThuc);

            if ("DaHuy".equalsIgnoreCase(dd.getTrangThai()) || "HoanThanh".equalsIgnoreCase(dd.getTrangThai())) {
                throw new IllegalArgumentException("Đơn này đã hủy hoặc đã hoàn thành, không thể phân công.");
            }
            com.example.Service.PhanCongService.kiemTraKhongTuPhucVu(ctv, dd);
            if (phanCongCTVRepository.findByDonDat_IdAndCongTacVien_Id(orderId, ctvId).isPresent()) {
                throw new IllegalArgumentException("Cộng tác viên này đã được phân công cho đơn này.");
            }
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
            }
            // Trùng giờ, khoảng nghỉ 1 tiếng giữa hai việc, quá tải trong ngày, di chuyển khác tỉnh/thành
            // Gói tháng: mọi buổi trong tháng (tính từ ngày bắt đầu đã chọn) đều được kiểm tra và xếp lịch
            java.util.List<LocalDate> cacBuoi = com.example.Service.PhanCongService.cacBuoi(dd, date);
            phanCongService.kiemTraCacBuoiHoacBaoLoi(ctvId, cacBuoi, start, end,
                    dd.getDiaChi() != null ? dd.getDiaChi().getKhuVuc() : null, null, true);

            // 1. Lưu Phân công CTV ở trạng thái ChoPhanCong để CTV xác nhận qua Mobile
            PhanCongCTV pc = PhanCongCTV.builder()
                    .maPhanCong(com.example.Service.MaSinh.tao("PC-"))
                    .donDat(dd)
                    .congTacVien(ctv)
                    .trangThai("ChoPhanCong")
                    .thoiGianPhanCong(java.time.LocalDateTime.now())
                    .build();
            phanCongCTVRepository.save(pc);

            // 2. Tạo Lịch làm việc cho CTV (mỗi buổi một lịch)
            phanCongService.taoLichLamViec(pc, cacBuoi, start, end, ghiChu);

            // 3. Thông báo cho CTV: ThongBaoSuKienService gửi khi phân công được tạo

            return ResponseEntity.ok(Map.of("success", true, "message", "Phân công và tạo lịch làm việc thành công!"));
        } catch (Exception ex) {
            // Hoàn tác mọi thay đổi dở dang của giao dịch (nếu không, phần đã lưu trước khi lỗi vẫn được ghi)
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return ResponseEntity.badRequest().body(Map.of("success", false,
                    "message", ex.getMessage() != null ? ex.getMessage() : "Không lưu được phân công."));
        }
    }

    private LichLamViecDTO toLichLamViecDTO(LichLamViec l) {
        if (l == null) return null;
        LichLamViecDTO dto = LichLamViecDTO.builder()
                .id(l.getId())
                .maLichLamViec(l.getMaLichLamViec())
                .ngayLam(l.getNgayLam())
                .gioBatDau(l.getGioBatDau())
                .gioKetThuc(l.getGioKetThuc())
                .trangThai(l.getTrangThai())
                .ketQuaThucHien(l.getKetQuaThucHien())
                .build();

        if (l.getCongTacVien() != null) {
            dto.setCongTacVienId(l.getCongTacVien().getId());
            dto.setCtvTen(l.getCongTacVien().getHoTen());
            dto.setCtvDiemDanhGia(l.getCongTacVien().getDiemDanhGia());
            dto.setCtvNoiCuTru(l.getCongTacVien().getNoiCuTru());
            dto.setCtvCapDo(l.getCongTacVien().getCapDo());
        }

        if (l.getPhanCong() != null) {
            dto.setPhanCongId(l.getPhanCong().getId());
            DonDatDichVu dd = l.getPhanCong().getDonDat();
            if (dd != null) {
                dto.setDonDatId(dd.getId());
                dto.setMaDonDat(dd.getMaDonDat());
                if (dd.getChiTietList() != null && !dd.getChiTietList().isEmpty()) {
                    var firstCt = dd.getChiTietList().get(0);
                    if (firstCt.getDichVu() != null) {
                        dto.setDichVuTen(firstCt.getDichVu().getTenDichVu());
                    }
                }
                if (dd.getKhachHang() != null) {
                    dto.setKhachHangTen(dd.getKhachHang().getHoTen());
                    dto.setKhachHangSdt(dd.getKhachHang().getSoDienThoai());
                }
                if (dd.getDiaChi() != null) {
                    dto.setDiaChiChiTiet(dd.getDiaChi().getDiaChiChiTiet());
                }
            }
        }
        return dto;
    }

        // ── UC-CSKH06 – Quản lý thông báo CSKH ─────────────────────────
    @GetMapping("/thong-bao")
    public String thongBao(org.springframework.ui.Model model) {
        model.addAttribute("thongBaos", thongBaoService.danhSach());
        return "cskh/thong-bao";
    }

    private String getThuTiengViet(LocalDate date) {
        if (date == null) return "";
        switch (date.getDayOfWeek()) {
            case MONDAY: return "Thứ Hai";
            case TUESDAY: return "Thứ Ba";
            case WEDNESDAY: return "Thứ Tư";
            case THURSDAY: return "Thứ Năm";
            case FRIDAY: return "Thứ Sáu";
            case SATURDAY: return "Thứ Bảy";
            case SUNDAY: return "Chủ Nhật";
            default: return "";
        }
    }
}
