package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bang 27: HoaDon */
@Entity @Table(name = "HoaDon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoaDon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaHoaDon", nullable = false, unique = true, length = 20) private String maHoaDon;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false, unique = true) private DonDatDichVu donDat;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khachHangId", nullable = false) private KhachHang khachHang;
    @Column(name = "NgayLap", nullable = false, updatable = false) private LocalDateTime ngayLap;
    @Column(name = "HinhThucThanhToan", nullable = false, length = 20) private String hinhThucThanhToan;
    @Column(name = "TongTienHang", nullable = false, precision = 12, scale = 0) private BigDecimal tongTienHang;
    @Column(name = "SoTienGiam", nullable = false, precision = 12, scale = 0) private BigDecimal soTienGiam;
    @Column(name = "TongThanhToan", nullable = false, precision = 12, scale = 0) private BigDecimal tongThanhToan;
    @Column(name = "TrangThaiThanhToan", nullable = false, length = 20) private String trangThaiThanhToan;
    @Column(name = "NgayThanhToan") private LocalDateTime ngayThanhToan;
    @PrePersist protected void onCreate() { if (ngayLap == null) ngayLap = LocalDateTime.now(); if (soTienGiam == null) soTienGiam = BigDecimal.ZERO; if (trangThaiThanhToan == null) trangThaiThanhToan = "ChuaThanhToan"; }
}
