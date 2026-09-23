package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

/** DTO cho LichSuSuDungKhuyenMai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuSuDungKhuyenMaiDTO {
    private Integer id;
    private Integer khuyenMaiId;
    private Integer khachHangId;
    private Integer donDatId;
    private LocalDateTime ngaySuDung;
    private BigDecimal soTienDuocGiam;
}