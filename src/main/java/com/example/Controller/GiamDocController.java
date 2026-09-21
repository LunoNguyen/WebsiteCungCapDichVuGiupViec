package com.example.Controller;

import com.example.DTO.*;
import com.example.Model.ThongBao;
import com.example.Model.TaiKhoan;
import com.example.Model.NhatKyTaiKhoan;
import com.example.Repository.*;
import com.example.Service.ThongKeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    private final NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository; // <--- Khai báo thêm log

    public GiamDocController(
            ThongKeService thongKeService,
            DonDatDichVuRepository donDatDichVuRepository,
            KhieuNaiRepository khieuNaiRepository,
            NhanVienRepository nhanVienRepository,
            ThongBaoRepository thongBaoRepository,
            TaiKhoanRepository taiKhoanRepository,
            DichVuRepository dichVuRepository,
            NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository) { // <--- Thêm vào Constructor
        this.thongKeService = thongKeService;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.dichVuRepository = dichVuRepository;
        this.nhatKyTaiKhoanRepository = nhatKyTaiKhoanRepository;
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

@GetMapping("/bao-cao")
    public String baoCao(@RequestParam(value = "period", defaultValue = "month") String period, Model model) {
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
        model.addAttribute("doanhThuHienTai", donStats.getDoanhThuTrieuDong() != null ? donStats.getDoanhThuTrieuDong() : 0);

        return "giam-doc/bao-cao";
    }

    @GetMapping("/khieu-nai")
    public String khieuNai(Model model) {
        model.addAttribute("khieuNaiStats", thongKeService.getKhieuNaiStats());
        model.addAttribute("khieuNais", khieuNaiRepository.findAll());
        return "giam-doc/khieu-nai";
    }

    @GetMapping("/nhan-vien")
    public String nhanVien(Model model) {
        model.addAttribute("nhanVienStats", thongKeService.getNhanVienStats());
        model.addAttribute("nhanViens", nhanVienRepository.findAll());
        return "giam-doc/nhan-vien";
    }

    @GetMapping("/thong-bao")
    public String thongBao(Model model) {
        List<ThongBao> allThongBao = thongBaoRepository.findAll();
        
        List<ThongBao> dsChoDuyet = allThongBao.stream()
                .filter(tb -> "Chờ duyệt".equalsIgnoreCase(tb.getTrangThai()) || "Nhap".equalsIgnoreCase(tb.getTrangThai()))
                .collect(Collectors.toList());

        List<ThongBao> dsDaGui = allThongBao.stream()
                .filter(tb -> !"Chờ duyệt".equalsIgnoreCase(tb.getTrangThai()) && !"Nhap".equalsIgnoreCase(tb.getTrangThai()))
                .collect(Collectors.toList());

        model.addAttribute("dsChoDuyet", dsChoDuyet);
        model.addAttribute("dsDaGui", dsDaGui);
        return "giam-doc/thong-bao";
    }

    @PostMapping("/thong-bao/duyet")
    public String duyetThongBao(@RequestParam Integer id) {
        thongBaoRepository.findById(id).ifPresent(tb -> {
            tb.setTrangThai("DaGui");
            tb.setThoiGianGui(LocalDateTime.now());
            thongBaoRepository.save(tb);
        });
        return "redirect:/giam-doc/thong-bao";
    }

    @PostMapping("/thong-bao/tu-choi")
    public String tuChoiThongBao(@RequestParam Integer id) {
        thongBaoRepository.findById(id).ifPresent(tb -> {
            tb.setTrangThai("TuChoi");
            thongBaoRepository.save(tb);
        });
        return "redirect:/giam-doc/thong-bao";
    }

    @PostMapping("/thong-bao/tao-moi")
    public String taoMoiThongBao(
            @RequestParam String tieuDe,
            @RequestParam String noiDung,
            @RequestParam String nhomNhan,
            HttpSession session) {
        
        String username = (String) session.getAttribute("authenticatedUsername");
        String tenNguoiGui = "Ban Giám đốc";
        
        if (username != null && !username.isBlank()) {
            var taiKhoanOpt = taiKhoanRepository.findByTenDangNhap(username);
            if (taiKhoanOpt.isPresent()) {
                var nhanVienOpt = nhanVienRepository.findByTaiKhoan(taiKhoanOpt.get());
                if (nhanVienOpt.isPresent()) {
                    tenNguoiGui = nhanVienOpt.get().getHoTen(); 
                } else {
                    tenNguoiGui = taiKhoanOpt.get().getTenDangNhap();
                }
            }
        }
        
        ThongBao tb = new ThongBao();
        tb.setMaThongBao("TB-" + System.currentTimeMillis()); 
        tb.setTieuDe(tieuDe);
        tb.setNoiDung(noiDung);
        tb.setNhomNhan(nhomNhan);
        tb.setNguoiGui(tenNguoiGui);
        tb.setThoiGianGui(LocalDateTime.now());
        tb.setTrangThai("DaGui");

        thongBaoRepository.save(tb);
        return "redirect:/giam-doc/thong-bao";
    }

    // =========================================================================
    // UC-GD05 – Quản lý tài khoản hệ thống (CÓ NHẬT KÝ)
    // =========================================================================
    
    @GetMapping("/tai-khoan")
    public String taiKhoan(Model model) {
        model.addAttribute("taiKhoans", taiKhoanRepository.findAll());
        // Lấy 5 log mới nhất truyền ra giao diện
        model.addAttribute("nhatKys", nhatKyTaiKhoanRepository.findTop5ByOrderByThoiGianDesc());
        return "giam-doc/tai-khoan";
    }

    @PostMapping("/tai-khoan/khoa")
    public String khoaTaiKhoan(@RequestParam Integer id, HttpServletRequest request) {
        taiKhoanRepository.findById(id).ifPresent(tk -> {
            tk.setTrangThai("BiKhoa");
            taiKhoanRepository.save(tk);

            // Ghi log
            NhatKyTaiKhoan log = new NhatKyTaiKhoan();
            log.setTaiKhoan(tk);
            log.setHanhDong("Khóa TK");
            log.setDiaChiIP(request.getRemoteAddr());
            log.setThietBi(request.getHeader("User-Agent"));
            log.setKetQua("ThanhCong");
            nhatKyTaiKhoanRepository.save(log);
        });
        return "redirect:/giam-doc/tai-khoan";
    }

    @PostMapping("/tai-khoan/mo-khoa")
    public String moKhoaTaiKhoan(@RequestParam Integer id, HttpServletRequest request) {
        taiKhoanRepository.findById(id).ifPresent(tk -> {
            tk.setTrangThai("HoatDong");
            taiKhoanRepository.save(tk);

            // Ghi log
            NhatKyTaiKhoan log = new NhatKyTaiKhoan();
            log.setTaiKhoan(tk);
            log.setHanhDong("Mở khóa TK");
            log.setDiaChiIP(request.getRemoteAddr());
            log.setThietBi(request.getHeader("User-Agent"));
            log.setKetQua("ThanhCong");
            nhatKyTaiKhoanRepository.save(log);
        });
        return "redirect:/giam-doc/tai-khoan";
    }
    //xử lý export excel
    @GetMapping("/tai-khoan/export")
    public void exportExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Danh_Sach_Tai_Khoan.xlsx";
        response.setHeader(headerKey, headerValue);

        List<TaiKhoan> danhSachTK = taiKhoanRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tài khoản");
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Mã TK", "Tên Đăng Nhập", "Email", "Số Điện Thoại", "Loại Tài Khoản", "Trạng Thái"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNum = 1;
            for (TaiKhoan tk : danhSachTK) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(tk.getMaTaiKhoan());
                row.createCell(1).setCellValue(tk.getTenDangNhap());
                row.createCell(2).setCellValue(tk.getEmail());
                row.createCell(3).setCellValue(tk.getSoDienThoai());
                row.createCell(4).setCellValue(tk.getLoaiTaiKhoan());
                row.createCell(5).setCellValue(tk.getTrangThai());
            }

            workbook.write(response.getOutputStream());
        }
    }
    // Xử lý phê duyệt khiếu nại
    @PostMapping("/khieu-nai/xu-ly")
    public String xuLyKhieuNai(@RequestParam Integer id, 
                               @RequestParam String phuongAn, 
                               @RequestParam String ghiChu) {
        khieuNaiRepository.findById(id).ifPresent(kn -> {
            // Nếu phương án là bổ sung tài liệu thì chuyển về Đang Xử Lý, ngược lại là Đã Giải Quyết
            if (phuongAn.equals("BoSungTaiLieu")) {
                kn.setTrangThai("DangXuLy");
            } else {
                kn.setTrangThai("DaGiaiQuyet");
                kn.setNgayGiaiQuyet(LocalDateTime.now());
            }
            // Ghi chú hiện tại chưa có cột trong DB KhieuNai, bạn có thể tạo thêm cột GhiChuGiamDoc nếu cần lưu.
            
            khieuNaiRepository.save(kn);
        });
        return "redirect:/giam-doc/khieu-nai";
    }
    // Xuất báo cáo thống kê ra Excel
    @GetMapping("/bao-cao/export-excel")
    public void exportBaoCaoExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Bao_Cao_Thong_Ke.xlsx");

        var donStats = thongKeService.getDonHangStats();
        var khStats = thongKeService.getKhachHangStats();
        var knStats = thongKeService.getKhieuNaiStats();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tong Quan Bao Cao");

            // Tạo tiêu đề báo cáo
            Row row0 = sheet.createRow(0);
            row0.createCell(0).setCellValue("BÁO CÁO HOẠT ĐỘNG KINH DOANH - NEATIFY");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("Chỉ số KPI");
            row2.createCell(1).setCellValue("Giá trị");

            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("Tổng Doanh Thu (Triệu VNĐ)");
            row3.createCell(1).setCellValue(donStats.getDoanhThuTrieuDong() != null ? donStats.getDoanhThuTrieuDong().doubleValue() : 0.0);

            Row row4 = sheet.createRow(4);
            row4.createCell(0).setCellValue("Tổng Đơn Dịch Vụ");
            row4.createCell(1).setCellValue(donStats.getTongDonHang());

            Row row5 = sheet.createRow(5);
            row5.createCell(0).setCellValue("Tổng Khách Hàng");
            row5.createCell(1).setCellValue(khStats.getTongKhachHang());

            Row row6 = sheet.createRow(6);
            row6.createCell(0).setCellValue("Tổng Khiếu Nại");
            row6.createCell(1).setCellValue(knStats.getTongKhieuNai());

            workbook.write(response.getOutputStream());
        }
    }
}