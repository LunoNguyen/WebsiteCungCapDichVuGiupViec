package com.example.Service;

import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Xac thuc tai khoan va phan quyen tu CSDL.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final NhanVienRepository nhanVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final CongTacVienRepository congTacVienRepository;

    public AuthService(
            TaiKhoanRepository taiKhoanRepository,
            NhanVienRepository nhanVienRepository,
            KhachHangRepository khachHangRepository,
            CongTacVienRepository congTacVienRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.khachHangRepository = khachHangRepository;
        this.congTacVienRepository = congTacVienRepository;
    }

    public AuthenticationResult authenticate(String tenDangNhap, String matKhau) {
        if (isBlank(tenDangNhap) || isBlank(matKhau)) {
            return AuthenticationResult.failure("Vui lòng nhập tên đăng nhập và mật khẩu.");
        }

        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap.trim()).orElse(null);
        if (taiKhoan == null || !passwordMatches(matKhau, taiKhoan.getMatKhau())) {
            return AuthenticationResult.failure("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }
        if (!"HoatDong".equalsIgnoreCase(taiKhoan.getTrangThai())) {
            return AuthenticationResult.failure("Tài khoản chưa được kích hoạt hoặc đã bị khóa.");
        }

        return resolveUserRoleAndRedirect(taiKhoan);
    }

    private AuthenticationResult resolveUserRoleAndRedirect(TaiKhoan taiKhoan) {
        String loaiTK = taiKhoan.getLoaiTaiKhoan();
        String role = "ROLE_USER";
        String redirectUrl = "/";
        String fullName = taiKhoan.getTenDangNhap();
        String avatar = "U";

        if ("NhanVien".equalsIgnoreCase(loaiTK)) {
            NhanVien nv = nhanVienRepository.findByTaiKhoan(taiKhoan)
                    .orElseGet(() -> nhanVienRepository.findByTaiKhoan_Id(taiKhoan.getId()).orElse(null));

            if (nv != null) {
                fullName = nv.getHoTen();
                ChucVu cv = nv.getChucVu();
                String maChucVu = cv != null ? cv.getMaChucVu() : "";
                String maPhongBan = (cv != null && cv.getPhongBan() != null) ? cv.getPhongBan().getMaPhongBan() : "";

                if ("CV-GD".equalsIgnoreCase(maChucVu) || "PB-BGD".equalsIgnoreCase(maPhongBan) || "giamdoc".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_GIAM_DOC";
                    redirectUrl = "/giam-doc/bao-cao";
                    avatar = "GĐ";
                } else if ("PB-HCNS".equalsIgnoreCase(maPhongBan) || "CV-TPHC".equalsIgnoreCase(maChucVu) || "CV-NVNS".equalsIgnoreCase(maChucVu) || "hanhnt".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_HCNS";
                    redirectUrl = "/hcns/nhan-vien";
                    avatar = "HC";
                } else if ("PB-CSKH".equalsIgnoreCase(maPhongBan) || "CV-CSKH".equalsIgnoreCase(maChucVu) || "dunght".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_CSKH";
                    redirectUrl = "/cskh/khieu-nai";
                    avatar = "CS";
                } else if ("PB-MKT".equalsIgnoreCase(maPhongBan) || "CV-MKT".equalsIgnoreCase(maChucVu) || "minhpv".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_MARKETING";
                    redirectUrl = "/marketing/thong-bao";
                    avatar = "MKT";
                } else {
                    role = "ROLE_HCNS";
                    redirectUrl = "/hcns/nhan-vien";
                    avatar = "NV";
                }
            } else {
                if ("giamdoc".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_GIAM_DOC";
                    redirectUrl = "/giam-doc/bao-cao";
                    avatar = "GĐ";
                } else if ("hanhnt".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_HCNS";
                    redirectUrl = "/hcns/nhan-vien";
                    avatar = "HC";
                } else if ("dunght".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_CSKH";
                    redirectUrl = "/cskh/khieu-nai";
                    avatar = "CS";
                } else if ("minhpv".equalsIgnoreCase(taiKhoan.getTenDangNhap())) {
                    role = "ROLE_MARKETING";
                    redirectUrl = "/marketing/thong-bao";
                    avatar = "MKT";
                }
            }
        } else if ("KhachHang".equalsIgnoreCase(loaiTK)) {
            role = "ROLE_KHACH_HANG";
            redirectUrl = "/";
            KhachHang kh = khachHangRepository.findByTaiKhoan(taiKhoan).orElse(null);
            if (kh != null) {
                fullName = kh.getHoTen();
                avatar = "KH";
            }
        } else if ("CongTacVien".equalsIgnoreCase(loaiTK)) {
            role = "ROLE_CTV";
            redirectUrl = "/";
            CongTacVien ctv = congTacVienRepository.findByTaiKhoan(taiKhoan).orElse(null);
            if (ctv != null) {
                fullName = ctv.getHoTen();
                avatar = "CTV";
            }
        }

        return AuthenticationResult.success(taiKhoan, role, fullName, avatar, redirectUrl);
    }

    private boolean passwordMatches(String input, String storedPassword) {
        return storedPassword != null && MessageDigest.isEqual(
                input.getBytes(StandardCharsets.UTF_8), storedPassword.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    public record AuthenticationResult(
            boolean success,
            String message,
            Integer taiKhoanId,
            String tenDangNhap,
            String loaiTaiKhoan,
            String role,
            String fullName,
            String avatar,
            String redirectUrl) {

        public static AuthenticationResult failure(String message) {
            return new AuthenticationResult(false, message, null, null, null, null, null, null, null);
        }

        public static AuthenticationResult success(TaiKhoan taiKhoan, String role, String fullName, String avatar, String redirectUrl) {
            return new AuthenticationResult(
                    true,
                    null,
                    taiKhoan.getId(),
                    taiKhoan.getTenDangNhap(),
                    taiKhoan.getLoaiTaiKhoan(),
                    role,
                    fullName,
                    avatar,
                    redirectUrl);
        }
    }
}