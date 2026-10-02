package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** Bang 15: DichVu */
@Entity @Table(name = "DichVu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DichVu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaDichVu", nullable = false, unique = true, length = 20)
    private String maDichVu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loaiDichVuId", nullable = false)
    private LoaiDichVu loaiDichVu;

    @Column(name = "TenDichVu", nullable = false, length = 200)
    private String tenDichVu;

    @Column(name = "MoTaChiTiet", columnDefinition = "TEXT")
    private String moTaChiTiet;

    // Thời gian thực hiện mỗi buổi (phút)
    @Column(name = "ThoiGianThucHien")
    private Integer thoiGianThucHien;

    // TheoLan hoặc GoiThang
    @Column(name = "LoaiHinhDat", nullable = false, length = 20)
    private String loaiHinhDat;

    @Column(name = "DonViTinh", nullable = false, length = 30)
    private String donViTinh;

    // --- Cột mới (gom từ GoiDichVu) ---
    @Column(name = "SoBuoi")
    private Integer soBuoi; // null = theo lần

    @Column(name = "SoNguoiThucHien")
    private Integer soNguoiThucHien;

    @Column(name = "GiaHienTai", nullable = false, precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal giaHienTai = BigDecimal.ZERO;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | An

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
        if (loaiHinhDat == null) loaiHinhDat = "TheoLan";
        if (giaHienTai == null) giaHienTai = BigDecimal.ZERO;
    }
}