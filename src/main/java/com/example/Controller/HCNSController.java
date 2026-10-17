package com.example.Controller;

import com.example.DTO.*;
import com.example.Model.*;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HCNS-Kế toán module controllers - UC-HCNS01 to UC-HCNS05
 * Xử lý đầy đủ nghiệp vụ CRUD kết nối trực tiếp CSDL MySQL.
 */
@Controller
@RequestMapping("/hcns")
public class HCNSController {

    private final ThongKeService thongKeService;
    private final NhanVienRepository nhanVienRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final LoaiDichVuRepository loaiDichVuRepository;
    private final DichVuRepository dichVuRepository;
    private final BangGiaDichVuRepository bangGiaDichVuRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final PhongBanRepository phongBanRepository;
    private final ChucVuRepository chucVuRepository;
    private final KhachHangRepository khachHangRepository;
    private final NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository;
    private final com.example.Service.MinioService minioService;
    private final com.example.Service.CapTaiKhoanCtvService capTaiKhoanCtvService;

    public HCNSController(
            ThongKeService thongKeService,
            NhanVienRepository nhanVienRepository,
            CongTacVienRepository congTacVienRepository,
            LoaiDichVuRepository loaiDichVuRepository,
            DichVuRepository dichVuRepository,
            BangGiaDichVuRepository bangGiaDichVuRepository,
            TaiKhoanRepository taiKhoanRepository,
            PhongBanRepository phongBanRepository,
            ChucVuRepository chucVuRepository,
            KhachHangRepository khachHangRepository,
            NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository,
            com.example.Service.MinioService minioService,
            com.example.Service.CapTaiKhoanCtvService capTaiKhoanCtvService) {
        this.thongKeService = thongKeService;
        this.nhanVienRepository = nhanVienRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.loaiDichVuRepository = loaiDichVuRepository;
        this.dichVuRepository = dichVuRepository;
        this.bangGiaDichVuRepository = bangGiaDichVuRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.phongBanRepository = phongBanRepository;
        this.chucVuRepository = chucVuRepository;
        this.khachHangRepository = khachHangRepository;
        this.nhatKyTaiKhoanRepository = nhatKyTaiKhoanRepository;
        this.minioService = minioService;
        this.capTaiKhoanCtvService = capTaiKhoanCtvService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var ctvStats = thongKeService.getCongTacVienStats();
        var nvStats = thongKeService.getNhanVienStats();
        var ctvs = congTacVienRepository.findAll();
        var dvs = dichVuRepository.findAll();

        model.addAttribute("congTacVienStats", ctvStats);
        model.addAttribute("nhanVienStats", nvStats);
        model.addAttribute("tongNhanVien", nvStats.getTongNhanVien());
        model.addAttribute("tongCongTacVien", ctvStats.getTongCongTacVien());
        model.addAttribute("tongDichVu", thongKeService.getTongDichVu());
        model.addAttribute("ctvChoDuyetCount", ctvStats.getChoDuyetCount());
        model.addAttribute("ctvMoiCount", ctvStats.getMoiCount());
        model.addAttribute("ctvThuongCount", ctvStats.getThuongCount());
        model.addAttribute("ctvUuTuCount", ctvStats.getUuTuCount());
        model.addAttribute("nhanViens", nhanVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        model.addAttribute("congTacViens", congTacVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        model.addAttribute("dichVus", dichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return "hcns/dashboard";
    }

    // =========================================================================
    // 1. UC-HCNS01 – Quản lý nhân viên (CRUD + Phân trang 10 người/trang, sắp xếp tăng dần)
    // =========================================================================
    @GetMapping("/nhan-vien")
    public String nhanVien(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        if (page < 0) page = 0;
        int pageSize = 10; // ĐẶC TẢ: hiển thị đúng 10 người, muốn xem tiếp phải nhấn ->

        Page<NhanVien> pageNhanVien = nhanVienRepository.findAll(
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "id"))
        );

        model.addAttribute("nhanVienStats", thongKeService.getNhanVienStats());
        model.addAttribute("nhanViens", pageNhanVien.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageNhanVien.getTotalPages());
        model.addAttribute("totalItems", pageNhanVien.getTotalElements());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("chucVus", chucVuRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        model.addAttribute("phongBans", phongBanRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return "hcns/nhan-vien";
    }

    @PostMapping("/nhan-vien/them")
    public String themNhanVien(
            @RequestParam String hoTen,
            @RequestParam String email,
            @RequestParam String soDienThoai,
            @RequestParam(required = false) String ngaySinh,
            @RequestParam(required = false, defaultValue = "Nam") String gioiTinh,
            @RequestParam(required = false) String diaChi,
            @RequestParam Integer chucVuId,
            @RequestParam(required = false) String ngayVaoLam,
            RedirectAttributes redirectAttributes) {

        try {
            if (hoTen == null || hoTen.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Họ tên nhân viên không được để trống!");
                return "redirect:/hcns/nhan-vien";
            }
            if (soDienThoai == null || !soDienThoai.trim().matches("\\d{9,11}")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại không hợp lệ (cần 9-11 chữ số)!");
                return "redirect:/hcns/nhan-vien";
            }

            var chucVuOpt = chucVuRepository.findById(chucVuId);
            if (chucVuOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chức vụ được chọn không tồn tại!");
                return "redirect:/hcns/nhan-vien";
            }

            // Kiểm tra trùng Email trong hệ thống
            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email nhân viên không được để trống!");
                return "redirect:/hcns/nhan-vien";
            }
            String cleanEmail = email.trim();
            if (nhanVienRepository.findByEmailIgnoreCase(cleanEmail).isPresent()
                    || taiKhoanRepository.findByEmail(cleanEmail).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + cleanEmail + "' đã tồn tại trên hệ thống, vui lòng chọn email khác!");
                return "redirect:/hcns/nhan-vien";
            }

            // Ràng buộc tuổi phải từ đủ 18 trở lên
            if (ngaySinh == null || ngaySinh.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh của nhân viên không được để trống!");
                return "redirect:/hcns/nhan-vien";
            }
            LocalDate dob = parseNgaySinh(ngaySinh);
            if (dob == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh không đúng định dạng (VD: 15/05/2000 hoặc 2000-05-15)!");
                return "redirect:/hcns/nhan-vien";
            }
            if (dob.plusYears(18).isAfter(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Nhân viên phải từ đủ 18 tuổi trở lên!");
                return "redirect:/hcns/nhan-vien";
            }
            if (dob.isBefore(LocalDate.now().minusYears(100))) {
                redirectAttributes.addFlashAttribute("errorMessage", "Năm sinh không hợp lệ!");
                return "redirect:/hcns/nhan-vien";
            }

            NhanVien nv = new NhanVien();
            int soThuTu = nhanVienRepository.findAll().stream()
                    .map(NhanVien::getMaNhanVien)
                    .filter(ma -> ma != null && ma.startsWith("NV-") && ma.substring(3).matches("\\d+"))
                    .mapToInt(ma -> Integer.parseInt(ma.substring(3)))
                    .max()
                    .orElse(0) + 1;

            nv.setMaNhanVien(String.format("NV-%03d", soThuTu));
            nv.setHoTen(hoTen.trim());
            nv.setEmail(email != null ? email.trim() : null);
            nv.setSoDienThoai(soDienThoai.trim());
            nv.setGioiTinh(gioiTinh);
            nv.setDiaChi(diaChi != null ? diaChi.trim() : null);
            nv.setChucVu(chucVuOpt.get());
            nv.setNgaySinh(dob);

            if (ngayVaoLam != null && !ngayVaoLam.trim().isEmpty()) {
                LocalDate nvl = parseNgaySinh(ngayVaoLam);
                if (nvl != null && nvl.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Ngày vào làm không được ở quá khứ!");
                }
                nv.setNgayVaoLam(nvl != null ? nvl : LocalDate.now());
            } else {
                nv.setNgayVaoLam(LocalDate.now());
            }
            nv.setTrangThai("DangLamViec");

            nhanVienRepository.save(nv);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm nhân viên mới thành công (Mã: " + nv.getMaNhanVien() + ")!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi thêm nhân viên: " + e.getMessage());
        }
        return "redirect:/hcns/nhan-vien";
    }

    @PostMapping("/nhan-vien/sua")
    public String suaNhanVien(
            @RequestParam Integer id,
            @RequestParam String hoTen,
            @RequestParam String email,
            @RequestParam String soDienThoai,
            @RequestParam(required = false) String ngaySinh,
            @RequestParam(required = false) String diaChi,
            @RequestParam Integer chucVuId,
            @RequestParam(required = false, defaultValue = "DangLamViec") String trangThai,
            RedirectAttributes redirectAttributes) {

        try {
            var nvOpt = nhanVienRepository.findById(id);
            if (nvOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy thông tin nhân viên!");
                return "redirect:/hcns/nhan-vien";
            }

            var chucVuOpt = chucVuRepository.findById(chucVuId);
            if (chucVuOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chức vụ không hợp lệ!");
                return "redirect:/hcns/nhan-vien";
            }

            NhanVien nv = nvOpt.get();

            // Nếu có cập nhật ngày sinh thì kiểm tra tuổi >= 18
            if (ngaySinh != null && !ngaySinh.trim().isEmpty()) {
                LocalDate dob = parseNgaySinh(ngaySinh);
                if (dob == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh không đúng định dạng (VD: 15/05/2000 hoặc 2000-05-15)!");
                    return "redirect:/hcns/nhan-vien";
                }
                if (dob.plusYears(18).isAfter(LocalDate.now())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Nhân viên phải từ đủ 18 tuổi trở lên!");
                    return "redirect:/hcns/nhan-vien";
                }
                if (dob.isBefore(LocalDate.now().minusYears(100))) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Năm sinh không hợp lệ!");
                    return "redirect:/hcns/nhan-vien";
                }
                nv.setNgaySinh(dob);
            }

            // Kiểm tra trùng Email khi sửa nhân viên
            if (email == null || email.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email nhân viên không được để trống!");
                return "redirect:/hcns/nhan-vien";
            }
            String cleanEmail = email.trim();
            boolean emailDuplicate = nhanVienRepository.findAll().stream()
                    .anyMatch(n -> !n.getId().equals(id) && cleanEmail.equalsIgnoreCase(n.getEmail()));
            if (emailDuplicate) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + cleanEmail + "' đã được sử dụng bởi nhân viên khác!");
                return "redirect:/hcns/nhan-vien";
            }
            var tkWithEmail = taiKhoanRepository.findByEmail(cleanEmail);
            if (tkWithEmail.isPresent() && (nv.getTaiKhoan() == null || !tkWithEmail.get().getId().equals(nv.getTaiKhoan().getId()))) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + cleanEmail + "' đã được sử dụng bởi tài khoản khác!");
                return "redirect:/hcns/nhan-vien";
            }

            nv.setHoTen(hoTen.trim());
            nv.setEmail(cleanEmail);
            nv.setSoDienThoai(soDienThoai.trim());
            nv.setDiaChi(diaChi != null ? diaChi.trim() : null);
            nv.setChucVu(chucVuOpt.get());
            nv.setTrangThai(trangThai);

            nhanVienRepository.save(nv);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ nhân viên thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
        }
        return "redirect:/hcns/nhan-vien";
    }

    @PostMapping("/nhan-vien/doi-trang-thai")
    public String doiTrangThaiNhanVien(
            @RequestParam Integer id,
            @RequestParam String trangThai,
            RedirectAttributes redirectAttributes) {
        try {
            nhanVienRepository.findById(id).ifPresent(nv -> {
                nv.setTrangThai(trangThai);
                nhanVienRepository.save(nv);
            });
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật trạng thái nhân viên!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/nhan-vien";
    }

    @PostMapping("/nhan-vien/xoa")
    public String xoaNhanVien(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            var nvOpt = nhanVienRepository.findById(id);
            if (nvOpt.isPresent()) {
                NhanVien nv = nvOpt.get();
                try {
                    nhanVienRepository.delete(nv);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã xóa nhân viên '" + nv.getHoTen() + "' thành công!");
                } catch (Exception ex) {
                    nv.setTrangThai("DaNghi");
                    nhanVienRepository.save(nv);
                    redirectAttributes.addFlashAttribute("successMessage", "Nhân viên '" + nv.getHoTen() + "' đã có dữ liệu liên kết nên được chuyển sang trạng thái 'Đã nghỉ việc'!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa nhân viên: " + e.getMessage());
        }
        return "redirect:/hcns/nhan-vien";
    }

    // =========================================================================
    // 2. UC-HCNS02 – Quản lý Cộng tác viên (CRUD)
    // =========================================================================
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.UngVienCtvService ungVienCtvService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.BangGiaService bangGiaService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.KhuVucService khuVucService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.KhuVucCtvService khuVucCtvService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.DiaGioiService diaGioiService;

    /**
     * Địa chỉ nơi ở hiện tại của CTV: tỉnh, phường (provinces.open-api.vn v2) + số nhà, ghép thành chuỗi đầy đủ.
     * Form cũ chỉ gửi chuỗi noiCuTru thì giữ nguyên chuỗi đó.
     */
    private String noiOCtv(String noiCuTru, String soNhaTenDuong, Integer maTinh, Integer maPhuong) {
        String kq = maPhuong != null
                ? diaGioiService.xacDinh(soNhaTenDuong, maTinh, maPhuong, null).diaChiDayDu()
                : (noiCuTru != null ? noiCuTru.trim() : "");
        if (kq.isBlank()) throw new IllegalArgumentException("Vui lòng nhập địa chỉ nơi ở hiện tại của cộng tác viên!");
        if (kq.length() > 200) throw new IllegalArgumentException("Địa chỉ nơi ở quá dài (tối đa 200 ký tự)!");
        return kq;
    }

    /** Địa chỉ phỏng vấn điền sẵn (application.properties: app.phong-van.dia-chi). */
    @org.springframework.beans.factory.annotation.Value("${app.phong-van.dia-chi:}")
    private String diaChiPhongVanMacDinh;

    @GetMapping("/cong-tac-vien")
    public String congTacVien(Model model) {
        List<CongTacVien> ds = congTacVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        var daXem = ungVienCtvService.hoSoDaXem(ds);
        var lichPhongVan = ungVienCtvService.lichPhongVan(ds);

        // Hồ sơ chờ duyệt: đơn mới (chưa xem) lên đầu, rồi đến đơn gửi gần đây nhất
        List<CongTacVien> choDuyet = ds.stream()
                .filter(c -> "ChoDuyet".equals(c.getTrangThai()))
                .sorted(java.util.Comparator.comparing((CongTacVien c) -> daXem.contains(c.getId()))
                        .thenComparing(CongTacVien::getId, java.util.Comparator.reverseOrder()))
                .toList();

        model.addAttribute("congTacVienStats", thongKeService.getCongTacVienStats());
        model.addAttribute("congTacViens", ds);
        model.addAttribute("hoSoChoDuyet", choDuyet);
        model.addAttribute("daXemIds", daXem);
        model.addAttribute("lichPhongVan", lichPhongVan);
        model.addAttribute("soHoSoMoi", choDuyet.stream().filter(c -> !daXem.contains(c.getId())).count());
        model.addAttribute("diaChiPhongVanMacDinh", diaChiPhongVanMacDinh);
        // Quận, huyện: chọn khu vực nhận việc khi thêm / sửa CTV, hiện trong bảng danh sách
        model.addAttribute("khuVucs", khuVucService.danhSach());
        model.addAttribute("khuVucCtv", khuVucCtvService.theoCtv());
        // Tách nơi ở (số nhà / phường / tỉnh) để hộp sửa điền sẵn
        java.util.Map<Integer, java.util.Map<String, Object>> noiOTach = new java.util.HashMap<>();
        for (CongTacVien c : ds) noiOTach.put(c.getId(), diaGioiService.tach(c.getNoiCuTru()));
        model.addAttribute("noiOTach", noiOTach);
        return "hcns/cong-tac-vien";
    }

    /** Chi tiết đơn ứng tuyển (JSON cho cửa sổ xem chi tiết). Mở xem thì đánh dấu đơn đã xem. */
    @GetMapping(value = "/cong-tac-vien/{id}/chi-tiet", produces = "application/json")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<Map<String, Object>> chiTietUngVien(
            @org.springframework.web.bind.annotation.PathVariable Integer id, HttpServletRequest request) {
        var ctv = congTacVienRepository.findById(id).orElse(null);
        if (ctv == null) {
            return org.springframework.http.ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy hồ sơ."));
        }
        ungVienCtvService.danhDauDaXem(ctv, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return org.springframework.http.ResponseEntity.ok(ungVienCtvService.chiTiet(id));
    }

    /** Bước trước khi duyệt: gửi thời gian, địa chỉ phỏng vấn cho ứng viên (thông báo + SMS). */
    @PostMapping("/cong-tac-vien/{id}/phong-van")
    public String guiLichPhongVan(@org.springframework.web.bind.annotation.PathVariable Integer id,
                                  @RequestParam String thoiGian,
                                  @RequestParam String diaDiem,
                                  @RequestParam(required = false) String ghiChu,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime luc = LocalDateTime.parse(thoiGian.trim()); // input datetime-local: yyyy-MM-ddTHH:mm
            Object ten = request.getSession().getAttribute("authenticatedFullName");
            String sms = ungVienCtvService.guiLichPhongVan(id, luc, diaDiem, ghiChu,
                    ten != null ? "HCNS - " + ten : "HCNS", request.getRemoteAddr(), request.getHeader("User-Agent"));
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi lịch phỏng vấn cho ứng viên.");
            redirectAttributes.addFlashAttribute("tinNhanDaGui", sms);
        } catch (java.time.format.DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thời gian phỏng vấn không hợp lệ.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi lịch phỏng vấn: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/them")
    public String themCongTacVien(
            @RequestParam String hoTen,
            @RequestParam String soDienThoai,
            @RequestParam(required = false) String noiCuTru,
            @RequestParam(required = false) String soNhaTenDuong,
            @RequestParam(required = false) Integer maTinh,
            @RequestParam(required = false) Integer maPhuong,
            @RequestParam(required = false) String ngaySinh,
            @RequestParam(required = false, defaultValue = "Nam") String gioiTinh,
            @RequestParam(required = false, defaultValue = "Moi") String capDo,
            @RequestParam(required = false, defaultValue = "HoatDong") String trangThai,
            RedirectAttributes redirectAttributes) {
        try {
            if (hoTen == null || hoTen.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Họ tên CTV không được để trống!");
                return "redirect:/hcns/cong-tac-vien";
            }
            if (soDienThoai == null || !soDienThoai.trim().matches("\\d{9,11}")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại CTV không hợp lệ!");
                return "redirect:/hcns/cong-tac-vien";
            }

            // Ràng buộc tuổi phải từ đủ 18 trở lên
            if (ngaySinh == null || ngaySinh.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh của Cộng tác viên không được để trống!");
                return "redirect:/hcns/cong-tac-vien";
            }
            LocalDate dob = parseNgaySinh(ngaySinh);
            if (dob == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh không đúng định dạng (VD: 15/05/2000 hoặc 2000-05-15)!");
                return "redirect:/hcns/cong-tac-vien";
            }
            if (dob.plusYears(18).isAfter(LocalDate.now())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Cộng tác viên phải từ đủ 18 tuổi trở lên!");
                return "redirect:/hcns/cong-tac-vien";
            }
            if (dob.isBefore(LocalDate.now().minusYears(100))) {
                redirectAttributes.addFlashAttribute("errorMessage", "Năm sinh không hợp lệ!");
                return "redirect:/hcns/cong-tac-vien";
            }

            CongTacVien ctv = new CongTacVien();
            int soThuTu = congTacVienRepository.findAll().stream()
            .map(CongTacVien::getMaCongTacVien)
            .filter(ma -> ma != null && ma.startsWith("CTV-") && ma.substring(4).matches("\\d+"))
            .mapToInt(ma -> Integer.parseInt(ma.substring(4)))
            .max()
            .orElse(0) + 1;

            ctv.setMaCongTacVien(String.format("CTV-%03d", soThuTu));
            ctv.setHoTen(hoTen.trim());
            ctv.setSoDienThoai(soDienThoai.trim());
            ctv.setNoiCuTru(noiOCtv(noiCuTru, soNhaTenDuong, maTinh, maPhuong));
            ctv.setGioiTinh(gioiTinh);
            ctv.setNgaySinh(dob);
            ctv.setCapDo(capDo);
            ctv.setTrangThai(trangThai);
            ctv.setDiemDanhGia(BigDecimal.valueOf(5.0));
            ctv.setNgayDangKy(LocalDate.now());

            congTacVienRepository.save(ctv);
            // Khu vực nhận việc ban đầu: quận của nơi ở; sau đó tự đổi theo GPS khi CTV dùng app
            if (maPhuong != null) {
                List<Integer> kvNoiO = diaGioiService.khuVucCuaPhuong(maPhuong).stream()
                        .map(com.example.Model.KhuVuc::getId).limit(1).toList();
                if (!kvNoiO.isEmpty()) khuVucCtvService.capNhat(ctv.getId(), kvNoiO);
            }
            redirectAttributes.addFlashAttribute("successMessage", "Thêm Cộng tác viên thành công (Mã: " + ctv.getMaCongTacVien() + ")!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm CTV: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/sua")
    public String suaCongTacVien(
            @RequestParam Integer id,
            @RequestParam String hoTen,
            @RequestParam String soDienThoai,
            @RequestParam(required = false) String noiCuTru,
            @RequestParam(required = false) String soNhaTenDuong,
            @RequestParam(required = false) Integer maTinh,
            @RequestParam(required = false) Integer maPhuong,
            @RequestParam(required = false) String ngaySinh,
            @RequestParam String capDo,
            @RequestParam String trangThai,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            var ctvOpt = congTacVienRepository.findById(id);
            if (ctvOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy cộng tác viên!");
                return "redirect:/hcns/cong-tac-vien";
            }

            CongTacVien ctv = ctvOpt.get();

            // Nếu có cập nhật ngày sinh thì kiểm tra tuổi >= 18
            if (ngaySinh != null && !ngaySinh.trim().isEmpty()) {
                LocalDate dob = parseNgaySinh(ngaySinh);
                if (dob == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Ngày sinh không đúng định dạng (VD: 15/05/2000 hoặc 2000-05-15)!");
                    return "redirect:/hcns/cong-tac-vien";
                }
                if (dob.plusYears(18).isAfter(LocalDate.now())) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Cộng tác viên phải từ đủ 18 tuổi trở lên!");
                    return "redirect:/hcns/cong-tac-vien";
                }
                if (dob.isBefore(LocalDate.now().minusYears(100))) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Năm sinh không hợp lệ!");
                    return "redirect:/hcns/cong-tac-vien";
                }
                ctv.setNgaySinh(dob);
            }

            ctv.setHoTen(hoTen.trim());
            ctv.setSoDienThoai(soDienThoai.trim());
            ctv.setNoiCuTru(noiOCtv(noiCuTru, soNhaTenDuong, maTinh, maPhuong));
            ctv.setCapDo(capDo);

            // Chờ duyệt -> Hoạt động = duyệt hồ sơ: kích hoạt tài khoản và gửi tin nhắn như nút "Duyệt"
            if ("ChoDuyet".equals(ctv.getTrangThai()) && "HoatDong".equals(trangThai)) {
                congTacVienRepository.save(ctv);
                var kq = capTaiKhoanCtvService.duyetVaCapTaiKhoan(id, null,
                        request.getRemoteAddr(), request.getHeader("User-Agent"));
                baoKetQuaDuyet(kq, redirectAttributes);
                return "redirect:/hcns/cong-tac-vien";
            }

            ctv.setTrangThai(trangThai);
            congTacVienRepository.save(ctv);

            // Trạng thái tài khoản đăng nhập đi theo trạng thái hồ sơ CTV
            TaiKhoan tk = ctv.getTaiKhoan();
            // Tài khoản dùng chung với vai trò khách hàng: chỉ mở (không khóa) tài khoản theo hồ sơ CTV
            boolean dungChung = tk != null && khachHangRepository.findByTaiKhoan(tk).isPresent();
            if (tk != null && (!dungChung || "HoatDong".equals(trangThai))) {
                String trangThaiTk = switch (trangThai) {
                    case "HoatDong" -> "HoatDong";
                    case "DinhChi" -> "BiKhoa";
                    default -> "ChoDuyet"; // ChoDuyet | TuChoi
                };
                if (!trangThaiTk.equals(tk.getTrangThai())) {
                    tk.setTrangThai(trangThaiTk);
                    tk.setNgayCapNhat(LocalDateTime.now());
                    taiKhoanRepository.save(tk);
                }
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ CTV thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật CTV: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    private void baoKetQuaDuyet(com.example.Service.CapTaiKhoanCtvService.KetQua kq, RedirectAttributes redirectAttributes) {
        if (kq.daGuiTin()) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã duyệt hồ sơ " + kq.hoTen() + ", kích hoạt tài khoản " + kq.tenDangNhap()
                            + " và gửi tin nhắn tới " + kq.soDienThoai() + ".");
            redirectAttributes.addFlashAttribute("tinNhanDaGui", kq.noiDungTin());
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã duyệt hồ sơ " + kq.hoTen() + " và kích hoạt tài khoản " + kq.tenDangNhap() + ".");
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Không gửi được tin nhắn tới " + kq.soDienThoai() + " (" + kq.maTin().replaceFirst("^loi: ", "")
                            + "). Vui lòng báo ứng viên nội dung: " + kq.noiDungTin());
        }
    }

    /**
     * Duyệt hồ sơ ứng viên: kích hoạt hồ sơ CTV và tài khoản đăng nhập (giữ mật khẩu ứng viên tự đặt),
     * gửi tin nhắn báo kết quả. HCNS có thể nhập mật khẩu mới nếu muốn đặt lại.
     */
    @PostMapping("/cong-tac-vien/duyet")
    public String duyetCongTacVien(@RequestParam Integer id,
                                   @RequestParam(required = false) String matKhau,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            var kq = capTaiKhoanCtvService.duyetVaCapTaiKhoan(id, matKhau,
                    request.getRemoteAddr(), request.getHeader("User-Agent"));
            baoKetQuaDuyet(kq, redirectAttributes);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/tu-choi")
    public String tuChoiCongTacVien(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            congTacVienRepository.findById(id).ifPresent(ctv -> {
                ctv.setTrangThai("TuChoi");
                congTacVienRepository.save(ctv);
            });
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối hồ sơ cộng tác viên!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/doi-trang-thai")
    public String doiTrangThaiCongTacVien(
            @RequestParam Integer id,
            @RequestParam String trangThai,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            congTacVienRepository.findById(id).ifPresent(ctv -> {
                ctv.setTrangThai(trangThai);
                TaiKhoan tk = ctv.getTaiKhoan();
                if (tk == null && "HoatDong".equalsIgnoreCase(trangThai)) {
                    // Nếu CTV chưa có tài khoản, tự tạo và kích hoạt tài khoản
                    String suffix = com.example.Service.MaSinh.duoi();
                    tk = new TaiKhoan();
                    tk.setMaTaiKhoan("TK-CTV-" + suffix);
                    tk.setTenDangNhap(ctv.getSoDienThoai());
                    tk.setEmail(ctv.getSoDienThoai() + "@ctv.local");
                    tk.setSoDienThoai(ctv.getSoDienThoai());
                    tk.setLoaiTaiKhoan("CongTacVien");
                    tk.setMatKhau(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
                    tk.setTrangThai("HoatDong");
                    tk = taiKhoanRepository.save(tk);
                    ctv.setTaiKhoan(tk);
                } else if (tk != null) {
                    // Tài khoản dùng chung với vai trò khách hàng: chỉ đổi hồ sơ CTV, không khóa tài khoản
                    boolean dungChung = khachHangRepository.findByTaiKhoan(tk).isPresent();
                    if ("HoatDong".equalsIgnoreCase(trangThai)) {
                        tk.setTrangThai("HoatDong");
                    } else if (dungChung) {
                        // giữ nguyên trạng thái tài khoản
                    } else if ("DinhChi".equalsIgnoreCase(trangThai) || "BiKhoa".equalsIgnoreCase(trangThai) || "Khoa".equalsIgnoreCase(trangThai)) {
                        tk.setTrangThai("BiKhoa");
                    } else if ("ChoDuyet".equalsIgnoreCase(trangThai)) {
                        tk.setTrangThai("ChoDuyet");
                    }
                    tk.setNgayCapNhat(LocalDateTime.now());
                    taiKhoanRepository.save(tk);

                    try {
                        NhatKyTaiKhoan log = new NhatKyTaiKhoan();
                        log.setTaiKhoan(tk);
                        log.setHanhDong("HCNS đổi trạng thái CTV thành: " + trangThai);
                        log.setDiaChiIP(request.getRemoteAddr());
                        log.setThietBi(request.getHeader("User-Agent"));
                        log.setKetQua("ThanhCong");
                        nhatKyTaiKhoanRepository.save(log);
                    } catch (Exception ignored) {}
                }
                congTacVienRepository.save(ctv);
            });
            redirectAttributes.addFlashAttribute("successMessage", "Đã thay đổi trạng thái cộng tác viên!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/xoa")
    public String xoaCongTacVien(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            var ctvOpt = congTacVienRepository.findById(id);
            if (ctvOpt.isPresent()) {
                CongTacVien ctv = ctvOpt.get();
                try {
                    congTacVienRepository.delete(ctv);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã xóa cộng tác viên '" + ctv.getHoTen() + "' thành công!");
                } catch (Exception ex) {
                    ctv.setTrangThai("DinhChi");
                    congTacVienRepository.save(ctv);
                    redirectAttributes.addFlashAttribute("successMessage", "Cộng tác viên '" + ctv.getHoTen() + "' đã có lịch sử công việc nên được chuyển sang trạng thái 'Đình chỉ'!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa CTV: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    // =========================================================================
    // 3. UC-HCNS03 – Quản lý Danh mục Dịch vụ (CRUD)
    // =========================================================================
    @GetMapping("/danh-muc-dich-vu")
    public String danhMucDichVu(Model model) {
        model.addAttribute("loaiDichVus", loaiDichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "thuTuHienThi").and(Sort.by(Sort.Direction.ASC, "id"))));
        model.addAttribute("dichVus", dichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        // id danh mục -> đường dẫn ảnh (/files/<object key>) lấy từ MinIO; danh mục chưa có ảnh thì không có trong map
        java.util.Map<Integer, String> anhDanhMuc = new java.util.HashMap<>();
        for (LoaiDichVu ldv : loaiDichVuRepository.findAll()) {
            String url = minioService.getViewUrl(ldv.getHinhAnh());
            if (url != null) {
                anhDanhMuc.put(ldv.getId(), url);
            }
        }
        model.addAttribute("anhDanhMuc", anhDanhMuc);

        // Ảnh đại diện theo mã dịch vụ: mỗi mã một thẻ, ảnh dùng chung cho mọi biến thể cùng mã
        Map<String, Map<String, Object>> nhomAnh = new java.util.LinkedHashMap<>();
        for (DichVu dv : dichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "maDichVu").and(Sort.by(Sort.Direction.ASC, "id")))) {
            Map<String, Object> g = nhomAnh.computeIfAbsent(dv.getMaDichVu(), ma -> {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("maDichVu", ma);
                m.put("tenNhom", dv.getTenDichVu() != null ? dv.getTenDichVu().replaceFirst("\\s*\\(.*\\)\\s*$", "") : ma);
                m.put("loaiDichVuId", dv.getLoaiDichVu() != null ? dv.getLoaiDichVu().getId() : null);
                m.put("tenLoaiDichVu", dv.getLoaiDichVu() != null ? dv.getLoaiDichVu().getTenLoaiDichVu() : "");
                m.put("soBienThe", 0);
                return m;
            });
            g.put("soBienThe", (Integer) g.get("soBienThe") + 1);
            if (g.get("anhUrl") == null) {
                String url = minioService.getViewUrl(dv.getHinhAnh());
                if (url != null) g.put("anhUrl", url);
            }
        }
        for (Map<String, Object> g : nhomAnh.values()) {
            g.put("coAnhRieng", g.get("anhUrl") != null);
            if (g.get("anhUrl") == null && g.get("loaiDichVuId") != null) {
                g.put("anhUrl", anhDanhMuc.get((Integer) g.get("loaiDichVuId"))); // đang dùng ảnh danh mục
            }
        }
        model.addAttribute("nhomAnhDichVu", new java.util.ArrayList<>(nhomAnh.values()));
        return "hcns/danh-muc-dich-vu";
    }

    /**
     * Đổi ảnh đại diện của một dịch vụ theo mã: tải ảnh lên MinIO một lần rồi gán cho mọi biến thể cùng mã
     * (VD DV101: 2 giờ, 3 giờ, 4 giờ, gói tháng). datMacDinh = true thì bỏ ảnh riêng, dùng ảnh danh mục.
     * Ảnh cũ trên MinIO bị xoá khi không còn dịch vụ nào dùng.
     */
    @PostMapping("/danh-muc-dich-vu/anh-dich-vu")
    public String doiAnhDichVu(
            @RequestParam String maDichVu,
            @RequestParam(value = "hinhAnhFile", required = false) org.springframework.web.multipart.MultipartFile hinhAnhFile,
            @RequestParam(defaultValue = "false") boolean datMacDinh,
            @RequestParam(defaultValue = "") String quayVe,
            RedirectAttributes redirectAttributes) {
        // Chỉ quay về một trong hai trang HCNS đã biết (không nhận đường dẫn tuỳ ý)
        String veTrang = "bang-gia".equals(quayVe)
                ? "redirect:/hcns/dich-vu-bang-gia"
                : "redirect:/hcns/danh-muc-dich-vu#anh-dich-vu";
        List<DichVu> bienThe = dichVuRepository.findByMaDichVu(maDichVu);
        if (bienThe.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dịch vụ mã " + maDichVu + ".");
            return veTrang;
        }
        boolean coFile = hinhAnhFile != null && !hinhAnhFile.isEmpty();
        if (!datMacDinh && !coFile) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn ảnh mới cho dịch vụ " + maDichVu + ".");
            return veTrang;
        }
        String keyMoi = null;
        if (!datMacDinh) {
            try {
                keyMoi = minioService.uploadImage(hinhAnhFile, com.example.Service.MinioService.Folder.DICH_VU);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chưa tải được ảnh: " + e.getMessage());
                return veTrang;
            }
        }
        java.util.Set<String> anhCu = new java.util.HashSet<>();
        for (DichVu dv : bienThe) {
            if (dv.getHinhAnh() != null && !dv.getHinhAnh().isBlank()) anhCu.add(dv.getHinhAnh());
            dv.setHinhAnh(keyMoi);
        }
        dichVuRepository.saveAll(bienThe);

        // Xoá ảnh cũ trên MinIO (bỏ qua ảnh tĩnh /images/... và ảnh ngoài) nếu không còn dịch vụ nào dùng
        java.util.Set<String> conDung = new java.util.HashSet<>();
        dichVuRepository.findAll().forEach(dv -> { if (dv.getHinhAnh() != null) conDung.add(dv.getHinhAnh()); });
        for (String key : anhCu) {
            if (key.equals(keyMoi) || conDung.contains(key) || key.startsWith("/") || key.startsWith("http")) continue;
            try {
                minioService.deleteFile(key);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(HCNSController.class)
                        .warn("Không xoá được ảnh cũ {} của dịch vụ {}: {}", key, maDichVu, e.getMessage());
            }
        }
        redirectAttributes.addFlashAttribute("successMessage", datMacDinh
                ? "Dịch vụ " + maDichVu + " đã chuyển về dùng ảnh của danh mục."
                : "Đã cập nhật ảnh đại diện cho " + bienThe.size() + " dịch vụ mã " + maDichVu + ".");
        return veTrang;
    }

        /**
     * Tải ảnh danh mục lên MinIO (thư mục công khai) và gán object key vào cột HinhAnh.
     * Ảnh cũ trên MinIO được xóa sau khi ảnh mới tải lên thành công.
     *
     * @return null nếu thành công hoặc không chọn ảnh; ngược lại là thông báo lỗi
     */
    private String luuAnhDanhMuc(LoaiDichVu ldv, org.springframework.web.multipart.MultipartFile hinhAnhFile) {
        if (hinhAnhFile == null || hinhAnhFile.isEmpty()) {
            return null;
        }
        try {
            String anhCu = ldv.getHinhAnh();
            String key = minioService.uploadImage(hinhAnhFile, com.example.Service.MinioService.Folder.LOAI_DICH_VU);
            ldv.setHinhAnh(key);
            if (anhCu != null && !anhCu.isBlank() && !anhCu.startsWith("/") && !anhCu.startsWith("http")) {
                minioService.deleteFile(anhCu);
            }
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    @PostMapping("/danh-muc-dich-vu/them")
    public String themDanhMuc(
            @RequestParam String tenLoaiDichVu,
            @RequestParam(required = false) String moTa,
            @RequestParam(required = false, defaultValue = "1") Integer thuTuHienThi,
            @RequestParam(required = false, defaultValue = "HienThi") String trangThai,
            @RequestParam(value = "hinhAnhFile", required = false) org.springframework.web.multipart.MultipartFile hinhAnhFile,
            RedirectAttributes redirectAttributes) {
        try {
            if (tenLoaiDichVu == null || tenLoaiDichVu.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên loại danh mục không được để trống!");
                return "redirect:/hcns/danh-muc-dich-vu";
            }

            LoaiDichVu ldv = new LoaiDichVu();
            ldv.setMaLoaiDichVu(com.example.Service.MaSinh.tao("LDV-"));
            ldv.setTenLoaiDichVu(tenLoaiDichVu.trim());
            ldv.setMoTa(moTa != null ? moTa.trim() : "");
            ldv.setThuTuHienThi(thuTuHienThi != null ? thuTuHienThi : 1);
            ldv.setTrangThai(trangThai);

            String loiAnh = luuAnhDanhMuc(ldv, hinhAnhFile);
            loaiDichVuRepository.save(ldv);
            if (loiAnh != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đã thêm danh mục nhưng chưa lưu được ảnh: " + loiAnh);
                return "redirect:/hcns/danh-muc-dich-vu";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/danh-muc-dich-vu";
    }

    @PostMapping("/danh-muc-dich-vu/sua")
    public String suaDanhMuc(
            @RequestParam Integer id,
            @RequestParam String tenLoaiDichVu,
            @RequestParam(required = false) String moTa,
            @RequestParam(required = false, defaultValue = "1") Integer thuTuHienThi,
            @RequestParam(required = false, defaultValue = "HienThi") String trangThai,
            @RequestParam(value = "hinhAnhFile", required = false) org.springframework.web.multipart.MultipartFile hinhAnhFile,
            RedirectAttributes redirectAttributes) {
        try {
            var ldvOpt = loaiDichVuRepository.findById(id);
            if (ldvOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Danh mục không tồn tại!");
                return "redirect:/hcns/danh-muc-dich-vu";
            }

            LoaiDichVu ldv = ldvOpt.get();
            ldv.setTenLoaiDichVu(tenLoaiDichVu.trim());
            ldv.setMoTa(moTa != null ? moTa.trim() : "");
            ldv.setThuTuHienThi(thuTuHienThi);
            ldv.setTrangThai(trangThai);

            String loiAnh = luuAnhDanhMuc(ldv, hinhAnhFile);
            loaiDichVuRepository.save(ldv);
            if (loiAnh != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Đã cập nhật danh mục nhưng chưa lưu được ảnh: " + loiAnh);
                return "redirect:/hcns/danh-muc-dich-vu";
            }
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/danh-muc-dich-vu";
    }

    @PostMapping("/danh-muc-dich-vu/xoa")
    public String xoaDanhMuc(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            var ldvOpt = loaiDichVuRepository.findById(id);
            if (ldvOpt.isPresent()) {
                LoaiDichVu ldv = ldvOpt.get();
                try {
                    loaiDichVuRepository.delete(ldv);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục '" + ldv.getTenLoaiDichVu() + "' thành công!");
                } catch (Exception ex) {
                    ldv.setTrangThai("An");
                    loaiDichVuRepository.save(ldv);
                    redirectAttributes.addFlashAttribute("successMessage", "Danh mục đang chứa các dịch vụ nên đã được chuyển sang trạng thái 'Ẩn'!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa danh mục: " + e.getMessage());
        }
        return "redirect:/hcns/danh-muc-dich-vu";
    }

    // =========================================================================
    // 4. UC-HCNS04 – Quản lý Dịch vụ và Bảng giá (CRUD + Phân trang 10 dòng/trang)
    // =========================================================================
    /**
     * Bảng giá có lọc ở server (đúng trên mọi trang): từ khoá, trạng thái, danh mục, dịch vụ,
     * loại hình đặt, khoảng giá, khoảng ngày áp dụng; sắp xếp; 10 dòng/trang.
     */
    @GetMapping("/dich-vu-bang-gia")
    public String dichVuBangGia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) Integer loaiDichVuId,
            @RequestParam(required = false) Integer dichVuId,
            @RequestParam(required = false) String maDichVu,
            @RequestParam(required = false) String loaiHinhDat,
            @RequestParam(required = false) BigDecimal giaTu,
            @RequestParam(required = false) BigDecimal giaDen,
            @RequestParam(required = false) String tuNgay,
            @RequestParam(required = false) String denNgay,
            @RequestParam(defaultValue = "macDinh") String sapXep,
            Model model) {
        if (page < 0) page = 0;
        int pageSize = 10; // ĐẶC TẢ: hiển thị đúng 10 dòng, muốn xem tiếp phải nhấn ->

        String tuKhoa = q != null ? q.trim().toLowerCase() : "";
        LocalDate ngayTu = parseNgay(tuNgay);
        LocalDate ngayDen = parseNgay(denNgay);

        // Lọc mọi điều kiện trừ trạng thái, để đếm số cho từng tab trạng thái
        List<BangGiaDichVu> truocTrangThai = bangGiaDichVuRepository.findAll().stream()
                .filter(bg -> tuKhoa.isEmpty() || chua(bg.getMaBangGia(), tuKhoa)
                        || (bg.getDichVu() != null && (chua(bg.getDichVu().getTenDichVu(), tuKhoa) || chua(bg.getDichVu().getMaDichVu(), tuKhoa))))
                .filter(bg -> loaiDichVuId == null || (bg.getDichVu() != null && bg.getDichVu().getLoaiDichVu() != null
                        && loaiDichVuId.equals(bg.getDichVu().getLoaiDichVu().getId())))
                .filter(bg -> dichVuId == null || (bg.getDichVu() != null && dichVuId.equals(bg.getDichVu().getId())))
                // Lọc theo dịch vụ = theo mã: lấy mọi biến thể cùng mã (2 giờ, 3 giờ, gói tháng...)
                .filter(bg -> maDichVu == null || maDichVu.isBlank()
                        || (bg.getDichVu() != null && maDichVu.equals(bg.getDichVu().getMaDichVu())))
                .filter(bg -> loaiHinhDat == null || loaiHinhDat.isBlank() || loaiHinhDat.equals(bg.getLoaiHinhDat()))
                .filter(bg -> giaTu == null || (bg.getDonGia() != null && bg.getDonGia().compareTo(giaTu) >= 0))
                .filter(bg -> giaDen == null || (bg.getDonGia() != null && bg.getDonGia().compareTo(giaDen) <= 0))
                .filter(bg -> ngayTu == null || (bg.getNgayApDung() != null && !bg.getNgayApDung().toLocalDate().isBefore(ngayTu)))
                .filter(bg -> ngayDen == null || (bg.getNgayApDung() != null && !bg.getNgayApDung().toLocalDate().isAfter(ngayDen)))
                .toList();

        // Trạng thái hiển thị: "Sắp áp dụng" suy ra từ thời điểm áp dụng (CSDL chỉ lưu DangApDung / HetHan)
        LocalDateTime bayGio = LocalDateTime.now();
        Map<Integer, String> hienThi = new HashMap<>();
        truocTrangThai.forEach(bg -> hienThi.put(bg.getId(), com.example.Service.BangGiaService.hienThi(bg, bayGio)));
        long demDangApDung = hienThi.values().stream().filter("DangApDung"::equals).count();
        long demSapApDung = hienThi.values().stream().filter("ChuaApDung"::equals).count();
        String tab = "DangApDung".equals(trangThai) || "HetHan".equals(trangThai) || "ChuaApDung".equals(trangThai) ? trangThai : "";
        java.util.Comparator<BangGiaDichVu> thuTu = switch (sapXep) {
            case "giaTang" -> java.util.Comparator.comparing(BangGiaDichVu::getDonGia, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
            case "giaGiam" -> java.util.Comparator.comparing(BangGiaDichVu::getDonGia, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()));
            case "tenDichVu" -> java.util.Comparator.comparing(bg -> bg.getDichVu() != null ? bg.getDichVu().getTenDichVu() : "",
                    java.text.Collator.getInstance(new java.util.Locale("vi", "VN")));
            case "ngayMoi" -> java.util.Comparator.comparing(BangGiaDichVu::getNgayApDung, java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()));
            default -> java.util.Comparator.comparing(BangGiaDichVu::getId);
        };
        List<BangGiaDichVu> ketQua = truocTrangThai.stream()
                .filter(bg -> tab.isEmpty() || tab.equals(hienThi.get(bg.getId())))
                .sorted(thuTu)
                .toList();

        int totalItems = ketQua.size();
        int totalPages = (int) Math.ceil(totalItems / (double) pageSize);
        if (totalPages > 0 && page >= totalPages) page = totalPages - 1;
        int from = Math.min(page * pageSize, totalItems);
        int to = Math.min(from + pageSize, totalItems);

        // Chuỗi query giữ bộ lọc cho link phân trang, tab, sắp xếp (không kèm page)
        org.springframework.web.util.UriComponentsBuilder qs = org.springframework.web.util.UriComponentsBuilder.newInstance();
        if (!tuKhoa.isEmpty()) qs.queryParam("q", q.trim());
        if (loaiDichVuId != null) qs.queryParam("loaiDichVuId", loaiDichVuId);
        if (dichVuId != null) qs.queryParam("dichVuId", dichVuId);
        if (maDichVu != null && !maDichVu.isBlank()) qs.queryParam("maDichVu", maDichVu);
        if (loaiHinhDat != null && !loaiHinhDat.isBlank()) qs.queryParam("loaiHinhDat", loaiHinhDat);
        if (giaTu != null) qs.queryParam("giaTu", giaTu.toPlainString());
        if (giaDen != null) qs.queryParam("giaDen", giaDen.toPlainString());
        if (ngayTu != null) qs.queryParam("tuNgay", ngayTu);
        if (ngayDen != null) qs.queryParam("denNgay", ngayDen);
        if (!"macDinh".equals(sapXep)) qs.queryParam("sapXep", sapXep);
        String boLocQuery = qs.build().encode().getQuery();
        String filterQuery = boLocQuery == null ? "" : boLocQuery + "&";
        String tabQuery = filterQuery + (tab.isEmpty() ? "" : "trangThai=" + tab + "&");

        model.addAttribute("bangGias", ketQua.subList(from, to));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", (long) totalItems);
        model.addAttribute("pageSize", pageSize);
        List<DichVu> dsDichVu = dichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        model.addAttribute("dichVus", dsDichVu);
        // Ảnh đại diện từng dịch vụ (null = dùng ảnh danh mục) cho khung "Ảnh dịch vụ" ở cửa sổ sửa gói giá
        java.util.Map<Integer, String> anhDichVu = new java.util.HashMap<>();
        for (DichVu dv : dsDichVu) {
            String url = minioService.getViewUrl(dv.getHinhAnh());
            if (url != null) anhDichVu.put(dv.getId(), url);
        }
        model.addAttribute("anhDichVu", anhDichVu);
        model.addAttribute("loaiDichVus", loaiDichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "thuTuHienThi").and(Sort.by(Sort.Direction.ASC, "id"))));

        model.addAttribute("demTatCa", truocTrangThai.size());
        model.addAttribute("demDangApDung", demDangApDung);
        model.addAttribute("demSapApDung", demSapApDung);
        model.addAttribute("demHetHan", truocTrangThai.size() - demDangApDung - demSapApDung);
        // Gói đang quyết định giá hiện tại của từng dịch vụ (đánh dấu "Giá hiện hành")
        java.util.Set<Integer> giaHienHanh = new java.util.HashSet<>();
        truocTrangThai.stream().map(BangGiaDichVu::getDichVu).filter(java.util.Objects::nonNull).distinct()
                .forEach(dv -> bangGiaService.giaApDung(dv.getId(), LocalDateTime.now()).ifPresent(bg -> giaHienHanh.add(bg.getId())));
        model.addAttribute("giaHienHanh", giaHienHanh);
        model.addAttribute("trangThaiHienThi", hienThi);
        model.addAttribute("fTrangThai", tab);
        model.addAttribute("fQ", q != null ? q.trim() : "");
        model.addAttribute("fLoaiDichVuId", loaiDichVuId);
        model.addAttribute("fDichVuId", dichVuId);
        model.addAttribute("fMaDichVu", maDichVu != null ? maDichVu : "");
        // Ô lọc "Dịch vụ": mỗi mã dịch vụ một dòng, tên chung (bỏ phần biến thể trong ngoặc)
        Map<String, String[]> nhomDichVu = new java.util.LinkedHashMap<>();
        for (DichVu dv : dichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "maDichVu").and(Sort.by(Sort.Direction.ASC, "id")))) {
            String ten = dv.getTenDichVu() != null ? dv.getTenDichVu().replaceFirst("\\s*\\(.*\\)\\s*$", "") : dv.getMaDichVu();
            nhomDichVu.putIfAbsent(dv.getMaDichVu(), new String[]{ten,
                    dv.getLoaiDichVu() != null ? String.valueOf(dv.getLoaiDichVu().getId()) : ""});
        }
        model.addAttribute("nhomDichVus", nhomDichVu);
        model.addAttribute("fLoaiHinhDat", loaiHinhDat != null ? loaiHinhDat : "");
        model.addAttribute("fGiaTu", giaTu);
        model.addAttribute("fGiaDen", giaDen);
        model.addAttribute("fTuNgay", ngayTu);
        model.addAttribute("fDenNgay", ngayDen);
        model.addAttribute("fSapXep", sapXep);
        model.addAttribute("filterQuery", filterQuery);   // bộ lọc, chưa kèm trạng thái
        model.addAttribute("tabQuery", tabQuery);         // bộ lọc + trạng thái, dùng cho phân trang
        model.addAttribute("coBoLoc", !filterQuery.isEmpty() && !filterQuery.equals("sapXep=" + sapXep + "&"));
        return "hcns/dich-vu-bang-gia";
    }

    private static boolean chua(String giaTri, String tuKhoa) {
        return giaTri != null && giaTri.toLowerCase().contains(tuKhoa);
    }

    private static LocalDate parseNgay(String s) {
        try {
            return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Ngày nhập không được ở quá khứ. Khi sửa, cho giữ nguyên ngày cũ (ngayCu) dù đã qua.
     */
    private static void khongQuaKhu(LocalDate ngay, LocalDate ngayCu, String tenTruong) {
        if (ngay != null && ngay.isBefore(LocalDate.now()) && !ngay.equals(ngayCu)) {
            throw new IllegalArgumentException(tenTruong + " không được ở quá khứ.");
        }
    }

    @PostMapping("/dich-vu-bang-gia/them")
    public String themBangGia(
            @RequestParam Integer dichVuId,
            @RequestParam(defaultValue = "TheoLan") String loaiHinhDat,
            @RequestParam String donViTinh,
            @RequestParam BigDecimal donGia,
            @RequestParam(required = false) String ngayApDung,
            @RequestParam(required = false) String gioApDung,
            @RequestParam(required = false) String ngayKetThuc,
            @RequestParam(required = false) String gioKetThuc,
            RedirectAttributes redirectAttributes) {
        try {
            var dvOpt = dichVuRepository.findById(dichVuId);
            if (dvOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Dịch vụ không tồn tại!");
                return "redirect:/hcns/dich-vu-bang-gia";
            }

            BangGiaDichVu bg = new BangGiaDichVu();
            bg.setMaBangGia(com.example.Service.MaSinh.tao("BG-"));
            bg.setDichVu(dvOpt.get());
            bg.setLoaiHinhDat(loaiHinhDat);
            bg.setDonViTinh(donViTinh != null ? donViTinh.trim() : "Lần");
            bg.setDonGia(donGia);

            // Thời điểm áp dụng (ngày + giờ); trống = ngay bây giờ. Kết thúc trống = không thời hạn
            LocalDateTime tu = com.example.Service.BangGiaService.ghepNgayGio(ngayApDung, gioApDung, false);
            bg.setNgayApDung(tu != null ? tu : LocalDateTime.now());
            bg.setNgayKetThuc(com.example.Service.BangGiaService.ghepNgayGio(ngayKetThuc, gioKetThuc, true));
            com.example.Service.BangGiaService.kiemTraKhoang(bg.getNgayApDung(), bg.getNgayKetThuc(), null);
            bg.setTrangThai("DangApDung"); // chưa tới giờ áp dụng vẫn lưu DangApDung (CHECK chk_BG_trangThai)
            bangGiaDichVuRepository.save(bg);
            bangGiaService.lamMoi();
            redirectAttributes.addFlashAttribute("successMessage", "Thêm gói bảng giá thành công (Mã: " + bg.getMaBangGia() + ")!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi thêm bảng giá: " + e.getMessage());
        }
        return "redirect:/hcns/dich-vu-bang-gia";
    }

    @PostMapping("/dich-vu-bang-gia/sua")
    public String suaBangGia(
            @RequestParam Integer id,
            @RequestParam Integer dichVuId,
            @RequestParam(defaultValue = "TheoLan") String loaiHinhDat,
            @RequestParam String donViTinh,
            @RequestParam BigDecimal donGia,
            @RequestParam(required = false) String ngayApDung,
            @RequestParam(required = false) String gioApDung,
            @RequestParam(required = false) String ngayKetThuc,
            @RequestParam(required = false) String gioKetThuc,
            @RequestParam(defaultValue = "DangApDung") String trangThai,
            RedirectAttributes redirectAttributes) {
        try {
            var bgOpt = bangGiaDichVuRepository.findById(id);
            if (bgOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Gói bảng giá không tồn tại!");
                return "redirect:/hcns/dich-vu-bang-gia";
            }

            var dvOpt = dichVuRepository.findById(dichVuId);
            if (dvOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Dịch vụ không hợp lệ!");
                return "redirect:/hcns/dich-vu-bang-gia";
            }

            BangGiaDichVu bg = bgOpt.get();
            LocalDateTime apDungCu = bg.getNgayApDung();
            bg.setDichVu(dvOpt.get());
            bg.setLoaiHinhDat(loaiHinhDat);
            bg.setDonViTinh(donViTinh.trim());
            // Đơn giá không sửa trực tiếp: dùng "Cập nhật giá" để tạo bảng giá mới, giữ lịch sử giá
            boolean doiGia = donGia != null && bg.getDonGia() != null && donGia.compareTo(bg.getDonGia()) != 0;

            LocalDateTime tu = com.example.Service.BangGiaService.ghepNgayGio(ngayApDung, gioApDung, false);
            if (tu != null) bg.setNgayApDung(tu);
            bg.setNgayKetThuc(com.example.Service.BangGiaService.ghepNgayGio(ngayKetThuc, gioKetThuc, true));
            // Giữ nguyên thời điểm áp dụng cũ (đã qua) thì không báo quá khứ
            com.example.Service.BangGiaService.kiemTraKhoang(bg.getNgayApDung(), bg.getNgayKetThuc(), apDungCu);
            bg.setTrangThai(trangThai);

            bangGiaDichVuRepository.save(bg);
            bangGiaService.lamMoi(); // ngừng / áp dụng lại / đổi ngày: tính lại giá dịch vụ
            redirectAttributes.addFlashAttribute("successMessage", doiGia
                    ? "Đã lưu thông tin gói. Đơn giá không đổi ở đây, hãy dùng \"Cập nhật giá\" để tạo bảng giá mới."
                    : "Cập nhật bảng giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật bảng giá: " + e.getMessage());
        }
        return "redirect:/hcns/dich-vu-bang-gia";
    }

    /**
     * Cập nhật giá nhiều gói cùng lúc: mỗi gói ids[i] được tạo một bảng giá mới giá donGia[i], áp dụng từ
     * ngayApDung (trống = hôm nay) đến ngayKetThuc (trống = không thời hạn). Bảng giá cũ giữ nguyên;
     * hết thời hạn thì giá tự quay về bảng giá chưa kết thúc mới nhất (BangGiaService).
     * Chạy trong một giao dịch: một gói lỗi thì không gói nào bị đổi.
     */
    @PostMapping("/dich-vu-bang-gia/sua-hang-loat")
    @Transactional
    public String suaGiaHangLoat(
            @RequestParam(name = "ids", required = false) List<Integer> ids,
            @RequestParam(name = "donGia", required = false) List<BigDecimal> donGias,
            @RequestParam(required = false) String ngayApDung,
            @RequestParam(required = false) String gioApDung,
            @RequestParam(required = false) String ngayKetThuc,
            @RequestParam(required = false) String gioKetThuc,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String returnQuery,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        // Quay về đúng bộ lọc và trang đang xem; chỉ nhận ký tự của query string đã mã hoá
        String giuLoc = (returnQuery != null && returnQuery.matches("[A-Za-z0-9%&=._+\\-]*")) ? returnQuery : "";
        String back = "redirect:/hcns/dich-vu-bang-gia?" + giuLoc + "page=" + Math.max(page, 0);
        if (ids == null || ids.isEmpty() || donGias == null || ids.size() != donGias.size()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Chưa chọn gói giá nào để sửa.");
            return back;
        }
        try {
            LocalDateTime tuNhap = com.example.Service.BangGiaService.ghepNgayGio(ngayApDung, gioApDung, false);
            LocalDateTime tu = tuNhap != null ? tuNhap : LocalDateTime.now();
            LocalDateTime den = com.example.Service.BangGiaService.ghepNgayGio(ngayKetThuc, gioKetThuc, true);
            String nguoi = nguoiCapNhat(request);
            for (int i = 0; i < ids.size(); i++) {
                bangGiaService.capNhatGia(ids.get(i), donGias.get(i), tu, den, nguoi);
            }
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo bảng giá mới cho " + ids.size() + " gói, áp dụng từ "
                    + tu.format(f) + (den != null ? " đến " + den.format(f) + ", sau đó quay về giá trước đó." : "."));
        } catch (Exception e) {
            org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            redirectAttributes.addFlashAttribute("errorMessage", "Không cập nhật được giá: " + e.getMessage() + ". Chưa gói nào bị đổi.");
        }
        return back;
    }

    /** Cập nhật giá một gói: tạo bảng giá mới (giữ bảng giá cũ), áp dụng từ ngày bắt đầu đến ngày kết thúc. */
    @PostMapping("/dich-vu-bang-gia/cap-nhat-gia")
    public String capNhatGiaMotGoi(@RequestParam Integer id,
                                   @RequestParam BigDecimal donGia,
                                   @RequestParam(required = false) String ngayApDung,
                                   @RequestParam(required = false) String gioApDung,
                                   @RequestParam(required = false) String ngayKetThuc,
                                   @RequestParam(required = false) String gioKetThuc,
                                   HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime tuNhap = com.example.Service.BangGiaService.ghepNgayGio(ngayApDung, gioApDung, false);
            LocalDateTime tu = tuNhap != null ? tuNhap : LocalDateTime.now();
            LocalDateTime den = com.example.Service.BangGiaService.ghepNgayGio(ngayKetThuc, gioKetThuc, true);
            BangGiaDichVu moi = bangGiaService.capNhatGia(id, donGia, tu, den, nguoiCapNhat(request));
            java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo bảng giá " + moi.getMaBangGia() + " áp dụng từ "
                    + tu.format(f) + (den != null ? " đến " + den.format(f) + ", sau đó quay về giá trước đó." : "."));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật giá: " + e.getMessage());
        }
        return "redirect:/hcns/dich-vu-bang-gia";
    }

    private static String nguoiCapNhat(HttpServletRequest request) {
        Object ten = request.getSession().getAttribute("authenticatedFullName");
        return ten != null ? "HCNS - " + ten : "HCNS";
    }

    @PostMapping("/dich-vu-bang-gia/xoa")
    public String xoaBangGia(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            var bgOpt = bangGiaDichVuRepository.findById(id);
            if (bgOpt.isPresent()) {
                BangGiaDichVu bg = bgOpt.get();
                try {
                    bangGiaDichVuRepository.delete(bg);
                    bangGiaService.lamMoi();
                    redirectAttributes.addFlashAttribute("successMessage", "Đã xóa gói bảng giá thành công!");
                } catch (Exception ex) {
                    bg.setTrangThai("HetHan");
                    bangGiaDichVuRepository.save(bg);
                    bangGiaService.lamMoi();
                    redirectAttributes.addFlashAttribute("successMessage", "Gói giá đã có đơn hàng sử dụng nên được chuyển sang trạng thái 'Hết hạn'!");
                }
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/hcns/dich-vu-bang-gia";
    }

    // =========================================================================
    // 5. UC-HCNS05 – Quản lý tài khoản (CRUD + Phân quyền + Mã hóa BCrypt)
    // =========================================================================
    @GetMapping("/tai-khoan")
    public String taiKhoan(Model model, jakarta.servlet.http.HttpSession session) {
        model.addAttribute("currentTaiKhoanId", session.getAttribute("authenticatedUserId"));
        List<TaiKhoan> taiKhoans = taiKhoanRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<NhanVien> nhanViens = nhanVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<CongTacVien> congTacViens = congTacVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<KhachHang> khachHangs = khachHangRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        // Một tài khoản có thể vừa là khách hàng vừa là CTV (dùng chung SĐT): hiện đủ các vai trò
        java.util.Set<Integer> taiKhoanLaCtv = new java.util.HashSet<>();
        congTacViens.forEach(c -> { if (c.getTaiKhoan() != null) taiKhoanLaCtv.add(c.getTaiKhoan().getId()); });
        java.util.Set<Integer> taiKhoanLaKh = new java.util.HashSet<>();
        khachHangs.forEach(k -> { if (k.getTaiKhoan() != null) taiKhoanLaKh.add(k.getTaiKhoan().getId()); });
        model.addAttribute("taiKhoanLaCtv", taiKhoanLaCtv);
        model.addAttribute("taiKhoanLaKh", taiKhoanLaKh);

        // Tạo map ownerNames và ownerCodes: id tài khoản -> Tên & Mã hiển thị
        Map<Integer, String> ownerNames = new HashMap<>();
        Map<Integer, String> ownerCodes = new HashMap<>();
        for (NhanVien nv : nhanViens) {
            if (nv.getTaiKhoan() != null && nv.getTaiKhoan().getId() != null) {
                ownerNames.put(nv.getTaiKhoan().getId(), nv.getHoTen());
                ownerCodes.put(nv.getTaiKhoan().getId(), nv.getMaNhanVien());
            }
        }
        for (CongTacVien ctv : congTacViens) {
            if (ctv.getTaiKhoan() != null && ctv.getTaiKhoan().getId() != null) {
                ownerNames.put(ctv.getTaiKhoan().getId(), ctv.getHoTen());
                ownerCodes.put(ctv.getTaiKhoan().getId(), ctv.getMaCongTacVien());
            }
        }
        for (KhachHang kh : khachHangs) {
            if (kh.getTaiKhoan() != null && kh.getTaiKhoan().getId() != null) {
                ownerNames.put(kh.getTaiKhoan().getId(), kh.getHoTen());
                ownerCodes.put(kh.getTaiKhoan().getId(), kh.getMaKhachHang());
            }
        }

        // Thống kê
        long tongTaiKhoan = taiKhoans.size();
        long countNoiBo = taiKhoans.stream().filter(t -> "NhanVien".equalsIgnoreCase(t.getLoaiTaiKhoan())).count();
        long countCTV = taiKhoans.stream().filter(t -> "CongTacVien".equalsIgnoreCase(t.getLoaiTaiKhoan())).count();
        long countKH = taiKhoans.stream().filter(t -> "KhachHang".equalsIgnoreCase(t.getLoaiTaiKhoan())).count();
        long countBiKhoa = taiKhoans.stream().filter(t -> "BiKhoa".equalsIgnoreCase(t.getTrangThai())).count();
        long countChoDuyet = taiKhoans.stream().filter(t -> "ChoDuyet".equalsIgnoreCase(t.getTrangThai())).count();

        model.addAttribute("taiKhoans", taiKhoans);
        model.addAttribute("nhanViens", nhanViens);
        model.addAttribute("congTacViens", congTacViens);
        model.addAttribute("khachHangs", khachHangs);
        model.addAttribute("ownerNames", ownerNames);
        model.addAttribute("ownerCodes", ownerCodes);

        model.addAttribute("tongTaiKhoan", tongTaiKhoan);
        model.addAttribute("countNoiBo", countNoiBo);
        model.addAttribute("countCTV", countCTV);
        model.addAttribute("countKH", countKH);
        model.addAttribute("countBiKhoa", countBiKhoa);
        model.addAttribute("countChoDuyet", countChoDuyet);

        return "hcns/tai-khoan";
    }

    @PostMapping("/tai-khoan/them")
    public String themTaiKhoan(
            @RequestParam String tenDangNhap,
            @RequestParam String matKhau,
            @RequestParam String email,
            @RequestParam String soDienThoai,
            @RequestParam String loaiTaiKhoan,
            @RequestParam(required = false) Integer targetPersonId,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            tenDangNhap = tenDangNhap != null ? tenDangNhap.trim() : "";
            matKhau = matKhau != null ? matKhau.trim() : "";
            email = email != null ? email.trim() : "";
            soDienThoai = soDienThoai != null ? soDienThoai.trim() : "";
            loaiTaiKhoan = loaiTaiKhoan != null ? loaiTaiKhoan.trim() : "NhanVien";

            // 1. Kiểm tra trống & khoảng trắng
            if (tenDangNhap.isEmpty() || matKhau.isEmpty() || email.isEmpty() || soDienThoai.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng nhập đầy đủ các trường thông tin bắt buộc!");
                return "redirect:/hcns/tai-khoan";
            }

            if (hasWhiteSpace(tenDangNhap) || hasWhiteSpace(email) || hasWhiteSpace(matKhau) || hasWhiteSpace(soDienThoai)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập, mật khẩu, email và số điện thoại không được chứa khoảng trắng!");
                return "redirect:/hcns/tai-khoan";
            }

            // 2. Kiểm tra định dạng số điện thoại
            if (!soDienThoai.matches("\\d{1,11}")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại chỉ được chứa số và không được vượt quá 11 kí tự!");
                return "redirect:/hcns/tai-khoan";
            }

            // 3. Kiểm tra độ dài mật khẩu (tối thiểu 6 kí tự)
            if (matKhau.length() < 6) {
                redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu khởi tạo phải có ít nhất 6 ký tự!");
                return "redirect:/hcns/tai-khoan";
            }

            // 4. Kiểm tra trùng Tên đăng nhập
            if (taiKhoanRepository.findByTenDangNhap(tenDangNhap).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập '" + tenDangNhap + "' đã tồn tại trên hệ thống!");
                return "redirect:/hcns/tai-khoan";
            }

            // 5. Kiểm tra trùng Email
            if (taiKhoanRepository.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + email + "' đã được sử dụng bởi tài khoản khác!");
                return "redirect:/hcns/tai-khoan";
            }

            // 6. Kiểm tra trùng Số điện thoại
            if (taiKhoanRepository.findBySoDienThoai(soDienThoai).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại '" + soDienThoai + "' đã được sử dụng bởi tài khoản khác!");
                return "redirect:/hcns/tai-khoan";
            }

            // 7. Kiểm tra người được chọn đã có tài khoản chưa (nếu có chọn)
            NhanVien targetNV = null;
            CongTacVien targetCTV = null;
            KhachHang targetKH = null;

            if (targetPersonId != null && targetPersonId > 0) {
                if ("NhanVien".equalsIgnoreCase(loaiTaiKhoan)) {
                    var opt = nhanVienRepository.findById(targetPersonId);
                    if (opt.isPresent()) {
                        targetNV = opt.get();
                        if (targetNV.getTaiKhoan() != null) {
                            redirectAttributes.addFlashAttribute("errorMessage", "Nhân viên '" + targetNV.getHoTen() + "' đã có tài khoản (" + targetNV.getTaiKhoan().getTenDangNhap() + ")!");
                            return "redirect:/hcns/tai-khoan";
                        }
                    }
                } else if ("CongTacVien".equalsIgnoreCase(loaiTaiKhoan)) {
                    var opt = congTacVienRepository.findById(targetPersonId);
                    if (opt.isPresent()) {
                        targetCTV = opt.get();
                        if (targetCTV.getTaiKhoan() != null) {
                            redirectAttributes.addFlashAttribute("errorMessage", "Cộng tác viên '" + targetCTV.getHoTen() + "' đã có tài khoản (" + targetCTV.getTaiKhoan().getTenDangNhap() + ")!");
                            return "redirect:/hcns/tai-khoan";
                        }
                    }
                } else if ("KhachHang".equalsIgnoreCase(loaiTaiKhoan)) {
                    var opt = khachHangRepository.findById(targetPersonId);
                    if (opt.isPresent()) {
                        targetKH = opt.get();
                        if (targetKH.getTaiKhoan() != null) {
                            redirectAttributes.addFlashAttribute("errorMessage", "Khách hàng '" + targetKH.getHoTen() + "' đã có tài khoản (" + targetKH.getTaiKhoan().getTenDangNhap() + ")!");
                            return "redirect:/hcns/tai-khoan";
                        }
                    }
                }
            }

            // 8. Mã hóa mật khẩu bằng BCrypt
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encodedPassword = passwordEncoder.encode(matKhau);

            // 9. Lưu tài khoản mới
            TaiKhoan tk = new TaiKhoan();
            tk.setMaTaiKhoan(com.example.Service.MaSinh.tao("TK-"));
            tk.setTenDangNhap(tenDangNhap);
            tk.setMatKhau(encodedPassword); // Mật khẩu được mã hóa an toàn bằng BCrypt
            tk.setEmail(email);
            tk.setSoDienThoai(soDienThoai);
            tk.setLoaiTaiKhoan(loaiTaiKhoan);
            tk.setTrangThai("HoatDong");
            tk.setNgayTao(LocalDateTime.now());
            taiKhoanRepository.save(tk);

            // 10. Gán tài khoản cho người sở hữu nếu có
            if (targetNV != null) {
                targetNV.setTaiKhoan(tk);
                nhanVienRepository.save(targetNV);
            } else if (targetCTV != null) {
                targetCTV.setTaiKhoan(tk);
                congTacVienRepository.save(targetCTV);
            } else if (targetKH != null) {
                targetKH.setTaiKhoan(tk);
                khachHangRepository.save(targetKH);
            }

            // 11. Ghi nhật ký hệ thống
            try {
                NhatKyTaiKhoan log = new NhatKyTaiKhoan();
                log.setTaiKhoan(tk);
                log.setHanhDong("Tạo tài khoản mới (" + loaiTaiKhoan + ")");
                log.setDiaChiIP(request.getRemoteAddr());
                log.setThietBi(request.getHeader("User-Agent"));
                log.setKetQua("ThanhCong");
                nhatKyTaiKhoanRepository.save(log);
            } catch (Exception ignored) {}

            redirectAttributes.addFlashAttribute("successMessage", "Cấp tài khoản '" + tenDangNhap + "' thành công (Mật khẩu đã được mã hóa BCrypt an toàn)!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi tạo tài khoản: " + e.getMessage());
        }
        return "redirect:/hcns/tai-khoan";
    }

    @PostMapping("/tai-khoan/sua")
    public String suaTaiKhoan(
            @RequestParam Integer id,
            @RequestParam String tenDangNhap,
            @RequestParam String email,
            @RequestParam String soDienThoai,
            @RequestParam String loaiTaiKhoan,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String matKhauMoi,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            tenDangNhap = tenDangNhap != null ? tenDangNhap.trim() : "";
            email = email != null ? email.trim() : "";
            soDienThoai = soDienThoai != null ? soDienThoai.trim() : "";
            matKhauMoi = matKhauMoi != null ? matKhauMoi.trim() : "";
            trangThai = trangThai != null ? trangThai.trim() : "";

            if (tenDangNhap.isEmpty() || email.isEmpty() || soDienThoai.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập, email và số điện thoại không được để trống!");
                return "redirect:/hcns/tai-khoan";
            }

            if (hasWhiteSpace(tenDangNhap) || hasWhiteSpace(email) || hasWhiteSpace(soDienThoai)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập, email và số điện thoại không được chứa khoảng trắng!");
                return "redirect:/hcns/tai-khoan";
            }

            if (!soDienThoai.matches("\\d{1,11}")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại chỉ được chứa số và tối đa 11 kí tự!");
                return "redirect:/hcns/tai-khoan";
            }

            // Kiểm tra trùng
            var existingTk = taiKhoanRepository.findByTenDangNhap(tenDangNhap);
            if (existingTk.isPresent() && !existingTk.get().getId().equals(id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên đăng nhập '" + tenDangNhap + "' đã thuộc về tài khoản khác!");
                return "redirect:/hcns/tai-khoan";
            }

            var existingEmail = taiKhoanRepository.findByEmail(email);
            if (existingEmail.isPresent() && !existingEmail.get().getId().equals(id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Email '" + email + "' đã thuộc về tài khoản khác!");
                return "redirect:/hcns/tai-khoan";
            }

            var existingPhone = taiKhoanRepository.findBySoDienThoai(soDienThoai);
            if (existingPhone.isPresent() && !existingPhone.get().getId().equals(id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số điện thoại '" + soDienThoai + "' đã thuộc về tài khoản khác!");
                return "redirect:/hcns/tai-khoan";
            }

            var opt = taiKhoanRepository.findById(id);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản cần cập nhật!");
                return "redirect:/hcns/tai-khoan";
            }

            TaiKhoan tk = opt.get();
            tk.setTenDangNhap(tenDangNhap);
            tk.setEmail(email);
            tk.setSoDienThoai(soDienThoai);
            tk.setLoaiTaiKhoan(loaiTaiKhoan);
            if (!trangThai.isEmpty()) {
                tk.setTrangThai(trangThai);
            }
            tk.setNgayCapNhat(LocalDateTime.now());

            if (!matKhauMoi.isEmpty()) {
                if (hasWhiteSpace(matKhauMoi)) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới không được chứa khoảng trắng!");
                    return "redirect:/hcns/tai-khoan";
                }
                if (matKhauMoi.length() < 6) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu mới phải có ít nhất 6 ký tự!");
                    return "redirect:/hcns/tai-khoan";
                }
                BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
                tk.setMatKhau(passwordEncoder.encode(matKhauMoi));
            }

            taiKhoanRepository.save(tk);

            // Đồng bộ trạng thái sang hồ sơ CTV nếu có
            if ("CongTacVien".equalsIgnoreCase(tk.getLoaiTaiKhoan())) {
                congTacVienRepository.findByTaiKhoan(tk).ifPresent(ctv -> {
                    if ("HoatDong".equalsIgnoreCase(tk.getTrangThai())) {
                        ctv.setTrangThai("HoatDong");
                    } else if ("BiKhoa".equalsIgnoreCase(tk.getTrangThai()) || "Khoa".equalsIgnoreCase(tk.getTrangThai())) {
                        ctv.setTrangThai("DinhChi");
                    } else if ("ChoDuyet".equalsIgnoreCase(tk.getTrangThai())) {
                        ctv.setTrangThai("ChoDuyet");
                    }
                    congTacVienRepository.save(ctv);
                });
            }

            // Ghi log
            try {
                NhatKyTaiKhoan log = new NhatKyTaiKhoan();
                log.setTaiKhoan(tk);
                log.setHanhDong("Cập nhật thông tin tài khoản" + (!matKhauMoi.isEmpty() ? " & Đổi mật khẩu (BCrypt)" : ""));
                log.setDiaChiIP(request.getRemoteAddr());
                log.setThietBi(request.getHeader("User-Agent"));
                log.setKetQua("ThanhCong");
                nhatKyTaiKhoanRepository.save(log);
            } catch (Exception ignored) {}

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản '" + tenDangNhap + "' thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật tài khoản: " + e.getMessage());
        }
        return "redirect:/hcns/tai-khoan";
    }

    /** Trạng thái tài khoản HCNS được phép đặt. */
    private static final List<String> TRANG_THAI_TAI_KHOAN = List.of("HoatDong", "ChoDuyet", "BiKhoa");

    /**
     * HCNS đổi trạng thái tài khoản: Hoạt động / Chờ duyệt / Bị khóa.
     * - Không tự đổi trạng thái tài khoản đang đăng nhập.
     * - Tài khoản CTV có hồ sơ đang chờ duyệt: kích hoạt = duyệt hồ sơ (cùng luồng trang Cộng tác viên).
     * - Trạng thái hồ sơ CTV đi theo trạng thái tài khoản (Bị khóa -> Đình chỉ).
     */
    @PostMapping("/tai-khoan/doi-trang-thai")
    public String doiTrangThaiTaiKhoan(
            @RequestParam Integer id,
            @RequestParam String trangThai,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            if (!TRANG_THAI_TAI_KHOAN.contains(trangThai)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ: " + trangThai);
                return "redirect:/hcns/tai-khoan";
            }
            Object dangDangNhap = request.getSession().getAttribute("authenticatedUserId");
            if (dangDangNhap != null && id.equals(dangDangNhap)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể tự đổi trạng thái tài khoản đang đăng nhập.");
                return "redirect:/hcns/tai-khoan";
            }

            var opt = taiKhoanRepository.findById(id);
            if (opt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản!");
                return "redirect:/hcns/tai-khoan";
            }
            TaiKhoan tk = opt.get();
            if (trangThai.equals(tk.getTrangThai())) {
                return "redirect:/hcns/tai-khoan";
            }

            CongTacVien ctv = "CongTacVien".equalsIgnoreCase(tk.getLoaiTaiKhoan())
                    ? congTacVienRepository.findByTaiKhoan(tk).orElse(null) : null;

            if (ctv != null && "ChoDuyet".equals(ctv.getTrangThai()) && "HoatDong".equals(trangThai)) {
                // Kích hoạt tài khoản CTV chờ duyệt = duyệt hồ sơ, giữ mật khẩu ứng viên đã đặt
                var kq = capTaiKhoanCtvService.duyetVaCapTaiKhoan(ctv.getId(), null,
                        request.getRemoteAddr(), request.getHeader("User-Agent"));
                baoKetQuaDuyet(kq, redirectAttributes);
                return "redirect:/hcns/tai-khoan";
            }

            String trangThaiCu = tk.getTrangThai();
            tk.setTrangThai(trangThai);
            tk.setNgayCapNhat(LocalDateTime.now());
            taiKhoanRepository.save(tk);

            if (ctv != null) {
                ctv.setTrangThai(switch (trangThai) {
                    case "HoatDong" -> "HoatDong";
                    case "BiKhoa" -> "DinhChi";
                    default -> "ChoDuyet";
                });
                congTacVienRepository.save(ctv);
            }

            try {
                NhatKyTaiKhoan log = new NhatKyTaiKhoan();
                log.setTaiKhoan(tk);
                log.setHanhDong("HCNS đổi trạng thái tài khoản: " + trangThaiCu + " -> " + trangThai);
                log.setDiaChiIP(request.getRemoteAddr());
                log.setThietBi(request.getHeader("User-Agent"));
                log.setKetQua("ThanhCong");
                nhatKyTaiKhoanRepository.save(log);
            } catch (Exception ignored) {}

            String actionText = switch (trangThai) {
                case "HoatDong" -> "BiKhoa".equals(trangThaiCu) ? "Mở khóa" : "Kích hoạt";
                case "ChoDuyet" -> "Chuyển chờ duyệt";
                default -> "Khóa";
            };
            redirectAttributes.addFlashAttribute("successMessage", actionText + " tài khoản '" + tk.getTenDangNhap() + "' thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi đổi trạng thái: " + e.getMessage());
        }
        return "redirect:/hcns/tai-khoan";
    }

    @PostMapping("/tai-khoan/xoa")
    public String xoaTaiKhoan(
            @RequestParam Integer id,
            RedirectAttributes redirectAttributes) {

        try {
            var opt = taiKhoanRepository.findById(id);
            if (opt.isPresent()) {
                TaiKhoan tk = opt.get();

                // 1. Gỡ liên kết khỏi NhanVien nếu có
                var optNV = nhanVienRepository.findByTaiKhoan(tk);
                if (optNV.isPresent()) {
                    NhanVien nv = optNV.get();
                    nv.setTaiKhoan(null);
                    nhanVienRepository.save(nv);
                }

                // 2. Gỡ liên kết khỏi CongTacVien nếu có
                var optCTV = congTacVienRepository.findByTaiKhoan(tk);
                if (optCTV.isPresent()) {
                    CongTacVien ctv = optCTV.get();
                    ctv.setTaiKhoan(null);
                    congTacVienRepository.save(ctv);
                }

                // 3. Gỡ liên kết khỏi KhachHang nếu có
                var optKH = khachHangRepository.findByTaiKhoan(tk);
                if (optKH.isPresent()) {
                    KhachHang kh = optKH.get();
                    kh.setTaiKhoan(null);
                    khachHangRepository.save(kh);
                }

                // 4. Xóa nhật ký tài khoản
                try {
                    nhatKyTaiKhoanRepository.deleteByTaiKhoan(tk);
                } catch (Exception ignored) {}

                // 5. Xóa tài khoản
                taiKhoanRepository.delete(tk);

                redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài khoản '" + tk.getTenDangNhap() + "' an toàn thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản để xóa!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa tài khoản: " + e.getMessage());
        }
        return "redirect:/hcns/tai-khoan";
    }

    private boolean hasWhiteSpace(String str) {
        if (str == null) return false;
        return str.chars().anyMatch(Character::isWhitespace);
    }

    private LocalDate parseNgaySinh(String ngaySinhStr) {
        if (ngaySinhStr == null || ngaySinhStr.trim().isEmpty()) {
            return null;
        }
        String s = ngaySinhStr.trim();
        java.time.format.DateTimeFormatter[] formatters = new java.time.format.DateTimeFormatter[] {
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd")
        };
        for (java.time.format.DateTimeFormatter fmt : formatters) {
            try {
                return LocalDate.parse(s, fmt);
            } catch (Exception ignored) {}
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception ignored) {}
        return null;
    }
}
