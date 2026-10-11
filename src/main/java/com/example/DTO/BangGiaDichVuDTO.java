package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private LocalDateTime ngayApDung; // DATETIME: thời điểm bắt đầu áp dụng
    private LocalDateTime ngayKetThuc;
    private String trangThai;
}
