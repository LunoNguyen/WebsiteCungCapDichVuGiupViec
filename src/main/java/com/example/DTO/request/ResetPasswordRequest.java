package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Quên mật khẩu: OTP (mục đích DatLaiMatKhau) nhận qua SMS + mật khẩu mới. */
@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Số điện thoại không được để trống")
    private String identifier;

    @NotBlank(message = "Mã OTP không được để trống")
    private String maCode;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    private String matKhauMoi;

    /** KhachHang | CongTacVien: số điện thoại có cả hai tài khoản thì chọn tài khoản nào (mặc định khách hàng). */
    private String loaiTaiKhoan;
}
