package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO tong hop so lieu thong ke Don dat dich vu tu CSDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonHangStatsDto {
    private long tongDonHang;
    private long choDuyetCount;
    private long dangThucHienCount;
    private long hoanThanhCount;
    private long daHuyCount;
    private BigDecimal tongDoanhThu;
    private BigDecimal doanhThuTrieuDong;
    private double tyLeHoanThanh;
}