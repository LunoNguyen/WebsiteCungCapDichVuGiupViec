package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tong hop so lieu thong ke Cong tac vien tu CSDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CongTacVienStatsDto {
    private long tongCongTacVien;
    private long hoatDongCount;
    private long choDuyetCount;
    private long dinhChiCount;
    private long uuTuCount;
    private long thuongCount;
    private long moiCount;
    private double diemDanhGiaTrungBinh;
}
