package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho LichSuTrangThaiDon. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuTrangThaiDonDTO {
    private Integer id;
    private Integer donDatId;
    private String trangThaiCu;
    private String trangThaiMoi;
    private String nguoiThucHien;
    private LocalDateTime thoiGian;
    private String ghiChu;
}
