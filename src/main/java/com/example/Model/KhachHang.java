package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** Bang 10: KhachHang */
@Entity @Table(name = "KhachHang")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhachHang {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaKhachHang", nullable = false, unique = true, length = 20)
    private String maKhachHang;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taiKhoanId", unique = true)
    private TaiKhoan taiKhoan;

    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "NgaySinh")
    private LocalDate ngaySinh;

    @Column(name = "GioiTinh", length = 10)
    private String gioiTinh; // Nam | Nu | Khac

    @Column(name = "SoDienThoai", nullable = false, length = 15)
    private String soDienThoai;

    @Column(name = "Email", length = 150)
    private String email;

    @Column(name = "NgayDangKy", nullable = false)
    private LocalDate ngayDangKy;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | BiKhoa

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
        if (ngayDangKy == null) ngayDangKy = LocalDate.now();
    }
}
