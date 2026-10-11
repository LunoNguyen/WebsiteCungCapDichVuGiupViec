package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String tenDangNhap;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String matKhau;

    /**
     * Vai trò muốn đăng nhập: KhachHang | CongTacVien. Một số điện thoại có thể vừa là khách hàng
     * vừa là cộng tác viên (dùng chung một tài khoản), nên app gửi vai trò theo tab đang chọn.
     * Bỏ trống: theo loại tài khoản như trước.
     */
    private String vaiTro;
}
