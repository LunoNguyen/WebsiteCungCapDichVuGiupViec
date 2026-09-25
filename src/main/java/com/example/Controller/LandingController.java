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
    private final com.example.Service.CustomerApiService customerApiService;

    public LandingController(AuthService authService, com.example.Service.CustomerApiService customerApiService) {
        this.authService = authService;
        this.customerApiService = customerApiService;
    }

    @GetMapping("/")
    public String index(Model model) {
        try {
            java.util.List<java.util.Map<String, Object>> allServices = customerApiService.getServices(null, null, null, null, null, null);
            if (allServices != null && !allServices.isEmpty()) {
                // Chọn 6 dịch vụ đại diện cho 6 mảng dịch vụ hoàn toàn khác nhau để trực quan, dễ lấy ảnh minh họa
                java.util.List<String> targetCodes = java.util.List.of("DV-001", "DV-051", "DV-123", "DV-072", "DV-079", "DV-129");
                java.util.List<java.util.Map<String, Object>> distinctServices = new java.util.ArrayList<>();
                for (String code : targetCodes) {
                    allServices.stream()
                            .filter(s -> code.equalsIgnoreCase((String) s.get("maDichVu")))
                            .findFirst()
                            .ifPresent(distinctServices::add);
                }
                // Nếu chưa đủ 6 dịch vụ thì lấy bù từ danh sách còn lại
                if (distinctServices.size() < 6) {
                    for (java.util.Map<String, Object> s : allServices) {
                        if (!distinctServices.contains(s) && distinctServices.size() < 6) {
                            distinctServices.add(s);
                        }
                    }
                }
                model.addAttribute("featuredServices", distinctServices);
            }
            model.addAttribute("categories", customerApiService.getServiceTypes());
        } catch (Exception e) {
            // Log fallback
        }
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

