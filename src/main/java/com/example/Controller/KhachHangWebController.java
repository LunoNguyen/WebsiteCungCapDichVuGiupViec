package com.example.Controller;

import com.example.DTO.request.AddressRequest;
import com.example.DTO.request.BookingCreateRequest;
import com.example.DTO.request.CustomerRegisterRequest;
import com.example.DTO.request.OtpSendRequest;
import com.example.DTO.request.OtpVerifyRequest;
import com.example.DTO.request.ResetPasswordRequest;
import com.example.DTO.request.ReviewCreateRequest;
import com.example.DTO.view.DanhMucView;
import com.example.Model.DonDatDichVu;
import com.example.Model.KhachHang;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.KhachHangRepository;
import com.example.Service.AuthService;
import com.example.Service.CatalogService;
import com.example.Service.CustomerAddressService;
import com.example.Service.CustomerApiService;
import com.example.Service.LichGoiThang;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Khu khách hàng trên website: đăng ký (OTP), quên mật khẩu, đặt dịch vụ, đơn của tôi, sổ địa chỉ, tài khoản.
 * Cộng tác viên không dùng khu này (bắt buộc dùng ứng dụng, xem LandingController#authenticate).
 * Logic nghiệp vụ dùng lại CustomerApiService / CustomerAddressService của API ứng dụng.
 */
@Controller
public class KhachHangWebController {

    private static final Logger log = LoggerFactory.getLogger(KhachHangWebController.class);

    private final CustomerApiService customerApiService;
    private final CustomerAddressService addressService;
    private final AuthService authService;
    private final KhachHangRepository khachHangRepository;
    private final DonDatDichVuRepository donDatDichVuRepository;

    public KhachHangWebController(CustomerApiService customerApiService,
                                  CustomerAddressService addressService,
                                  AuthService authService,
                                  KhachHangRepository khachHangRepository,
                                  DonDatDichVuRepository donDatDichVuRepository) {
        this.customerApiService = customerApiService;
        this.addressService = addressService;
        this.authService = authService;
        this.khachHangRepository = khachHangRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
    }

    // =====================================================================
    // ĐĂNG KÝ + OTP
    // =====================================================================

    @GetMapping("/dang-ky")
    public String dangKy(HttpSession session) {
        if (khachHangId(session) != null) return "redirect:/";
        return "khach-hang/dang-ky";
    }

    @PostMapping("/dang-ky")
    public String xuLyDangKy(@RequestParam String hoTen,
                             @RequestParam String soDienThoai,
                             @RequestParam(required = false) String email,
                             @RequestParam String matKhau,
                             @RequestParam String nhapLaiMatKhau,
                             RedirectAttributes ra) {
        ra.addFlashAttribute("hoTen", hoTen);
        ra.addFlashAttribute("soDienThoai", soDienThoai);
        ra.addFlashAttribute("email", email);
        if (!matKhau.equals(nhapLaiMatKhau)) {
            ra.addFlashAttribute("error", "Hai mật khẩu chưa khớp nhau.");
            return "redirect:/dang-ky";
        }
        if (matKhau.trim().length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu cần ít nhất 6 ký tự.");
            return "redirect:/dang-ky";
        }
        try {
            CustomerRegisterRequest req = new CustomerRegisterRequest();
            req.setHoTen(hoTen.trim());
            req.setSoDienThoai(soDienThoai.replaceAll("\\s+", ""));
            req.setEmail(email != null && !email.isBlank() ? email.trim() : null);
            req.setMatKhau(matKhau);
            Map<String, Object> kq = customerApiService.registerCustomer(req);
            if (Boolean.TRUE.equals(kq.get("canXacThucOtp"))) {
                ra.addFlashAttribute("info", kq.get("message"));
                if (kq.get("otpCode") != null) ra.addFlashAttribute("otpThu", kq.get("otpCode"));
                ra.addAttribute("sdt", req.getSoDienThoai());
                return "redirect:/xac-thuc-otp";
            }
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.warn("Đăng ký khách hàng trên web lỗi: {}", e.getMessage());
            ra.addFlashAttribute("error", "Chưa đăng ký được. Vui lòng thử lại sau ít phút.");
        }
        return "redirect:/dang-ky";
    }

    @GetMapping("/xac-thuc-otp")
    public String xacThucOtp(@RequestParam(required = false) String sdt, Model model) {
        if (sdt == null || sdt.isBlank()) return "redirect:/dang-ky";
        model.addAttribute("sdt", sdt);
        model.addAttribute("mucDich", "DangKy");
        return "khach-hang/xac-thuc-otp";
    }

    @PostMapping("/xac-thuc-otp")
    public String xuLyXacThucOtp(@RequestParam String sdt, @RequestParam String maCode, RedirectAttributes ra) {
        try {
            OtpVerifyRequest req = new OtpVerifyRequest();
            req.setIdentifier(sdt);
            req.setMaCode(maCode.trim());
            req.setMucDich("DangKy");
            customerApiService.verifyOtp(req);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chưa xác thực được mã. Vui lòng thử lại.");
        }
        ra.addAttribute("sdt", sdt);
        return "redirect:/xac-thuc-otp";
    }

    @PostMapping("/xac-thuc-otp/gui-lai")
    public String guiLaiOtp(@RequestParam String sdt, @RequestParam(defaultValue = "DangKy") String mucDich, RedirectAttributes ra) {
        boolean datLai = "DatLaiMatKhau".equals(mucDich);
        try {
            OtpSendRequest req = new OtpSendRequest();
            req.setIdentifier(sdt);
            req.setMucDich(datLai ? "DatLaiMatKhau" : "DangKy");
            Map<String, Object> kq = customerApiService.sendOtp(req);
            ra.addFlashAttribute("info", kq.get("message"));
            if (kq.get("otpCode") != null) ra.addFlashAttribute("otpThu", kq.get("otpCode"));
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chưa gửi được mã. Vui lòng thử lại sau ít phút.");
        }
        ra.addAttribute("sdt", sdt);
        return datLai ? "redirect:/dat-lai-mat-khau" : "redirect:/xac-thuc-otp";
    }

    // =====================================================================
    // QUÊN MẬT KHẨU
    // =====================================================================

    @GetMapping("/quen-mat-khau")
    public String quenMatKhau() {
        return "khach-hang/quen-mat-khau";
    }

    @PostMapping("/quen-mat-khau")
    public String guiOtpDatLai(@RequestParam String soDienThoai, RedirectAttributes ra) {
        String sdt = soDienThoai.replaceAll("\\s+", "");
        try {
            OtpSendRequest req = new OtpSendRequest();
            req.setIdentifier(sdt);
            req.setMucDich("DatLaiMatKhau");
            Map<String, Object> kq = customerApiService.sendOtp(req);
            ra.addFlashAttribute("info", kq.get("message"));
            if (kq.get("otpCode") != null) ra.addFlashAttribute("otpThu", kq.get("otpCode"));
            ra.addAttribute("sdt", sdt);
            return "redirect:/dat-lai-mat-khau";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chưa gửi được mã. Vui lòng thử lại sau ít phút.");
        }
        ra.addFlashAttribute("soDienThoai", soDienThoai);
        return "redirect:/quen-mat-khau";
    }

    @GetMapping("/dat-lai-mat-khau")
    public String datLaiMatKhau(@RequestParam(required = false) String sdt, Model model) {
        if (sdt == null || sdt.isBlank()) return "redirect:/quen-mat-khau";
        model.addAttribute("sdt", sdt);
        model.addAttribute("mucDich", "DatLaiMatKhau");
        return "khach-hang/xac-thuc-otp";
    }

    @PostMapping("/dat-lai-mat-khau")
    public String xuLyDatLai(@RequestParam String sdt, @RequestParam String maCode,
                             @RequestParam String matKhauMoi, RedirectAttributes ra) {
        try {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setIdentifier(sdt);
            req.setMaCode(maCode.trim());
            req.setMatKhauMoi(matKhauMoi);
            customerApiService.resetPassword(req);
            return "redirect:/login?reset=true";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chưa đặt lại được mật khẩu. Vui lòng thử lại.");
        }
        ra.addAttribute("sdt", sdt);
        return "redirect:/dat-lai-mat-khau";
    }

    // =====================================================================
    // ĐẶT DỊCH VỤ
    // =====================================================================

    @GetMapping("/dat-dich-vu/{danhMucId}")
    @SuppressWarnings("unchecked")
    public String datDichVu(@PathVariable Integer danhMucId,
                            @RequestParam(required = false) Integer dv,
                            HttpSession session, HttpServletRequest request, Model model) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);

        List<DanhMucView> danhMucs = (List<DanhMucView>) model.getAttribute("danhMucs");
        DanhMucView danhMuc = CatalogService.timDanhMuc(danhMucs, danhMucId);
        if (danhMuc == null) return "redirect:/dich-vu";
        model.addAttribute("danhMuc", danhMuc);
        model.addAttribute("dvChon", dv);
        Map<Integer, Integer> soBuoiGoi = new java.util.HashMap<>();
        danhMuc.getDichVuGoiThang().forEach(g -> soBuoiGoi.put(g.getId(), LichGoiThang.soBuoi(g.getSoBuoi(), g.getTenDichVu())));
        model.addAttribute("soBuoiGoi", soBuoiGoi);

        try {
            model.addAttribute("diaChis", addressService.danhSach(khId));
        } catch (Exception e) {
            log.warn("Trang đặt dịch vụ: không tải được địa chỉ: {}", e.getMessage());
            model.addAttribute("diaChis", List.of());
            model.addAttribute("loiDiaChi", true);
        }
        model.addAttribute("khuVucs", taiKhuVuc());

        // Bảy ngày tới, kể từ hôm nay
        List<LocalDate> ngays = new ArrayList<>();
        for (int i = 0; i < 7; i++) ngays.add(LocalDate.now().plusDays(i));
        model.addAttribute("ngays", ngays);
        model.addAttribute("gios", List.of("07:00", "08:00", "09:00", "10:00", "13:00", "14:00", "15:00", "16:00", "17:00"));
        return "khach-hang/dat-dich-vu";
    }

    @PostMapping("/dat-dich-vu/{danhMucId}")
    public String xuLyDatDichVu(@PathVariable Integer danhMucId,
                                @RequestParam Integer dichVuId,
                                @RequestParam(defaultValue = "TheoLan") String loaiHinhDat,
                                @RequestParam(required = false) Integer diaChiId,
                                @RequestParam(required = false) String diaChiChiTiet,
                                @RequestParam(required = false) Integer khuVucId,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayThucHien,
                                @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime gioBatDau,
                                @RequestParam(required = false) List<String> ngayTrongTuan,
                                @RequestParam(required = false) String codeKhuyenMai,
                                @RequestParam(required = false) String yeuCauDacBiet,
                                HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        String quayLai = "redirect:/dat-dich-vu/" + danhMucId + "?dv=" + dichVuId;

        if (ngayThucHien == null || gioBatDau == null) {
            ra.addFlashAttribute("error", "Vui lòng chọn ngày và giờ bắt đầu.");
            return quayLai;
        }
        if (LocalDateTime.of(ngayThucHien, gioBatDau).isBefore(LocalDateTime.now().plusHours(1))) {
            ra.addFlashAttribute("error", "Giờ bắt đầu cần sau thời điểm hiện tại ít nhất 1 giờ.");
            return quayLai;
        }
        boolean goiThang = "GoiThang".equals(loaiHinhDat);
        if (goiThang && (ngayTrongTuan == null || ngayTrongTuan.isEmpty())) {
            ra.addFlashAttribute("error", "Gói tháng cần chọn ít nhất một ngày trong tuần.");
            return quayLai;
        }
        try {
            BookingCreateRequest req = new BookingCreateRequest();
            req.setKhachHangId(khId);
            req.setDichVuId(dichVuId);
            req.setLoaiHinhDat(goiThang ? "GoiThang" : "TheoLan");
            req.setDiaChiId(diaChiId);
            if (diaChiId == null && diaChiChiTiet != null && !diaChiChiTiet.isBlank()) {
                req.setDiaChiChiTiet(diaChiChiTiet.trim());
                req.setKhuVucId(khuVucId);
            }
            req.setNgayThucHien(ngayThucHien);
            req.setGioBatDau(gioBatDau);
            if (goiThang) req.setNgayThucHienTrongTuan(String.join(",", ngayTrongTuan));
            if (codeKhuyenMai != null && !codeKhuyenMai.isBlank()) req.setCodeKhuyenMai(codeKhuyenMai.trim().toUpperCase());
            if (yeuCauDacBiet != null && !yeuCauDacBiet.isBlank()) req.setYeuCauDacBiet(yeuCauDacBiet.trim());
            Map<String, Object> kq = customerApiService.createBooking(req);
            Object id = kq.get("donDatId") != null ? kq.get("donDatId") : kq.get("id");
            ra.addFlashAttribute("success", "Đã đặt dịch vụ. bTaskee đang tìm cộng tác viên cho bạn.");
            return id != null ? "redirect:/tai-khoan/don-hang/" + id : "redirect:/tai-khoan/don-hang";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.warn("Đặt dịch vụ trên web lỗi: {}", e.getMessage());
            ra.addFlashAttribute("error", "Chưa đặt được dịch vụ. Vui lòng thử lại sau ít phút.");
        }
        return quayLai;
    }

    // =====================================================================
    // ĐƠN CỦA TÔI
    // =====================================================================

    @GetMapping("/tai-khoan/don-hang")
    public String donHang(@RequestParam(defaultValue = "sap-toi") String tab,
                          HttpSession session, HttpServletRequest request, Model model) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);

        List<Map<String, Object>> tatCa;
        try {
            tatCa = customerApiService.getCustomerBookings(khId, null);
        } catch (Exception e) {
            log.warn("Đơn của tôi: không tải được: {}", e.getMessage());
            tatCa = List.of();
            model.addAttribute("loiTai", true);
        }
        int sapToi = 0, daXong = 0, daHuy = 0;
        List<Map<String, Object>> donTheoTab = new ArrayList<>();
        for (Map<String, Object> d : tatCa) {
            String tt = String.valueOf(d.get("trangThai"));
            String nhom = "HoanThanh".equals(tt) ? "da-xong" : "DaHuy".equals(tt) ? "da-huy" : "sap-toi";
            switch (nhom) {
                case "da-xong" -> daXong++;
                case "da-huy" -> daHuy++;
                default -> sapToi++;
            }
            if (nhom.equals(tab)) {
                Map<String, Object> m = new LinkedHashMap<>(d);
                ganTrangThai(m);
                donTheoTab.add(m);
            }
        }
        model.addAttribute("tab", tab);
        model.addAttribute("dons", donTheoTab);
        model.addAttribute("soSapToi", sapToi);
        model.addAttribute("soDaXong", daXong);
        model.addAttribute("soDaHuy", daHuy);
        model.addAttribute("active", "don-hang");
        return "khach-hang/don-hang";
    }

    @GetMapping("/tai-khoan/don-hang/{id}")
    public String chiTietDon(@PathVariable Integer id, HttpSession session, HttpServletRequest request, Model model) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        if (!laDonCuaKhach(id, khId)) return "redirect:/tai-khoan/don-hang";

        Map<String, Object> don = new LinkedHashMap<>(customerApiService.getBookingDetail(id));
        ganTrangThai(don);
        model.addAttribute("don", don);
        model.addAttribute("active", "don-hang");
        return "khach-hang/don-chi-tiet";
    }

    @PostMapping("/tai-khoan/don-hang/{id}/huy")
    public String huyDon(@PathVariable Integer id, @RequestParam(required = false) String lyDo,
                         HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        if (!laDonCuaKhach(id, khId)) return "redirect:/tai-khoan/don-hang";
        try {
            customerApiService.cancelBooking(id, khId, lyDo != null && !lyDo.isBlank() ? lyDo.trim() : "Khách hủy trên website");
            ra.addFlashAttribute("success", "Đã hủy đơn.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tai-khoan/don-hang/" + id;
    }

    @PostMapping("/tai-khoan/don-hang/{id}/danh-gia")
    public String danhGia(@PathVariable Integer id,
                          @RequestParam(required = false) Integer diemChatLuong,
                          @RequestParam(required = false) Integer diemThaiDo,
                          @RequestParam(required = false) String nhanXet,
                          HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        if (!laDonCuaKhach(id, khId)) return "redirect:/tai-khoan/don-hang";
        if (diemChatLuong == null || diemThaiDo == null) {
            ra.addFlashAttribute("error", "Vui lòng chấm đủ hai mục: chất lượng và thái độ.");
            return "redirect:/tai-khoan/don-hang/" + id;
        }
        try {
            ReviewCreateRequest req = new ReviewCreateRequest();
            req.setDonDatId(id);
            req.setKhachHangId(khId);
            req.setDiemChatLuong(diemChatLuong);
            req.setDiemThaiDo(diemThaiDo);
            req.setNhanXet(nhanXet != null ? nhanXet.trim() : null);
            customerApiService.createReview(req);
            ra.addFlashAttribute("success", "Cảm ơn bạn đã đánh giá.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tai-khoan/don-hang/" + id;
    }

    // =====================================================================
    // SỔ ĐỊA CHỈ
    // =====================================================================

    @GetMapping("/tai-khoan/dia-chi")
    public String diaChi(@RequestParam(name = "quay-lai", required = false) String quayLai,
                         HttpSession session, HttpServletRequest request, Model model) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        try {
            model.addAttribute("diaChis", addressService.danhSach(khId));
        } catch (Exception e) {
            model.addAttribute("diaChis", List.of());
            model.addAttribute("loiTai", true);
        }
        model.addAttribute("khuVucs", taiKhuVuc());
        if (quayLai != null && LandingController.laDuongDanNoiBo(quayLai)) model.addAttribute("quayLai", quayLai);
        model.addAttribute("active", "dia-chi");
        return "khach-hang/dia-chi";
    }

    @PostMapping("/tai-khoan/dia-chi")
    public String themDiaChi(@RequestParam String diaChiChiTiet,
                             @RequestParam(required = false) Integer khuVucId,
                             @RequestParam(required = false) BigDecimal dienTichNha,
                             @RequestParam(required = false) String luuYDacBiet,
                             @RequestParam(required = false) Boolean laMacDinh,
                             @RequestParam(name = "quay-lai", required = false) String quayLai,
                             HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        try {
            AddressRequest req = new AddressRequest();
            req.setDiaChiChiTiet(diaChiChiTiet.trim());
            req.setKhuVucId(khuVucId);
            req.setDienTichNha(dienTichNha);
            req.setLuuYDacBiet(luuYDacBiet);
            req.setLaMacDinh(Boolean.TRUE.equals(laMacDinh));
            addressService.them(khId, req);
            ra.addFlashAttribute("success", "Đã thêm địa chỉ.");
            if (quayLai != null && LandingController.laDuongDanNoiBo(quayLai)) return "redirect:" + quayLai;
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tai-khoan/dia-chi";
    }

    @PostMapping("/tai-khoan/dia-chi/{id}/mac-dinh")
    public String chonMacDinh(@PathVariable Integer id, HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        try {
            addressService.chonMacDinh(khId, id);
            ra.addFlashAttribute("success", "Đã đặt làm địa chỉ mặc định.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tai-khoan/dia-chi";
    }

    @PostMapping("/tai-khoan/dia-chi/{id}/xoa")
    public String xoaDiaChi(@PathVariable Integer id, HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        try {
            addressService.xoa(khId, id);
            ra.addFlashAttribute("success", "Đã xóa địa chỉ.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tai-khoan/dia-chi";
    }

    // =====================================================================
    // TÀI KHOẢN
    // =====================================================================

    @GetMapping("/tai-khoan")
    public String taiKhoan(HttpSession session, HttpServletRequest request, Model model) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        KhachHang kh = khachHangRepository.findById(khId).orElse(null);
        if (kh == null) {
            session.invalidate();
            return "redirect:/login";
        }
        model.addAttribute("kh", kh);
        model.addAttribute("active", "tai-khoan");
        return "khach-hang/tai-khoan";
    }

    @PostMapping("/tai-khoan/doi-mat-khau")
    public String doiMatKhau(@RequestParam String matKhauHienTai, @RequestParam String matKhauMoi,
                             @RequestParam String nhapLaiMatKhau,
                             HttpSession session, HttpServletRequest request, RedirectAttributes ra) {
        Integer khId = khachHangId(session);
        if (khId == null) return chuyenDangNhap(request);
        if (!matKhauMoi.equals(nhapLaiMatKhau)) {
            ra.addFlashAttribute("error", "Hai mật khẩu mới chưa khớp nhau.");
            return "redirect:/tai-khoan";
        }
        try {
            authService.doiMatKhau((Integer) session.getAttribute("authenticatedUserId"), matKhauHienTai, matKhauMoi);
            ra.addFlashAttribute("success", "Đã đổi mật khẩu.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tai-khoan";
    }

    // =====================================================================
    // HỖ TRỢ
    // =====================================================================

    private static Integer khachHangId(HttpSession session) {
        Object id = session.getAttribute("khachHangId");
        return id instanceof Integer i ? i : null;
    }

    /** Chưa đăng nhập: về màn đăng nhập, đăng nhập xong quay lại đúng trang đang mở. */
    private static String chuyenDangNhap(HttpServletRequest request) {
        String duongDan = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod()) && request.getQueryString() != null) {
            duongDan += "?" + request.getQueryString();
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())) duongDan = "/tai-khoan/don-hang";
        return "redirect:/login?can-dang-nhap=true&redirect=" + URLEncoder.encode(duongDan, StandardCharsets.UTF_8);
    }

    private boolean laDonCuaKhach(Integer donId, Integer khId) {
        return donDatDichVuRepository.findById(donId)
                .map(DonDatDichVu::getKhachHang)
                .map(kh -> khId.equals(kh.getId()))
                .orElse(false);
    }

    private List<?> taiKhuVuc() {
        try {
            return customerApiService.getAreas();
        } catch (Exception e) {
            log.warn("Không tải được khu vực: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Gắn nhãn và tông màu trạng thái cho đơn. "Quá giờ hẹn mà chưa có người nhận" không lưu trong CSDL:
     * tính từ ngày giờ hẹn so với bây giờ, để khách thấy ngay đơn cần gọi hỗ trợ.
     */
    static void ganTrangThai(Map<String, Object> d) {
        String tt = String.valueOf(d.get("trangThai"));
        String nhan;
        String tone;
        switch (tt) {
            case "ChoDuyet", "DangTimCTV" -> { nhan = "Đang tìm người làm"; tone = "primary"; }
            case "DaXacNhan", "DaPhanCong" -> { nhan = "Đã có người nhận"; tone = "info"; }
            case "DangThucHien" -> { nhan = "Đang làm"; tone = "success"; }
            case "HoanThanh" -> { nhan = "Hoàn thành"; tone = "neutral"; }
            case "DaHuy" -> { nhan = "Đã hủy"; tone = "danger"; }
            default -> { nhan = tt; tone = "neutral"; }
        }
        if (("ChoDuyet".equals(tt) || "DangTimCTV".equals(tt))
                && d.get("ngayThucHien") instanceof LocalDate ngay && d.get("gioBatDau") instanceof LocalTime gio
                && LocalDateTime.of(ngay, gio).isBefore(LocalDateTime.now())) {
            nhan = "Quá giờ, chưa có người nhận";
            tone = "warning";
        }
        d.put("nhanTrangThai", nhan);
        d.put("toneTrangThai", tone);
    }
}
