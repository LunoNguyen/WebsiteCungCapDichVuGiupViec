package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho HoSoCTV. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoSoCTVDTO {
    private Integer id;
    private String maHoSo;
    private Integer congTacVienId;
    private String loaiTaiLieu;
    private String duongDanFile;
    private LocalDateTime ngayTai;
}
