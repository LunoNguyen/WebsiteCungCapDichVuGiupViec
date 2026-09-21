package com.example.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO tong hop so lieu thong ke Marketing & Khuyen mai tu CSDL
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingStatsDto {
    private long tongKhuyenMai;
    private long khuyenMaiDangChayCount;
    private long tongCoupons;
    private long tongLuotSuDungCoupon;
    private BigDecimal doanhThuKhuyenMaiTrieuDong;
}
