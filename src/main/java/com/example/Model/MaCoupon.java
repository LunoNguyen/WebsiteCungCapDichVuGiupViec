package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 20: MaCoupon */
@Entity @Table(name = "MaCoupon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MaCoupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaCoupon", nullable = false, unique = true, length = 20)
    private String maCoupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuongTrinhKMId", nullable = false)
    private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

    @Column(name = "CodeCoupon", nullable = false, unique = true, length = 50)
    private String codeCoupon;

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
