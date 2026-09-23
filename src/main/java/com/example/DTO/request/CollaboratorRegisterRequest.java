package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CollaboratorRegisterRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String soDienThoai;

    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String matKhau;

    private LocalDate ngaySinh;
    private String gioiTinh; // Nam | Nu | Khac

    @NotBlank(message = "Nơi cư trú không được để trống")
    private String noiCuTru;

    private String tenDangNhap;

    // Danh sách ID các dịch vụ CTV muốn đăng ký
    private List<Integer> danhSachDichVuId;

    // Danh sách ID các khu vực hoạt động
    private List<Integer> danhSachKhuVucId;

    // Danh sách chứng chỉ, bằng cấp (Đường dẫn lưu trên MinIO)
    private List<ChungChiItem> danhSachChungChi;

    // Danh sách hồ sơ tài liệu (CCCD trước, sau, ảnh chân dung lưu trên MinIO)
    private List<HoSoItem> danhSachHoSo;

    @Data
    public static class ChungChiItem {
        private String loaiChungChi; // BangCap | ChungNhan | ChungChi
        private String tenChungChi;
        private String noiCap;
        private LocalDate ngayCap;
        private String duongDanFile; // MinIO URL
    }

    @Data
    public static class HoSoItem {
        private String loaiTaiLieu; // AnhChanDung | CCCD_Mat_Truoc | CCCD_Mat_Sau | TaiLieuKhac
        private String duongDanFile; // MinIO URL
    }
}
