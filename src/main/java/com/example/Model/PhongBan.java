package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 4: PhongBan */
@Entity @Table(name = "PhongBan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhongBan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaPhongBan", nullable = false, unique = true, length = 20)
    private String maPhongBan;

    @Column(name = "TenPhongBan", nullable = false, length = 100)
    private String tenPhongBan;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | DaGiaiThe

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
    }
}
