package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho LoaiDichVu. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoaiDichVuDTO {
    private Integer id;
    private String maLoaiDichVu;
    private String tenLoaiDichVu;
    private String moTa;
    private String hinhAnh;
    private Integer thuTuHienThi;
    private String trangThai;
}
