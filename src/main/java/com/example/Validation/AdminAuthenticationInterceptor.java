package com.example.Validation;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Bao ve cac duong dan quan tri khoi truy cap truc tiep khi chua dang nhap. */
@Component
public class AdminAuthenticationInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object userId = request.getSession(false) == null ? null : request.getSession(false).getAttribute("authenticatedUserId");
        Object accountType = request.getSession(false) == null ? null : request.getSession(false).getAttribute("accountType");
        if (userId != null && "NhanVien".equals(accountType)) return true;
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
