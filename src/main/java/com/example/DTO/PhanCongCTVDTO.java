package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho PhanCongCTV. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhanCongCTVDTO {
    private Integer id;
    private String maPhanCong;
    private Integer donDatId;
    private Integer congTacVienId;
    private String trangThai;
    private LocalDateTime thoiGianPhanCong;
    private LocalDateTime thoiGianXacNhan;
    private String lyDoTuChoi;
}
