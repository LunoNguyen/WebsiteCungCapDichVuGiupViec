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
}
