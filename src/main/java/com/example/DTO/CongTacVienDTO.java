package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho CongTacVien. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CongTacVienDTO {
    private Integer id;
    private String maCongTacVien;
    private Integer taiKhoanId;
    private String hoTen;
    private LocalDate ngaySinh;
    private String gioiTinh;
    private String noiCuTru;
    private String soDienThoai;
    private BigDecimal diemDanhGia;
    private String capDo;
    private String trangThai;
    private LocalDate ngayDangKy;
}
