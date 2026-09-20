package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ThongBao. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThongBaoDTO {
    private Integer id;
    private String maThongBao;
    private String tieuDe;
    private String noiDung;
    private String nguoiGui;
    private String nhomNhan;
    private LocalDateTime thoiGianGui;
    private String trangThai;
}
