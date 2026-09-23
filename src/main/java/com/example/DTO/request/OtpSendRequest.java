package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpSendRequest {
    @NotBlank(message = "Số điện thoại hoặc email không được để trống")
    private String identifier; // SoDienThoai hoặc Email

    private String mucDich; // DangKy | DatLaiMatKhau | XacNhanGD (mặc định DangKy)
}
