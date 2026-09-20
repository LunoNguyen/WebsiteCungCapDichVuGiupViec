package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho LichSuXuLyKhieuNai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuXuLyKhieuNaiDTO {
    private Integer id;
    private Integer khieuNaiId;
    private Integer nhanVienId;
    private String hanhDong;
    private String ketQua;
    private LocalDateTime thoiGian;
}
