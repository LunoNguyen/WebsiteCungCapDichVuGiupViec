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
    private final CongTacVienRepository congTacVienRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final PhanCongCTVRepository phanCongCTVRepository;
    private final PhanHoiDanhGiaRepository phanHoiDanhGiaRepository;
    private final DichVuRepository dichVuRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final KhuVucRepository khuVucRepository;

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
            KhuVucRepository khuVucRepository) {
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
        model.addAttribute("recentOrders",    thongKeService.getDonHangGanDay(8));
        model.addAttribute("recentComplaints",thongKeService.getKhieuNaiGanDay(5));
        model.addAttribute("phanBoDon",       thongKeService.getPhanBoTrangThaiDon());
        return "cskh/dashboard";
    }

    // ── UC-CSKH01 – Quản lý khách hàng ──────────────────────────────
    @GetMapping("/khach-hang")
    public String khachHang(Model model) {
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
            @RequestParam(required = false) String tinhThanh,
            @RequestParam(required = false) String quanHuyen,
            @RequestParam(required = false) String phuongXa,
            @RequestParam(required = false) String maKhuVuc,
            @RequestParam(required = false) String soNhaTenDuong,
            @RequestParam(required = false) String diaChiChiTiet,
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

            // 1. Xác thực / tìm hoặc tạo mới bản ghi KhuVuc từ dữ liệu API provinces.open-api.vn
            KhuVuc kv = null;
            if (maKhuVuc != null && !maKhuVuc.isBlank()) {
                kv = khuVucRepository.findByMaKhuVuc(maKhuVuc.trim()).orElse(null);
            }

            String wardOrDistrict = (phuongXa != null && !phuongXa.isBlank())
                    ? phuongXa.trim()
                    : (quanHuyen != null ? quanHuyen.trim() : "");

            if (kv == null && !wardOrDistrict.isBlank() && tinhThanh != null && !tinhThanh.isBlank()) {
                kv = khuVucRepository.findFirstByQuanHuyenAndTinhThanh(wardOrDistrict, tinhThanh.trim())
                        .or(() -> khuVucRepository.findFirstByTenKhuVucAndTinhThanh(wardOrDistrict, tinhThanh.trim()))
                        .orElse(null);
            }

            if (kv == null && ((tinhThanh != null && !tinhThanh.isBlank()) || !wardOrDistrict.isBlank())) {
                String code = (maKhuVuc != null && !maKhuVuc.isBlank() && maKhuVuc.trim().length() <= 20)
                        ? maKhuVuc.trim()
                        : ("KV-" + (System.currentTimeMillis() % 1000000));
                int retry = 0;
                while (khuVucRepository.findByMaKhuVuc(code).isPresent() && retry < 20) {
                    code = "KV-" + ((System.currentTimeMillis() + retry + 1) % 1000000);
                    retry++;
                }
                String areaName = !wardOrDistrict.isBlank() ? wardOrDistrict : (tinhThanh != null ? tinhThanh.trim() : "Khu vực");
                kv = KhuVuc.builder()
                        .maKhuVuc(code)
                        .tenKhuVuc(areaName)
                        .quanHuyen(!wardOrDistrict.isBlank() ? wardOrDistrict : "Chưa xác định")
                        .tinhThanh(tinhThanh != null && !tinhThanh.isBlank() ? tinhThanh.trim() : "Chưa xác định")
                        .trangThai("HoatDong")
                        .build();
                kv = khuVucRepository.save(kv);
            }

            // 2. Tạo bản ghi KhachHang
            String maKhachHang = "KH-" + (System.currentTimeMillis() % 1000000);
            int retryKh = 0;
            while (khachHangRepository.findByMaKhachHang(maKhachHang).isPresent() && retryKh < 20) {
                maKhachHang = "KH-" + ((System.currentTimeMillis() + retryKh + 1) % 1000000);
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
            String fullAddress = (diaChiChiTiet != null && !diaChiChiTiet.isBlank()) ? diaChiChiTiet.trim() : "";
            if (fullAddress.isBlank()) {
                StringBuilder sb = new StringBuilder();
                if (soNhaTenDuong != null && !soNhaTenDuong.isBlank()) sb.append(soNhaTenDuong.trim());
                if (!wardOrDistrict.isBlank()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(wardOrDistrict);
                }
                if (tinhThanh != null && !tinhThanh.isBlank()) {
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(tinhThanh.trim());
                }
                fullAddress = sb.toString();
            }
            if (fullAddress.isBlank()) {
                fullAddress = "Chưa cập nhật";
            }

            String maDiaChi = "DC-" + (System.currentTimeMillis() % 1000000);
            int retryDc = 0;
            while (diaChiKhachHangRepository.findByMaDiaChi(maDiaChi).isPresent() && retryDc < 20) {
                maDiaChi = "DC-" + ((System.currentTimeMillis() + retryDc + 1) % 1000000);
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
    public String danhGia(Model model) {
        var danhGiaList = danhGiaRepository.findAll();
        var phanHoiList = phanHoiDanhGiaRepository.findAll();

        long lowRating = danhGiaList.stream()
                .filter(dg -> (dg.getDiemChatLuong() + dg.getDiemThaiDo()) / 2.0 < 3)
                .count();
        long replied = phanHoiList.stream()
                .map(ph -> ph.getDanhGia().getId())
                .distinct()
                .count();

        model.addAttribute("danhGias",       danhGiaList);
        model.addAttribute("phanHoiList",    phanHoiList);
        model.addAttribute("diemDanhGiaTB",  thongKeService.getDiemDanhGiaTrungBinh());
        model.addAttribute("lowRatingCount", lowRating);
        model.addAttribute("repliedCount",   replied);
        model.addAttribute("congTacViens",   congTacVienRepository.findAll());
        model.addAttribute("khachHangs",     khachHangRepository.findAll());
        return "cskh/danh-gia";
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
            CongTacVien ctv = congTacVienRepository.findById(ctvId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CTV"));
            DonDatDichVu dd = donDatDichVuRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Đơn đặt dịch vụ"));

            LocalDate date = LocalDate.parse(ngayLam);
            java.time.LocalTime start = java.time.LocalTime.parse(gioBatDau);
            java.time.LocalTime end = java.time.LocalTime.parse(gioKetThuc);

            // 1. Lưu Phân công CTV
            PhanCongCTV pc = PhanCongCTV.builder()
                    .maPhanCong("PC-" + (System.currentTimeMillis() % 1000000))
                    .donDat(dd)
                    .congTacVien(ctv)
                    .trangThai("DaXacNhan")
                    .thoiGianPhanCong(java.time.LocalDateTime.now())
                    .thoiGianXacNhan(java.time.LocalDateTime.now())
                    .build();
            phanCongCTVRepository.save(pc);

            // 2. Tạo Lịch làm việc cho CTV
            LichLamViec llv = LichLamViec.builder()
                    .maLichLamViec("LLV-" + (System.currentTimeMillis() % 1000000))
                    .phanCong(pc)
                    .congTacVien(ctv)
                    .ngayLam(date)
                    .gioBatDau(start)
                    .gioKetThuc(end)
                    .trangThai("SapToi")
                    .ketQuaThucHien(ghiChu)
                    .build();
            lichLamViecRepository.save(llv);

            // 3. Cập nhật trạng thái đơn
            dd.setTrangThai("DaXacNhan");
            donDatDichVuRepository.save(dd);

            return ResponseEntity.ok(Map.of("success", true, "message", "Phân công và tạo lịch làm việc thành công!"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", ex.getMessage()));
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
    public String thongBao(Model model) {
        model.addAttribute("thongBaos",      thongBaoRepository.findAll());
        model.addAttribute("donHangStats",   thongKeService.getDonHangStats());
        model.addAttribute("khieuNaiStats",  thongKeService.getKhieuNaiStats());
        return "cskh/thong-bao";
    }
}
