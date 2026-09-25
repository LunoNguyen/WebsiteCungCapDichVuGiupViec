package com.example.Controller;

import com.example.DTO.*;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    private final PhanHoiDanhGiaRepository phanHoiDanhGiaRepository;
    private final DichVuRepository dichVuRepository;

    public CSKHController(
            ThongKeService thongKeService,
            KhachHangRepository khachHangRepository,
            DonDatDichVuRepository donDatDichVuRepository,
            KhieuNaiRepository khieuNaiRepository,
            DanhGiaRepository danhGiaRepository,
            ThongBaoRepository thongBaoRepository,
            CongTacVienRepository congTacVienRepository,
            LichLamViecRepository lichLamViecRepository,
            PhanHoiDanhGiaRepository phanHoiDanhGiaRepository,
            DichVuRepository dichVuRepository) {
        this.thongKeService = thongKeService;
        this.khachHangRepository = khachHangRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.phanHoiDanhGiaRepository = phanHoiDanhGiaRepository;
        this.dichVuRepository = dichVuRepository;
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
        model.addAttribute("khachHangs",     khachHangRepository.findAll());
        return "cskh/khach-hang";
    }

    // ── UC-CSKH02 – Quản lý đơn đặt dịch vụ (CRUD) ─────────────────
    @GetMapping("/don-dat-dich-vu")
    public String donDatDichVu(Model model) {
        model.addAttribute("donHangStats",   thongKeService.getDonHangStats());
        model.addAttribute("donDatDichVus",  donDatDichVuRepository.findAll());
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
        model.addAttribute("donDatDichVus",     donDatDichVuRepository.findAll());
        model.addAttribute("congTacViens",      congTacVienRepository.findAll());
        model.addAttribute("lichLamViecs",      lichLamViecRepository.findAll());
        model.addAttribute("congTacVienStats",  thongKeService.getCongTacVienStats());
        model.addAttribute("donHangStats",      thongKeService.getDonHangStats());
        return "cskh/lich-phan-cong";
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
