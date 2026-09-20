package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Bang 7: CongTacVien */
@Entity @Table(name = "CongTacVien")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CongTacVien {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaCongTacVien", nullable = false, unique = true, length = 20)
    private String maCongTacVien;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taiKhoanId", unique = true)
    private TaiKhoan taiKhoan;

    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "NgaySinh")
    private LocalDate ngaySinh;

    @Column(name = "GioiTinh", length = 10)
    private String gioiTinh; // Nam | Nu | Khac

    @Column(name = "NoiCuTru", nullable = false, length = 200)
    private String noiCuTru;

    @Column(name = "SoDienThoai", nullable = false, length = 15)
    private String soDienThoai;

    @Column(name = "DiemDanhGia", nullable = false, precision = 3, scale = 2)
    private BigDecimal diemDanhGia;

    @Column(name = "CapDo", nullable = false, length = 20)
    private String capDo; // Moi | Thuong | UuTu

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // ChoDuyet | HoatDong | DinhChi | TuChoi

    @Column(name = "NgayDangKy", nullable = false)
    private LocalDate ngayDangKy;

    @PrePersist
    protected void onCreate() {
        if (diemDanhGia == null) diemDanhGia = BigDecimal.ZERO;
        if (capDo == null) capDo = "Moi";
        if (trangThai == null) trangThai = "ChoDuyet";
        if (ngayDangKy == null) ngayDangKy = LocalDate.now();
    }
}
