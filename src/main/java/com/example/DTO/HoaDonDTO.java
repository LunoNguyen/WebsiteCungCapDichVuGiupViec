package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho HoaDon. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDonDTO {
    private Integer id;
    private String maHoaDon;
    private Integer donDatId;
    private Integer khachHangId;
    private LocalDateTime ngayLap;
    private String hinhThucThanhToan;
    private BigDecimal tongTienHang;
    private BigDecimal soTienGiam;
    private BigDecimal tongThanhToan;
    private String trangThaiThanhToan;
    private LocalDateTime ngayThanhToan;
}
