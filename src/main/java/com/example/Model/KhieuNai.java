package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 30: KhieuNai */
@Entity @Table(name = "KhieuNai")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhieuNai {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaKhieuNai", nullable = false, unique = true, length = 20) private String maKhieuNai;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false) private DonDatDichVu donDat;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khachHangId", nullable = false) private KhachHang khachHang;
    @Column(name = "LoaiVanDe", nullable = false, length = 50) private String loaiVanDe;
    @Column(name = "NoiDung", nullable = false, columnDefinition = "TEXT") private String noiDung;
    @Column(name = "TrangThai", nullable = false, length = 30) private String trangThai;
    @Column(name = "NgayGui", nullable = false, updatable = false) private LocalDateTime ngayGui;
    @Column(name = "NgayGiaiQuyet") private LocalDateTime ngayGiaiQuyet;
    @PrePersist protected void onCreate() { if (trangThai == null) trangThai = "Moi"; if (ngayGui == null) ngayGui = LocalDateTime.now(); }
}
