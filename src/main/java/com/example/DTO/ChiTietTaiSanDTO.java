package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ChiTietTaiSan. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietTaiSanDTO {
    private Integer id;
    private Integer bieuMauId;
    private String khuVucVatDung;
    private String moTaHienTrang;
    private String hinhAnh;
}
