package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho PhanHoiDanhGia. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhanHoiDanhGiaDTO {
    private Integer id;
    private Integer danhGiaId;
    private Integer nhanVienId;
    private String noiDung;
    private LocalDateTime thoiGian;
}
