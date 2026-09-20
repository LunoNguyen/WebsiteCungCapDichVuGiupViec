package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho DonDatDichVu. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonDatDichVuDTO {
    private Integer id;
    private String maDonDat;
    private Integer khachHangId;
    private Integer diaChiId;
    private Integer nhanVienTiepNhanId;
    private Integer dichVuId;
    private Integer bangGiaId;
    private Integer goiDichVuId;
    private Integer couponId;
    private String loaiHinhDat;
    private LocalDate ngayThucHien;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private String yeuCauDacBiet;
    private BigDecimal chiPhiGoc;
    private BigDecimal soTienGiam;
    private BigDecimal thanhTien;
    private String trangThai;
    private LocalDateTime ngayTao;
    private String ghiChu;
}
