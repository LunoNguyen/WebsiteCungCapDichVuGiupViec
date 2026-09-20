package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "DonViTinh", nullable = false, length = 30)
    private String donViTinh;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | An

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
    }
}
