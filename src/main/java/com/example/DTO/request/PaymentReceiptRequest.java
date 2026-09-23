package com.example.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentReceiptRequest {
    @NotNull(message = "donDatId không được để trống")
    private Integer donDatId;

    private Integer hoaDonId;
    private String hinhThucThanhToan; // TienMat | ChuyenKhoan (mặc định ChuyenKhoan)
    private BigDecimal soTienNhan;
    private String nguoiNopTien;
    private String nguoiThuTien;
}
