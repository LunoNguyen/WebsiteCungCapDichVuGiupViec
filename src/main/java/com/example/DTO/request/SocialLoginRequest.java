package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SocialLoginRequest {
    @NotBlank(message = "Nhà cung cấp không được để trống (Google/Facebook)")
    private String provider; // Google | Facebook

    @NotBlank(message = "Provider User ID / Token không được để trống")
    private String providerId;

    private String email;
    private String hoTen;
    private String soDienThoai;
    private String avatarUrl;
}
