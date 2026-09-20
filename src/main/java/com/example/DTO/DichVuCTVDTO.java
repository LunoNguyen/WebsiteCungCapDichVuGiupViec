package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho DichVuCTV. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DichVuCTVDTO {
    private Integer id;
    private String maDichVuCTV;
    private Integer congTacVienId;
    private Integer dichVuId;
    private String trangThai;
    private LocalDate ngayBatDau;
}
