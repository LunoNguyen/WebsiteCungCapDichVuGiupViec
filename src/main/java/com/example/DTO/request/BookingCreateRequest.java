package com.example.DTO.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingCreateRequest {
    @NotNull(message = "khachHangId hoặc taiKhoanId không được để trống")
    private Integer khachHangId;

    private Integer taiKhoanId; // Hỗ trợ truyền taiKhoanId nếu client lưu taiKhoanId

    private Integer diaChiId;
    // Nếu khách hàng nhập địa chỉ mới ngay tại màn hình đặt dịch vụ:
    private String diaChiChiTiet;
    private Integer khuVucId;

    @NotNull(message = "dichVuId không được để trống")
    private Integer dichVuId;

    private String loaiHinhDat; // TheoLan | GoiThang (mặc định TheoLan)
    private Integer bangGiaId;
    private Integer goiDichVuId;

    @NotNull(message = "Ngày thực hiện không được để trống")
    private LocalDate ngayThucHien;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime gioBatDau;

    private LocalTime gioKetThuc;
    private String yeuCauDacBiet;
    private String codeKhuyenMai;
    private String ghiChu;
}
