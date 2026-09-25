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
            NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository) {
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
    @GetMapping("/cong-tac-vien")
    public String congTacVien(Model model) {
        model.addAttribute("congTacVienStats", thongKeService.getCongTacVienStats());
        model.addAttribute("congTacViens", congTacVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return "hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/them")
    public String themCongTacVien(
            @RequestParam String hoTen,
            @RequestParam String soDienThoai,
            @RequestParam String noiCuTru,
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
            ctv.setNoiCuTru(noiCuTru != null ? noiCuTru.trim() : "TP.HCM");
            ctv.setGioiTinh(gioiTinh);
            ctv.setNgaySinh(dob);
            ctv.setCapDo(capDo);
            ctv.setTrangThai(trangThai);
            ctv.setDiemDanhGia(BigDecimal.valueOf(5.0));
            ctv.setNgayDangKy(LocalDate.now());

            congTacVienRepository.save(ctv);
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
            @RequestParam String noiCuTru,
            @RequestParam(required = false) String ngaySinh,
            @RequestParam String capDo,
            @RequestParam String trangThai,
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
            ctv.setNoiCuTru(noiCuTru.trim());
            ctv.setCapDo(capDo);
            ctv.setTrangThai(trangThai);

            congTacVienRepository.save(ctv);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ CTV thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật CTV: " + e.getMessage());
        }
        return "redirect:/hcns/cong-tac-vien";
    }

    @PostMapping("/cong-tac-vien/duyet")
    public String duyetCongTacVien(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            congTacVienRepository.findById(id).ifPresent(ctv -> {
                ctv.setTrangThai("HoatDong");
                congTacVienRepository.save(ctv);
            });
            redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt hồ sơ cộng tác viên thành công!");
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
            RedirectAttributes redirectAttributes) {
        try {
            congTacVienRepository.findById(id).ifPresent(ctv -> {
                ctv.setTrangThai(trangThai);
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
        return "hcns/danh-muc-dich-vu";
    }

    @PostMapping("/danh-muc-dich-vu/them")
    public String themDanhMuc(
            @RequestParam String tenLoaiDichVu,
            @RequestParam(required = false) String moTa,
            @RequestParam(required = false, defaultValue = "1") Integer thuTuHienThi,
            @RequestParam(required = false, defaultValue = "HienThi") String trangThai,
            RedirectAttributes redirectAttributes) {
        try {
            if (tenLoaiDichVu == null || tenLoaiDichVu.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tên loại danh mục không được để trống!");
                return "redirect:/hcns/danh-muc-dich-vu";
            }

            LoaiDichVu ldv = new LoaiDichVu();
            ldv.setMaLoaiDichVu("LDV-" + (loaiDichVuRepository.count() + 1));
            ldv.setTenLoaiDichVu(tenLoaiDichVu.trim());
            ldv.setMoTa(moTa != null ? moTa.trim() : "");
            ldv.setThuTuHienThi(thuTuHienThi != null ? thuTuHienThi : 1);
            ldv.setTrangThai(trangThai);

            loaiDichVuRepository.save(ldv);
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

            loaiDichVuRepository.save(ldv);
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
    @GetMapping("/dich-vu-bang-gia")
    public String dichVuBangGia(
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        if (page < 0) page = 0;
        int pageSize = 10; // ĐẶC TẢ: hiển thị đúng 10 dòng, muốn xem tiếp phải nhấn ->

        Page<BangGiaDichVu> pageBangGia = bangGiaDichVuRepository.findAll(
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.ASC, "id"))
        );

        model.addAttribute("bangGias", pageBangGia.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageBangGia.getTotalPages());
        model.addAttribute("totalItems", pageBangGia.getTotalElements());
        model.addAttribute("pageSize", pageSize);
        model.addAttribute("dichVus", dichVuRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
        return "hcns/dich-vu-bang-gia";
    }

    @PostMapping("/dich-vu-bang-gia/them")
    public String themBangGia(
            @RequestParam Integer dichVuId,
            @RequestParam(defaultValue = "TheoLan") String loaiHinhDat,
            @RequestParam String donViTinh,
            @RequestParam BigDecimal donGia,
            @RequestParam(required = false) String ngayApDung,
            @RequestParam(required = false) String ngayKetThuc,
            RedirectAttributes redirectAttributes) {
        try {
            var dvOpt = dichVuRepository.findById(dichVuId);
            if (dvOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Dịch vụ không tồn tại!");
                return "redirect:/hcns/dich-vu-bang-gia";
            }

            BangGiaDichVu bg = new BangGiaDichVu();
            bg.setMaBangGia("BG-" + System.currentTimeMillis() % 1000000);
            bg.setDichVu(dvOpt.get());
            bg.setLoaiHinhDat(loaiHinhDat);
            bg.setDonViTinh(donViTinh != null ? donViTinh.trim() : "Lần");
            bg.setDonGia(donGia);

            if (ngayApDung != null && !ngayApDung.trim().isEmpty()) {
                bg.setNgayApDung(LocalDate.parse(ngayApDung.trim()));
            } else {
                bg.setNgayApDung(LocalDate.now());
            }

            if (ngayKetThuc != null && !ngayKetThuc.trim().isEmpty()) {
                bg.setNgayKetThuc(LocalDate.parse(ngayKetThuc.trim()));
            }

            bg.setTrangThai("DangApDung");
            bangGiaDichVuRepository.save(bg);
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
            @RequestParam(required = false) String ngayKetThuc,
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
            bg.setDichVu(dvOpt.get());
            bg.setLoaiHinhDat(loaiHinhDat);
            bg.setDonViTinh(donViTinh.trim());
            bg.setDonGia(donGia);

            if (ngayApDung != null && !ngayApDung.trim().isEmpty()) {
                bg.setNgayApDung(LocalDate.parse(ngayApDung.trim()));
            }
            if (ngayKetThuc != null && !ngayKetThuc.trim().isEmpty()) {
                bg.setNgayKetThuc(LocalDate.parse(ngayKetThuc.trim()));
            } else {
                bg.setNgayKetThuc(null);
            }
            bg.setTrangThai(trangThai);

            bangGiaDichVuRepository.save(bg);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật bảng giá thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật bảng giá: " + e.getMessage());
        }
        return "redirect:/hcns/dich-vu-bang-gia";
    }

    @PostMapping("/dich-vu-bang-gia/xoa")
    public String xoaBangGia(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            var bgOpt = bangGiaDichVuRepository.findById(id);
            if (bgOpt.isPresent()) {
                BangGiaDichVu bg = bgOpt.get();
                try {
                    bangGiaDichVuRepository.delete(bg);
                    redirectAttributes.addFlashAttribute("successMessage", "Đã xóa gói bảng giá thành công!");
                } catch (Exception ex) {
                    bg.setTrangThai("HetHan");
                    bangGiaDichVuRepository.save(bg);
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
    public String taiKhoan(Model model) {
        List<TaiKhoan> taiKhoans = taiKhoanRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<NhanVien> nhanViens = nhanVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<CongTacVien> congTacViens = congTacVienRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<KhachHang> khachHangs = khachHangRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

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
            tk.setMaTaiKhoan("TK-" + System.currentTimeMillis());
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
            @RequestParam(required = false) String matKhauMoi,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            tenDangNhap = tenDangNhap != null ? tenDangNhap.trim() : "";
            email = email != null ? email.trim() : "";
            soDienThoai = soDienThoai != null ? soDienThoai.trim() : "";
            matKhauMoi = matKhauMoi != null ? matKhauMoi.trim() : "";

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

    @PostMapping("/tai-khoan/doi-trang-thai")
    public String doiTrangThaiTaiKhoan(
            @RequestParam Integer id,
            @RequestParam String trangThai,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        try {
            var opt = taiKhoanRepository.findById(id);
            if (opt.isPresent()) {
                TaiKhoan tk = opt.get();
                tk.setTrangThai(trangThai);
                tk.setNgayCapNhat(LocalDateTime.now());
                taiKhoanRepository.save(tk);

                try {
                    NhatKyTaiKhoan log = new NhatKyTaiKhoan();
                    log.setTaiKhoan(tk);
                    log.setHanhDong("Đổi trạng thái: " + trangThai);
                    log.setDiaChiIP(request.getRemoteAddr());
                    log.setThietBi(request.getHeader("User-Agent"));
                    log.setKetQua("ThanhCong");
                    nhatKyTaiKhoanRepository.save(log);
                } catch (Exception ignored) {}

                String actionText = "HoatDong".equals(trangThai) ? "Mở khóa" : "Khóa";
                redirectAttributes.addFlashAttribute("successMessage", actionText + " tài khoản '" + tk.getTenDangNhap() + "' thành công!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy tài khoản!");
            }
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
