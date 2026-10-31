package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyRequest {
    @NotBlank(message = "Số điện thoại hoặc email không được để trống")
    private String identifier; // SoDienThoai hoặc Email hoặc tenDangNhap

    @NotBlank(message = "Mã OTP không được để trống")
    private String maCode;

    private String mucDich; // DangKy | DatLaiMatKhau | XacNhanGD (mặc định DangKy)

    /** KhachHang | CongTacVien: số điện thoại có cả hai tài khoản thì chọn tài khoản nào (mặc định khách hàng). */
    private String loaiTaiKhoan;
}
