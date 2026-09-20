package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ChiTietHoaDon. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietHoaDonDTO {
    private Integer id;
    private Integer hoaDonId;
    private String tenDichVu;
    private String yeuCauDacBiet;
    private String donViTinh;
    private BigDecimal soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private String loaiDong;
}
