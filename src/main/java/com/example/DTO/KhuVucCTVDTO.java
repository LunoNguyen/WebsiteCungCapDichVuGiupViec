package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho KhuVucCTV. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhuVucCTVDTO {
    private Integer id;
    private String maKhuVucCTV;
    private Integer congTacVienId;
    private Integer khuVucId;
    private String trangThai;
}
