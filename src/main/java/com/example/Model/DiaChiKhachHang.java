package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** Bang 13: DiaChiKhachHang */
@Entity @Table(name = "DiaChiKhachHang")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DiaChiKhachHang {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaDiaChi", nullable = false, unique = true, length = 20)
    private String maDiaChi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khachHangId", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khuVucId")
    private KhuVuc khuVuc;

    @Column(name = "DiaChiChiTiet", nullable = false, length = 300)
    private String diaChiChiTiet;

    @Column(name = "DienTichNha", precision = 8, scale = 2)
    private BigDecimal dienTichNha;

    @Column(name = "LuuYDacBiet", columnDefinition = "TEXT")
    private String luuYDacBiet;

    @Column(name = "LaMacDinh", nullable = false)
    private Boolean laMacDinh;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | DaXoa

    @PrePersist
    protected void onCreate() {
        if (laMacDinh == null) laMacDinh = false;
        if (trangThai == null) trangThai = "HoatDong";
    }
}
