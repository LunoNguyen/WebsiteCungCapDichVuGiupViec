package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotNull(message = "Thiếu mã tài khoản")
    private Integer taiKhoanId; // nhận từ kết quả /v1/auth/login

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String matKhauHienTai;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    private String matKhauMoi;
}
