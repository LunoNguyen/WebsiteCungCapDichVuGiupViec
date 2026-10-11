package com.example.Controller;

import com.example.Model.KhachHang;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.KhachHangRepository;
import com.example.Service.AuthService;
import com.example.Service.CatalogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Landing Page & Authentication Controller
 */
@Controller
public class LandingController {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LandingController.class);

    private final AuthService authService;
    private final CatalogService catalogService;
    private final KhachHangRepository khachHangRepository;
    private final CongTacVienRepository congTacVienRepository;

    public LandingController(AuthService authService, CatalogService catalogService,
                             KhachHangRepository khachHangRepository, CongTacVienRepository congTacVienRepository) {
        this.authService = authService;
        this.catalogService = catalogService;
        this.khachHangRepository = khachHangRepository;
        this.congTacVienRepository = congTacVienRepository;
    }

    /** Trang chủ: dữ liệu lấy từ CatalogService (đã cache Redis). Danh mục cho menu do SiteModelAdvice nạp. */
    @GetMapping("/")
    public String index(Model model) {
        try {
            model.addAttribute("dichVuNoiBat", CatalogService.chonDichVuNoiBat(catalogService.getDanhMuc()));
            model.addAttribute("danhGias", catalogService.getDanhGiaNoiBat());
            model.addAttribute("khuyenMais", catalogService.getKhuyenMaiDangChay());
            model.addAttribute("soLieu", catalogService.getSoLieuCongKhai());
        } catch (Exception e) {
            log.warn("Trang chủ: không tải được dữ liệu từ CSDL: {}", e.getMessage());
        }
        return "index";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String unauthorized,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String redirect,
            @RequestParam(name = "can-dang-nhap", required = false) String canDangNhap,
            @RequestParam(required = false) String registered,
            @RequestParam(required = false) String reset,
            HttpSession session,
            Model model) {
        if (session.getAttribute("khachHangId") != null && redirect != null && laDuongDanNoiBo(redirect)) {
            return "redirect:" + redirect;
        }
        if ("true".equals(unauthorized)) {
            model.addAttribute("warning", "Vui lòng đăng nhập để có quyền truy cập vào hệ thống quản trị.");
        }
        if ("true".equals(canDangNhap)) {
            model.addAttribute("warning", "Vui lòng đăng nhập để tiếp tục.");
        }
        if ("true".equals(logout)) {
            model.addAttribute("info", "Bạn đã đăng xuất.");
        }
        if ("true".equals(registered)) {
            model.addAttribute("info", "Tài khoản đã được kích hoạt. Mời bạn đăng nhập.");
        }
        if ("true".equals(reset)) {
            model.addAttribute("info", "Đã đặt lại mật khẩu. Mời bạn đăng nhập bằng mật khẩu mới.");
        }
        if ("forbidden".equals(error)) {
            model.addAttribute("error", "Tài khoản của bạn không có quyền truy cập vào phân hệ này.");
        }
        if (redirect != null && laDuongDanNoiBo(redirect)) {
            model.addAttribute("redirect", redirect);
        }
        return "auth/login";
    }

    /** Màn báo cộng tác viên phải dùng ứng dụng (sau khi đăng nhập web bằng tài khoản CTV). */
    @GetMapping("/login/cong-tac-vien")
    public String congTacVienDungApp(Model model) {
        model.addAttribute("ctvApp", true);
        return "auth/login";
    }

    @PostMapping("/login")
    public String authenticate(
            @RequestParam String tenDangNhap,
            @RequestParam String matKhau,
            @RequestParam(required = false) String redirect,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        boolean coRedirect = redirect != null && laDuongDanNoiBo(redirect);
        AuthService.AuthenticationResult result = authService.authenticate(tenDangNhap, matKhau);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("error", result.message());
            redirectAttributes.addFlashAttribute("tenDangNhap", tenDangNhap);
            if (coRedirect) redirectAttributes.addAttribute("redirect", redirect);
            return "redirect:/login";
        }

        // Nhân viên: vào phân hệ quản trị như cũ
        if ("NhanVien".equalsIgnoreCase(result.loaiTaiKhoan())) {
            session.setAttribute("authenticatedUserId", result.taiKhoanId());
            session.setAttribute("authenticatedUsername", result.tenDangNhap());
            session.setAttribute("accountType", result.loaiTaiKhoan());
            session.setAttribute("userRole", result.role());
            session.setAttribute("authenticatedFullName", result.fullName());
            session.setAttribute("userAvatar", result.avatar());
            return "redirect:" + result.redirectUrl();
        }

        // Khách hàng (kể cả tài khoản dùng chung với hồ sơ CTV): vào khu khách hàng trên web
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(result.taiKhoanId()).orElse(null);
        if (kh != null) {
            if ("BiKhoa".equalsIgnoreCase(kh.getTrangThai())) {
                redirectAttributes.addFlashAttribute("error", "Tài khoản khách hàng đã bị khóa. Vui lòng liên hệ hỗ trợ.");
                return "redirect:/login";
            }
            session.setAttribute("authenticatedUserId", result.taiKhoanId());
            session.setAttribute("authenticatedUsername", result.tenDangNhap());
            session.setAttribute("accountType", "KhachHang");
            session.setAttribute("userRole", "ROLE_KHACH_HANG");
            session.setAttribute("khachHangId", kh.getId());
            session.setAttribute("authenticatedFullName", kh.getHoTen());
            session.setAttribute("userAvatar", chuCaiDau(kh.getHoTen()));
            return "redirect:" + (coRedirect ? redirect : "/");
        }

        // Cộng tác viên: bắt buộc dùng ứng dụng, không tạo phiên đăng nhập web
        if ("CongTacVien".equalsIgnoreCase(result.loaiTaiKhoan())
                || congTacVienRepository.findByTaiKhoan_Id(result.taiKhoanId()).isPresent()) {
            return "redirect:/login/cong-tac-vien";
        }

        redirectAttributes.addFlashAttribute("error", "Tài khoản này chưa có hồ sơ khách hàng. Vui lòng đăng ký.");
        return "redirect:/login";
    }

    /** Chỉ nhận đường dẫn trong site ("/abc"), chặn chuyển hướng sang trang ngoài ("//evil.com", "/\evil.com"). */
    static boolean laDuongDanNoiBo(String url) {
        return url.startsWith("/") && !url.startsWith("//") && !url.startsWith("/\\");
    }

    /** Hai chữ cái cho avatar: chữ đầu của họ và của tên ("Nguyễn Văn An" → "NA"). */
    static String chuCaiDau(String hoTen) {
        if (hoTen == null || hoTen.isBlank()) return "KH";
        String[] tu = hoTen.trim().split("\\s+");
        String dau = tu[0].substring(0, 1);
        String cuoi = tu.length > 1 ? tu[tu.length - 1].substring(0, 1) : "";
        return (dau + cuoi).toUpperCase();
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }
}

