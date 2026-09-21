package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tong hop so lieu thong ke Khach hang tu CSDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangStatsDto {
    private long tongKhachHang;
    private long khachMoiCount;
    private long khachVipCount;
    private long khachCoDonHangCount;
    private double diemDanhGiaTrungBinh;
}
