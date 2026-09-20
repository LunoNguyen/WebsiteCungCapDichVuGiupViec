package com.example.Validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Bao ve cac duong dan quan tri khoi truy cap truc tiep khi chua dang nhap
 * hoac khi khong dung role phan quyen.
 */
@Component
public class AdminAuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }
        }

        HttpSession session = request.getSession(false);
        String userRole = (session != null) ? (String) session.getAttribute("userRole") : null;

        boolean isGiamDocPath = path.startsWith("/giam-doc");
        boolean isHCNSPath = path.startsWith("/hcns");
        boolean isCSKHPath = path.startsWith("/cskh");
        boolean isMarketingPath = path.startsWith("/marketing");

        if (isGiamDocPath || isHCNSPath || isCSKHPath || isMarketingPath) {
            // 1. Chưa đăng nhập -> Chuyển hướng về trang đăng nhập
            if (userRole == null || userRole.isBlank()) {
                String encodedUri = URLEncoder.encode(path, StandardCharsets.UTF_8);
                response.sendRedirect(request.getContextPath() + "/login?unauthorized=true&redirect=" + encodedUri);
                return false;
            }

            // 2. Kiểm tra quyền hạn nghiêm ngặt: Mỗi role chỉ được vào phân hệ của mình, tuyệt đối không được truy cập chéo
            if (isGiamDocPath && !"ROLE_GIAM_DOC".equals(userRole)) {
                response.sendRedirect(request.getContextPath() + getDashboardForRole(userRole) + "?error=forbidden");
                return false;
            }

            if (isHCNSPath && !"ROLE_HCNS".equals(userRole)) {
                response.sendRedirect(request.getContextPath() + getDashboardForRole(userRole) + "?error=forbidden");
                return false;
            }

            if (isCSKHPath && !"ROLE_CSKH".equals(userRole)) {
                response.sendRedirect(request.getContextPath() + getDashboardForRole(userRole) + "?error=forbidden");
                return false;
            }

            if (isMarketingPath && !"ROLE_MARKETING".equals(userRole)) {
                response.sendRedirect(request.getContextPath() + getDashboardForRole(userRole) + "?error=forbidden");
                return false;
            }
        }

        return true;
    }

    private String getDashboardForRole(String role) {
        if ("ROLE_GIAM_DOC".equals(role)) return "/giam-doc/dashboard";
        if ("ROLE_HCNS".equals(role)) return "/hcns/dashboard";
        if ("ROLE_CSKH".equals(role)) return "/cskh/dashboard";
        if ("ROLE_MARKETING".equals(role)) return "/marketing/dashboard";
        return "/";
    }
}

