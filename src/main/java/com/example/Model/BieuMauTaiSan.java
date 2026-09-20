package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 25: BieuMauTaiSan */
@Entity @Table(name = "BieuMauTaiSan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BieuMauTaiSan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaBieuMau", nullable = false, unique = true, length = 20) private String maBieuMau;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false) private DonDatDichVu donDat;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "congTacVienId", nullable = false) private CongTacVien congTacVien;
    @Column(name = "LoaiBieuMau", nullable = false, length = 20) private String loaiBieuMau;
    @Column(name = "ThoiGianGhiNhan", nullable = false, updatable = false) private LocalDateTime thoiGianGhiNhan;
    @Column(name = "XacNhanKhachHang", nullable = false) private Boolean xacNhanKhachHang;
    @Column(name = "YKienChinhSua", columnDefinition = "TEXT") private String yKienChinhSua;
    @Column(name = "GhiChuDacBiet", columnDefinition = "TEXT") private String ghiChuDacBiet;
    @PrePersist protected void onCreate() { if (thoiGianGhiNhan == null) thoiGianGhiNhan = LocalDateTime.now(); if (xacNhanKhachHang == null) xacNhanKhachHang = false; }
}
