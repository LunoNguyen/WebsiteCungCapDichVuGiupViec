package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ChungChiCTV. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChungChiCTVDTO {
    private Integer id;
    private String maChungChi;
    private Integer congTacVienId;
    private String loaiChungChi;
    private String tenChungChi;
    private String noiCap;
    private LocalDate ngayCap;
    private String duongDanFile;
}
