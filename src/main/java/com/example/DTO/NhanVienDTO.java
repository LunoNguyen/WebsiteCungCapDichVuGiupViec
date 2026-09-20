package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho NhanVien. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhanVienDTO {
    private Integer id;
    private String maNhanVien;
    private Integer taiKhoanId;
    private Integer chucVuId;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String diaChi;
    private String soDienThoai;
    private String email;
    private LocalDate ngayVaoLam;
    private String trangThai;
}
