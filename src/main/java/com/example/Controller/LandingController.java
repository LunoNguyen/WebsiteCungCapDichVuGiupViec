package com.example.Controller;

import com.example.Service.AuthService;
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

    private final AuthService authService;

    public LandingController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login(
            @RequestParam(required = false) String unauthorized,
            @RequestParam(required = false) String logout,
            @RequestParam(required = false) String error,
            Model model) {
        if ("true".equals(unauthorized)) {
            model.addAttribute("warning", "Vui lòng đăng nhập để có quyền truy cập vào hệ thống quản trị.");
        }
        if ("true".equals(logout)) {
            model.addAttribute("info", "Bạn đã đăng xuất khỏi hệ thống thành công.");
        }
        if ("forbidden".equals(error)) {
            model.addAttribute("error", "Tài khoản của bạn không có quyền truy cập vào phân hệ này.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String authenticate(
            @RequestParam String tenDangNhap,
            @RequestParam String matKhau,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        AuthService.AuthenticationResult result = authService.authenticate(tenDangNhap, matKhau);
        if (!result.success()) {
            redirectAttributes.addFlashAttribute("error", result.message());
            redirectAttributes.addFlashAttribute("tenDangNhap", tenDangNhap);
            return "redirect:/login";
        }

        session.setAttribute("authenticatedUserId", result.taiKhoanId());
        session.setAttribute("authenticatedUsername", result.tenDangNhap());
        session.setAttribute("accountType", result.loaiTaiKhoan());
        session.setAttribute("userRole", result.role());
        session.setAttribute("authenticatedFullName", result.fullName());
        session.setAttribute("userAvatar", result.avatar());

        return "redirect:" + result.redirectUrl();
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }
}

