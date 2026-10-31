package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpSendRequest {
    @NotBlank(message = "Số điện thoại hoặc email không được để trống")
    private String identifier; // SoDienThoai hoặc Email

    private String mucDich; // DangKy | DatLaiMatKhau | XacNhanGD (mặc định DangKy)

    /** KhachHang | CongTacVien: số điện thoại có cả hai tài khoản thì chọn tài khoản nào (mặc định khách hàng). */
    private String loaiTaiKhoan;
}
