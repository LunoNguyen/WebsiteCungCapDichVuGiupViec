package com.example.Controller;

import com.example.Service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Landing Page Controller
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
    public String login() {
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
        return "redirect:" + result.redirectUrl();
    }
}
