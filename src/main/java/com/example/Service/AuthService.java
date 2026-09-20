package com.example.Service;

import com.example.Model.TaiKhoan;
import com.example.Repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Xac thuc tai khoan dang nhap tu bang TaiKhoan. */
@Service
public class AuthService {

    private final TaiKhoanRepository taiKhoanRepository;

    public AuthService(TaiKhoanRepository taiKhoanRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
    }

    public AuthenticationResult authenticate(String tenDangNhap, String matKhau) {
        if (isBlank(tenDangNhap) || isBlank(matKhau)) {
            return AuthenticationResult.failure("Vui lòng nhập tên đăng nhập và mật khẩu.");
        }

        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap.trim()).orElse(null);
        if (taiKhoan == null || !passwordMatches(matKhau, taiKhoan.getMatKhau())) {
            return AuthenticationResult.failure("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }
        if (!"HoatDong".equals(taiKhoan.getTrangThai())) {
            return AuthenticationResult.failure("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
        }

      return AuthenticationResult.success(taiKhoan, redirectFor(taiKhoan));
    }

    private boolean passwordMatches(String input, String storedPassword) {
        return storedPassword != null && MessageDigest.isEqual(
                input.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    private String redirectFor(TaiKhoan taiKhoan) {
        String loaiTaiKhoan = taiKhoan.getLoaiTaiKhoan();
        String maTK = taiKhoan.getMaTaiKhoan();

        if ("NhanVien".equals(loaiTaiKhoan) && maTK != null) {
            // Giám đốc -> Thống kê báo cáo
            if (maTK.startsWith("TK-GD")) {
                return "/giam-doc/bao-cao"; 
            }
            if (maTK.startsWith("TK-HCNS")) {
                return "/hcns/nhan-vien"; 
            }
            // CSKH -> Quản lý khiếu nại
            if (maTK.startsWith("TK-CSKH")) {
                return "/cskh/khieu-nai"; 
            }
            // Marketing -> Quản lý thông báo
            if (maTK.startsWith("TK-MKT")) {
                return "/marketing/thong-bao"; 
            }
        }
        
    
        
        return "/";
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    public record AuthenticationResult(boolean success, String message, Integer taiKhoanId,
                                       String tenDangNhap, String loaiTaiKhoan, String redirectUrl) {
        static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, message, null, null, null, null);
        }
        static AuthenticationResult success(TaiKhoan taiKhoan, String redirectUrl) {
            return new AuthenticationResult(true, null, taiKhoan.getId(), taiKhoan.getTenDangNhap(),
                    taiKhoan.getLoaiTaiKhoan(), redirectUrl);
        }
    }
}
