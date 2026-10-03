package com.example.DTO.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/** Dịch vụ hiển thị trên website công khai (lưu được vào cache Redis). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DichVuView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String maDichVu;
    private String tenDichVu;
    private String moTa;
    /** TheoLan | GoiThang */
    private String loaiHinhDat;
    private String donViTinh;
    /** Vd: "2 giờ", "1 giờ 30 phút"; rỗng nếu thời gian không cố định. */
    private String thoiGianText;
    private Integer soBuoi;
    private Integer soNguoiThucHien;
    private BigDecimal gia;
    /** Vd: "192.000đ" hoặc "Liên hệ". */
    private String giaText;
}
