package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho BiLai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiLaiDTO {
    private Integer id;
    private String maBiLai;
    private Integer hoaDonId;
    private LocalDateTime ngayGioThuTien;
    private BigDecimal soTienNhan;
    private String hinhThucThanhToan;
    private String nguoiNopTien;
    private String nguoiThuTien;
}
