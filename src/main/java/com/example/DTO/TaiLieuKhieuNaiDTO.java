package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho TaiLieuKhieuNai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaiLieuKhieuNaiDTO {
    private Integer id;
    private Integer khieuNaiId;
    private String loaiFile;
    private String duongDanFile;
    private LocalDateTime ngayTai;
}
