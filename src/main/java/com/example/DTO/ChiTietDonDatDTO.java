package com.example.DTO;

import com.example.Model.ChiTietDonDat;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietDonDatDTO {
    private Integer dichVuId;
    private String tenDichVu;
    private Integer soLuong;
    private BigDecimal donGia;
    private BigDecimal thanhTien;
    private String ngayThucHienTrongTuan;
    private String ghiChu;

    public static ChiTietDonDatDTO from(ChiTietDonDat ct) {
        return ChiTietDonDatDTO.builder()
                .dichVuId(ct.getDichVu().getId())
                .tenDichVu(ct.getDichVu().getTenDichVu())
                .soLuong(ct.getSoLuong())
                .donGia(ct.getDonGia())
                .thanhTien(ct.getThanhTien())
                .ngayThucHienTrongTuan(ct.getNgayThucHienTrongTuan())
                .ghiChu(ct.getGhiChu())
                .build();
    }
}