package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho MaCoupon. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaCouponDTO {
    private Integer id;
    private String maCoupon;
    private Integer chuongTrinhKhuyenMaiId;
    private String codeCoupon;
    private Integer soLuotToiDa;
    private Integer soLuotDaDung;
    private String trangThai;
}
