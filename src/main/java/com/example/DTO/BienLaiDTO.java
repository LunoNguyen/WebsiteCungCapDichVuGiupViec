package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

/** DTO cho BienLai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BienLaiDTO {
    private Integer id;
    private String maBienLai;
    private Integer hoaDonId;
    private LocalDateTime ngayGioThuTien;
    private BigDecimal soTienNhan;
    private String hinhThucThanhToan;
    private String nguoiNopTien;
    private String nguoiThuTien;
}