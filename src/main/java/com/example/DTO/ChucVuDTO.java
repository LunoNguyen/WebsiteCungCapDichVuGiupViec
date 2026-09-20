package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ChucVu. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChucVuDTO {
    private Integer id;
    private String maChucVu;
    private Integer phongBanId;
    private String tenChucVu;
    private String moTa;
}
