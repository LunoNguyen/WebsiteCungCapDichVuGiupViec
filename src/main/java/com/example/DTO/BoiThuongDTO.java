package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho BoiThuong. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoiThuongDTO {
    private Integer id;
    private String maBoiThuong;
    private Integer khieuNaiId;
    private String phuongAnBoiThuong;
    private BigDecimal giaTriBoiThuong;
    private String trangThai;
    private LocalDateTime ngayPheduyet;
    private LocalDateTime ngayThucHien;
}
