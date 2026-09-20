package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho GoiDichVu. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoiDichVuDTO {
    private Integer id;
    private String maGoi;
    private Integer dichVuId;
    private String tenGoi;
    private Integer soBuoi;
    private String tanSuat;
    private BigDecimal giaGoi;
    private String moTa;
    private String trangThai;
}
