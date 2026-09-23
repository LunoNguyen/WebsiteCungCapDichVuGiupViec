package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ValidatePromotionRequest {
    @NotBlank(message = "codeKhuyenMai không được để trống")
    private String codeKhuyenMai;

    @NotNull(message = "tongTienDonHang không được để trống")
    private BigDecimal tongTienDonHang;
}
