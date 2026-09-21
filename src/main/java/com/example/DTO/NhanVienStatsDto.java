package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tong hop so lieu thong ke Nhan vien noi bo tu CSDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NhanVienStatsDto {
    private long tongNhanVien;
    private long dangLamViecCount;
    private long tamNghiCount;
    private long nghiViecCount;
}
