package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** Bang 6: NhanVien */
@Entity @Table(name = "NhanVien")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhanVien {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaNhanVien", nullable = false, unique = true, length = 20)
    private String maNhanVien;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taiKhoanId", unique = true)
    private TaiKhoan taiKhoan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chucVuId", nullable = false)
    private ChucVu chucVu;

    @Column(name = "HoTen", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "NgaySinh")
    private LocalDate ngaySinh;

    @Column(name = "GioiTinh", length = 10)
    private String gioiTinh; // Nam | Nu | Khac

    @Column(name = "DiaChi", length = 200)
    private String diaChi;

    @Column(name = "SoDienThoai", nullable = false, length = 15)
    private String soDienThoai;

    @Column(name = "Email", length = 150)
    private String email;

    @Column(name = "NgayVaoLam", nullable = false)
    private LocalDate ngayVaoLam;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // DangLamViec | DaNghi | TamNghi

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "DangLamViec";
    }
}
