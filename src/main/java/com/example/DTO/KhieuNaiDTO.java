package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho KhieuNai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhieuNaiDTO {
    private Integer id;
    private String maKhieuNai;
    private Integer donDatId;
    private Integer khachHangId;
    private String loaiVanDe;
    private String noiDung;
    private String trangThai;
    private LocalDateTime ngayGui;
    private LocalDateTime ngayGiaiQuyet;
}
