package com.example.DTO.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCalculationResult {
    private Integer dichVuId;
    private String tenDichVu;
    private String loaiHinhDat;
    private BigDecimal chiPhiGoc;
    private BigDecimal soTienGiam;
    private BigDecimal thanhTien;
    private String maKhuyenMai;
    private String thongDiepKMDuocApDung;
}
