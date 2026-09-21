package com.example.Controller;

import com.example.DTO.*;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Giám đốc module controllers - UC-GD01 to UC-GD05
 * Gọi dữ liệu trực tiếp từ CSDL thay vì gán tĩnh.
 */
@Controller
@RequestMapping("/giam-doc")
public class GiamDocController {

    private final ThongKeService thongKeService;
    private final DonDatDichVuRepository donDatDichVuRepository;
    private final KhieuNaiRepository khieuNaiRepository;
    private final NhanVienRepository nhanVienRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    private final DichVuRepository dichVuRepository;

    public GiamDocController(
            ThongKeService thongKeService,
            DonDatDichVuRepository donDatDichVuRepository,
            KhieuNaiRepository khieuNaiRepository,
            NhanVienRepository nhanVienRepository,
            ThongBaoRepository thongBaoRepository,
            TaiKhoanRepository taiKhoanRepository,
            DichVuRepository dichVuRepository) {
        this.thongKeService = thongKeService;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.dichVuRepository = dichVuRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var donStats = thongKeService.getDonHangStats();
        var khStats = thongKeService.getKhachHangStats();
        var knStats = thongKeService.getKhieuNaiStats();
        var ctvStats = thongKeService.getCongTacVienStats();
        var nvStats = thongKeService.getNhanVienStats();

        model.addAttribute("donHangStats", donStats);
        model.addAttribute("khachHangStats", khStats);
        model.addAttribute("khieuNaiStats", knStats);
        model.addAttribute("congTacVienStats", ctvStats);
        model.addAttribute("nhanVienStats", nvStats);

        model.addAttribute("tongDonHang", donStats.getTongDonHang());
        model.addAttribute("tongKhachHang", khStats.getTongKhachHang());
        model.addAttribute("tongKhieuNai", knStats.getTongKhieuNai());
        model.addAttribute("khieuNaiLeoThangCount", knStats.getLeoThangCount());
        model.addAttribute("diemDanhGiaTB", thongKeService.getDiemDanhGiaTrungBinh());
        model.addAttribute("doanhThuTrieuDong", donStats.getDoanhThuTrieuDong());
        model.addAttribute("recentOrders", thongKeService.getDonHangGanDay(8));
        model.addAttribute("pendingComplaints", thongKeService.getKhieuNaiGanDay(5));
        model.addAttribute("orderDistribution", thongKeService.getPhanBoTrangThaiDon());
        return "giam-doc/dashboard";
    }

    // UC-GD02 – Thống kê & Báo cáo
    @GetMapping("/bao-cao")
    public String baoCao(Model model) {
        var donStats = thongKeService.getDonHangStats();
        var ctvStats = thongKeService.getCongTacVienStats();
        var khStats = thongKeService.getKhachHangStats();
        var knStats = thongKeService.getKhieuNaiStats();

        model.addAttribute("donHangStats", donStats);
        model.addAttribute("congTacVienStats", ctvStats);
        model.addAttribute("khachHangStats", khStats);
        model.addAttribute("khieuNaiStats", knStats);

        model.addAttribute("tongDonHang", donStats.getTongDonHang());
        model.addAttribute("tongKhachHang", khStats.getTongKhachHang());
        model.addAttribute("tongKhieuNai", knStats.getTongKhieuNai());
        model.addAttribute("tongCongTacVien", ctvStats.getTongCongTacVien());
        model.addAttribute("doanhThuTrieuDong", donStats.getDoanhThuTrieuDong());
        model.addAttribute("recentOrders", thongKeService.getDonHangGanDay(15));
        model.addAttribute("dichVus", dichVuRepository.findAll());
        model.addAttribute("topDichVu", thongKeService.getTopDichVu());
        model.addAttribute("orderDistribution", thongKeService.getPhanBoTrangThaiDon());
        return "giam-doc/bao-cao";
    }

    // UC-GD01 – Xem xét khiếu nại leo thang
    @GetMapping("/khieu-nai")
    public String khieuNai(Model model) {
        model.addAttribute("khieuNaiStats", thongKeService.getKhieuNaiStats());
        model.addAttribute("khieuNais", khieuNaiRepository.findAll());
        return "giam-doc/khieu-nai";
    }

    // UC-GD03 – Xem và phê duyệt thay đổi nhân sự
    @GetMapping("/nhan-vien")
    public String nhanVien(Model model) {
        model.addAttribute("nhanVienStats", thongKeService.getNhanVienStats());
        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        return "giam-doc/nhan-vien";
    }

    // UC-GD04 – Tạo & quản lý thông báo
    @GetMapping("/thong-bao")
    public String thongBao(Model model) {
        model.addAttribute("thongBaos", thongBaoRepository.findAll());
        return "giam-doc/thong-bao";
    }

    // UC-GD05 – Quản lý tài khoản hệ thống
    @GetMapping("/tai-khoan")
    public String taiKhoan(Model model) {
        model.addAttribute("taiKhoans", taiKhoanRepository.findAll());
        return "giam-doc/tai-khoan";
    }
}

