package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho BieuMauTaiSan. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BieuMauTaiSanDTO {
    private Integer id;
    private String maBieuMau;
    private Integer donDatId;
    private Integer congTacVienId;
    private String loaiBieuMau;
    private LocalDateTime thoiGianGhiNhan;
    private Boolean xacNhanKhachHang;
    private String yKienChinhSua;
    private String ghiChuDacBiet;
}
