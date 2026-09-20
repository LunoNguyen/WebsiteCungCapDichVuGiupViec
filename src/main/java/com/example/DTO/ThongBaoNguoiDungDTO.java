package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho ThongBaoNguoiDung. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThongBaoNguoiDungDTO {
    private Integer id;
    private Integer thongBaoId;
    private Integer taiKhoanId;
    private Boolean daDoc;
    private LocalDateTime thoiGianDoc;
    private String trangThai;
}
