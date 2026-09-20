package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** Bang 28: ChiTietHoaDon */
@Entity @Table(name = "ChiTietHoaDon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietHoaDon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hoaDonId", nullable = false) private HoaDon hoaDon;
    @Column(name = "TenDichVu", nullable = false, length = 200) private String tenDichVu;
    @Column(name = "YeuCauDacBiet", columnDefinition = "TEXT") private String yeuCauDacBiet;
    @Column(name = "DonViTinh", nullable = false, length = 30) private String donViTinh;
    @Column(name = "SoLuong", nullable = false, precision = 8, scale = 2) private BigDecimal soLuong;
    @Column(name = "DonGia", nullable = false, precision = 12, scale = 0) private BigDecimal donGia;
    @Column(name = "ThanhTien", nullable = false, precision = 12, scale = 0) private BigDecimal thanhTien;
    @Column(name = "LoaiDong", nullable = false, length = 20) private String loaiDong;
    @PrePersist protected void onCreate() { if (loaiDong == null) loaiDong = "DichVuChinh"; }
}
