package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 20: MaKhuyenMai (thay the cho MaCoupon) */
@Entity @Table(name = "MaKhuyenMai")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaKhuyenMai {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaKhuyenMai", nullable = false, unique = true, length = 20)
    private String maKhuyenMai;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuongTrinhKMId", nullable = false)
    private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

    @Column(name = "CodeKhuyenMai", nullable = false, unique = true, length = 50)
    private String codeKhuyenMai;

    @Column(name = "SoLuotToiDa", nullable = false)
    private Integer soLuotToiDa;

    @Column(name = "SoLuotDaDung", nullable = false)
    private Integer soLuotDaDung;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | HetLuot | HetHan

    @PrePersist
    protected void onCreate() {
        if (soLuotToiDa == null) soLuotToiDa = 1;
        if (soLuotDaDung == null) soLuotDaDung = 0;
        if (trangThai == null) trangThai = "HoatDong";
    }
}