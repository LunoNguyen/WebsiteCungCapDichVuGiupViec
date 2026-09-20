package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho LichSuSuDungCoupon. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuSuDungCouponDTO {
    private Integer id;
    private Integer couponId;
    private Integer khachHangId;
    private Integer donDatId;
    private LocalDateTime ngaySuDung;
    private BigDecimal soTienDuocGiam;
}
