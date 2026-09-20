package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho NhatKyTaiKhoan. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhatKyTaiKhoanDTO {
    private Integer id;
    private Integer taiKhoanId;
    private String hanhDong;
    private String diaChiIP;
    private String thietBi;
    private LocalDateTime thoiGian;
    private String ketQua;
}
