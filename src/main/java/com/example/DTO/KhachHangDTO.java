package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho KhachHang. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhachHangDTO {
    private Integer id;
    private String maKhachHang;
    private Integer taiKhoanId;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String soDienThoai;
    private String email;
    private LocalDate ngayDangKy;
    private String trangThai;
}
