package com.example.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CalculatePriceRequest {
    @NotNull(message = "dichVuId không được để trống")
    private Integer dichVuId;

    private String loaiHinhDat; // TheoLan | GoiThang
    private Integer bangGiaId;
    private Integer goiDichVuId;
    private String codeKhuyenMai;
}
