package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

/** DTO cho DichVu. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DichVuDTO {
    private Integer id;
    private String maDichVu;
    private Integer loaiDichVuId;
    private String tenDichVu;
    private String moTaChiTiet;
    
    // Bổ sung 2 trường mới theo Database v4
    private Integer thoiGianThucHien; 
    private String loaiHinhDat;       
    
    private String donViTinh;
    private String trangThai;
}