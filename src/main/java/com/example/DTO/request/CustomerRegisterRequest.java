package com.example.DTO.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CustomerRegisterRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String soDienThoai;

    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String matKhau;

    private LocalDate ngaySinh;
    private String gioiTinh; // Nam | Nu | Khac
    private String diaChiChiTiet;
    private Integer khuVucId;
    private String tenDangNhap; // Tùy chọn, nếu không gửi sẽ lấy số điện thoại làm tên đăng nhập
}
