package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho LichLamViec. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichLamViecDTO {
    private Integer id;
    private String maLichLamViec;
    private Integer phanCongId;
    private Integer congTacVienId;
    private LocalDate ngayLam;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private String trangThai;
    private String ketQuaThucHien;

    // Display fields for UI / Calendar
    private String ctvTen;
    private BigDecimal ctvDiemDanhGia;
    private String ctvNoiCuTru;
    private String ctvCapDo;
    private String khachHangTen;
    private String khachHangSdt;
    private String dichVuTen;
    private String diaChiChiTiet;
    private String maDonDat;
    private Integer donDatId;
}
