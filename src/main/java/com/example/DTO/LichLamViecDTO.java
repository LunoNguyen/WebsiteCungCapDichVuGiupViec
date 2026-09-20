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
}
