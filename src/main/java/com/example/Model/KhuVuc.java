package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 11: KhuVuc */
@Entity @Table(name = "KhuVuc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhuVuc {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaKhuVuc", nullable = false, unique = true, length = 20)
    private String maKhuVuc;

    @Column(name = "TenKhuVuc", nullable = false, length = 150)
    private String tenKhuVuc;

    @Column(name = "QuanHuyen", nullable = false, length = 100)
    private String quanHuyen;

    @Column(name = "TinhThanh", nullable = false, length = 100)
    private String tinhThanh;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | TamNgung

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
    }
}
