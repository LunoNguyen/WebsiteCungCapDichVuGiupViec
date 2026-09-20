package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho BangGiaDichVu. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BangGiaDichVuDTO {
    private Integer id;
    private String maBangGia;
    private Integer dichVuId;
    private Integer khuVucId;
    private String loaiHinhDat;
    private String donViTinh;
    private BigDecimal donGia;
    private LocalDate ngayApDung;
    private LocalDate ngayKetThuc;
    private String trangThai;
}
