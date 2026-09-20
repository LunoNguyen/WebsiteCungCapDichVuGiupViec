package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho TaiKhoan. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaiKhoanDTO {
    private Integer id;
    private String maTaiKhoan;
    private String tenDangNhap;
    private String matKhau;
    private String email;
    private String soDienThoai;
    private String loaiTaiKhoan;
    private String trangThai;
    private LocalDateTime ngayTao;
    private LocalDateTime ngayCapNhat;
}
