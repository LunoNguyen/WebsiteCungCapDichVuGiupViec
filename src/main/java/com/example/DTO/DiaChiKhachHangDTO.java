package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho DiaChiKhachHang. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiaChiKhachHangDTO {
    private Integer id;
    private String maDiaChi;
    private Integer khachHangId;
    private Integer khuVucId;
    private String diaChiChiTiet;
    private BigDecimal dienTichNha;
    private String luuYDacBiet;
    private Boolean laMacDinh;
    private String trangThai;
}
