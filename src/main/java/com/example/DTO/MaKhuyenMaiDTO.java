package com.example.DTO;
import lombok.*;

/** DTO cho MaKhuyenMai. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaKhuyenMaiDTO {
    private Integer id;
    private String maKhuyenMai;
    private Integer chuongTrinhKhuyenMaiId;
    private String codeKhuyenMai;
    private Integer soLuotToiDa;
    private Integer soLuotDaDung;
    private String trangThai;
}