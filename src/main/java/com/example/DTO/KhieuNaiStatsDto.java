package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tong hop so lieu thong ke Khieu nai tu CSDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhieuNaiStatsDto {
    private long tongKhieuNai;
    private long chuaXuLyCount;
    private long dangXuLyCount;
    private long leoThangCount;
    private long daGiaiQuyetCount;
    private double thoiGianXuLyTrungBinhNgay;
}
