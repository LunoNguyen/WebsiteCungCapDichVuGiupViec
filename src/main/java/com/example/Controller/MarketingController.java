package com.example.Controller;

import com.example.DTO.*;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Marketing module controllers - UC-MKT01 to UC-MKT04
 * Gọi dữ liệu trực tiếp từ CSDL thay vì gán tĩnh.
 */
@Controller
@RequestMapping("/marketing")
public class MarketingController {

    private final ThongKeService thongKeService;
    private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
    private final MaCouponRepository maCouponRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final DanhGiaRepository danhGiaRepository;

    public MarketingController(
            ThongKeService thongKeService,
            ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository,
            MaCouponRepository maCouponRepository,
            ThongBaoRepository thongBaoRepository,
            DanhGiaRepository danhGiaRepository) {
        this.thongKeService = thongKeService;
        this.chuongTrinhKhuyenMaiRepository = chuongTrinhKhuyenMaiRepository;
        this.maCouponRepository = maCouponRepository;
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
        model.addAttribute("coupons", maCouponRepository.findAll());
        return "marketing/dashboard";
    }

    // UC-MKT01 – Quản lý khuyến mãi
    @GetMapping("/khuyen-mai")
    public String khuyenMai(Model model) {
        model.addAttribute("marketingStats", thongKeService.getMarketingStats());
        model.addAttribute("khuyenMais", chuongTrinhKhuyenMaiRepository.findAll());
        model.addAttribute("coupons", maCouponRepository.findAll());
        return "marketing/khuyen-mai";
    }

    // UC-MKT02 – Quản lý thông báo (gửi đến KH/CTV)
    @GetMapping("/thong-bao")
    public String thongBao(Model model) {
        model.addAttribute("thongBaos", thongBaoRepository.findAll());
        return "marketing/thong-bao";
    }

    // UC-MKT03 – Phân tích & báo cáo marketing
    @GetMapping("/phan-tich")
    public String phanTich(Model model) {
        var mktStats = thongKeService.getMarketingStats();
        model.addAttribute("marketingStats", mktStats);
        model.addAttribute("tongKhuyenMai", mktStats.getTongKhuyenMai());
        model.addAttribute("tongCoupons", mktStats.getTongCoupons());
        model.addAttribute("tongKhachHang", thongKeService.getTongKhachHang());
        model.addAttribute("khuyenMais", chuongTrinhKhuyenMaiRepository.findAll());
        model.addAttribute("coupons", maCouponRepository.findAll());
        model.addAttribute("tongDoanhThuTrieu", thongKeService.getDoanhThuTrieuDong());
        return "marketing/phan-tich";
    }

    // UC-MKT04 – Quản lý đánh giá (marketing view)
    @GetMapping("/danh-gia")
    public String danhGia(Model model) {
        model.addAttribute("danhGias", danhGiaRepository.findAll());
        return "marketing/danh-gia";
    }
}

