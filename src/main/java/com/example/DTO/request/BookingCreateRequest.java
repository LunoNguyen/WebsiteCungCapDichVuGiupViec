package com.example.DTO.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class BookingCreateRequest {
    @NotNull(message = "khachHangId hoặc taiKhoanId không được để trống")
    private Integer khachHangId;

    private Integer taiKhoanId; // Hỗ trợ truyền taiKhoanId nếu client lưu taiKhoanId

    private Integer diaChiId;
    // Nếu khách hàng nhập địa chỉ mới ngay tại màn hình đặt dịch vụ:
    private String diaChiChiTiet;
    private Integer khuVucId;

    // Danh sách dịch vụ trong đơn (đặt nhiều dịch vụ)
    @Valid
    private List<ChiTietDonDatRequest> chiTiet;

    // Lối tắt cho đặt 1 dịch vụ (client cũ vẫn gửi trường này)
    private Integer dichVuId;

    private Integer bangGiaId;
    private String ngayThucHienTrongTuan;

    private String loaiHinhDat; // TheoLan | GoiThang (mặc định TheoLan)

    @NotNull(message = "Ngày thực hiện không được để trống")
    private LocalDate ngayThucHien;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime gioBatDau;

    private LocalTime gioKetThuc;
    private String yeuCauDacBiet;
    private String codeKhuyenMai;
    private String ghiChu;

    /** Gộp chiTiet và dichVuId thành một danh sách duy nhất cho service dùng */
    @JsonIgnore
    public List<ChiTietDonDatRequest> getDanhSachDichVu() {
        if (chiTiet != null && !chiTiet.isEmpty()) return chiTiet;
        List<ChiTietDonDatRequest> ds = new ArrayList<>();
        if (dichVuId != null) {
            ChiTietDonDatRequest ct = new ChiTietDonDatRequest();
            ct.setDichVuId(dichVuId);
            ct.setSoLuong(1);
            ds.add(ct);
        }
        return ds;
    }
}