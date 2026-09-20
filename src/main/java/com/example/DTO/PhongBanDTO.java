package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho PhongBan. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhongBanDTO {
    private Integer id;
    private String maPhongBan;
    private String tenPhongBan;
    private String moTa;
    private String trangThai;
}
