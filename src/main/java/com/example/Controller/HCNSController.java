package com.example.Controller;

import com.example.Repository.*;
import com.example.Service.ThongKeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HCNS-Kế toán module controllers - UC-HCNS01 to UC-HCNS05
 * Gọi dữ liệu trực tiếp từ CSDL thay vì gán tĩnh.
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

    public HCNSController(
            ThongKeService thongKeService,
            NhanVienRepository nhanVienRepository,
            CongTacVienRepository congTacVienRepository,
            LoaiDichVuRepository loaiDichVuRepository,
            DichVuRepository dichVuRepository,
            BangGiaDichVuRepository bangGiaDichVuRepository,
            TaiKhoanRepository taiKhoanRepository) {
        this.thongKeService = thongKeService;
        this.nhanVienRepository = nhanVienRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.loaiDichVuRepository = loaiDichVuRepository;
        this.dichVuRepository = dichVuRepository;
        this.bangGiaDichVuRepository = bangGiaDichVuRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long tongNV = thongKeService.getTongNhanVien();
        long tongCTV = thongKeService.getTongCongTacVien();
        long tongDV = thongKeService.getTongDichVu();
        var ctvs = congTacVienRepository.findAll();
        var dvs = dichVuRepository.findAll();

        long ctvChoDuyet = ctvs.stream().filter(c -> "ChoDuyet".equalsIgnoreCase(c.getTrangThai())).count();
        long ctvMoi = ctvs.stream().filter(c -> "Moi".equalsIgnoreCase(c.getCapDo())).count();
        long ctvThuong = ctvs.stream().filter(c -> "Thuong".equalsIgnoreCase(c.getCapDo())).count();
        long ctvUuTu = ctvs.stream().filter(c -> "UuTu".equalsIgnoreCase(c.getCapDo())).count();

        model.addAttribute("tongNhanVien", tongNV);
        model.addAttribute("tongCongTacVien", tongCTV);
        model.addAttribute("tongDichVu", tongDV);
        model.addAttribute("ctvChoDuyetCount", ctvChoDuyet);
        model.addAttribute("ctvMoiCount", ctvMoi);
        model.addAttribute("ctvThuongCount", ctvThuong);
        model.addAttribute("ctvUuTuCount", ctvUuTu);
        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        model.addAttribute("congTacViens", ctvs);
        model.addAttribute("dichVus", dvs);
        return "hcns/dashboard";
    }

    // UC-HCNS01 – Quản lý nhân viên
    @GetMapping("/nhan-vien")
    public String nhanVien(Model model) {
        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        return "hcns/nhan-vien";
    }

    // UC-HCNS02 – Quản lý CTV
    @GetMapping("/cong-tac-vien")
    public String congTacVien(Model model) {
        model.addAttribute("congTacViens", congTacVienRepository.findAll());
        return "hcns/cong-tac-vien";
    }

    // UC-HCNS03 – Quản lý danh mục dịch vụ
    @GetMapping("/danh-muc-dich-vu")
    public String danhMucDichVu(Model model) {
        model.addAttribute("loaiDichVus", loaiDichVuRepository.findAll());
        return "hcns/danh-muc-dich-vu";
    }

    // UC-HCNS04 – Quản lý dịch vụ và bảng giá
    @GetMapping("/dich-vu-bang-gia")
    public String dichVuBangGia(Model model) {
        model.addAttribute("dichVus", dichVuRepository.findAll());
        model.addAttribute("bangGias", bangGiaDichVuRepository.findAll());
        return "hcns/dich-vu-bang-gia";
    }

    // UC-HCNS05 – Quản lý tài khoản
    @GetMapping("/tai-khoan")
    public String taiKhoan(Model model) {
        model.addAttribute("taiKhoans", taiKhoanRepository.findAll());
        return "hcns/tai-khoan";
    }
}

