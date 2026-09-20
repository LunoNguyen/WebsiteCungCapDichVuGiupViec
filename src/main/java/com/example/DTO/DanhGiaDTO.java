package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
/** DTO cho DanhGia. Quan he duoc truyen bang khoa ngoai id. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DanhGiaDTO {
    private Integer id;
    private String maDanhGia;
    private Integer donDatId;
    private Integer khachHangId;
    private Integer congTacVienId;
    private Integer diemChatLuong;
    private Integer diemThaiDo;
    private String nhanXet;
    private LocalDateTime ngayDanhGia;
    private String trangThai;
}
