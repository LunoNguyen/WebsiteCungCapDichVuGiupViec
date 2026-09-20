package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho KhuVuc. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhuVucDTO {
    private Integer id;
    private String maKhuVuc;
    private String tenKhuVuc;
    private String quanHuyen;
    private String tinhThanh;
    private String trangThai;
}
