package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho OTPXacThuc. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OTPXacThucDTO {
    private Integer id;
    private Integer taiKhoanId;
    private String mucDich;
    private String maCode;
    private LocalDateTime thoiGianTao;
    private LocalDateTime thoiGianHetHan;
    private Boolean daSuDung;
}
