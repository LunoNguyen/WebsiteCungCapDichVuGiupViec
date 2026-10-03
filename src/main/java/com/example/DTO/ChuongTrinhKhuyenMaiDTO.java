package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ChuongTrinhKhuyenMai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChuongTrinhKhuyenMaiDTO {
    private Integer id;
    private String maChuongTrinh;
    private String tenChuongTrinh;
    private String moTa;
    private String loaiGiam;
    private BigDecimal giaTriGiam;
    private BigDecimal soTienGiamToiDa;
    private BigDecimal dieuKienToiThieu;
    private LocalDate ngayBatDau;
    private LocalDate ngayKetThuc;
    private String trangThai;

    public BigDecimal getGiaTriGiamToiDa() {
        return this.soTienGiamToiDa;
    }

    public void setGiaTriGiamToiDa(BigDecimal giaTriGiamToiDa) {
        this.soTienGiamToiDa = giaTriGiamToiDa;
    }
}
