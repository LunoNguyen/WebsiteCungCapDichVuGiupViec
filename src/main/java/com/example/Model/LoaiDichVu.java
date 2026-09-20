package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 14: LoaiDichVu (Service Category) */
@Entity @Table(name = "LoaiDichVu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoaiDichVu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaLoaiDichVu", nullable = false, unique = true, length = 20)
    private String maLoaiDichVu;

    @Column(name = "TenLoaiDichVu", nullable = false, length = 150)
    private String tenLoaiDichVu;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "HinhAnh", length = 255)
    private String hinhAnh;

    @Column(name = "ThuTuHienThi", nullable = false)
    private Integer thuTuHienThi;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HienThi | An

    @PrePersist
    protected void onCreate() {
        if (thuTuHienThi == null) thuTuHienThi = 0;
        if (trangThai == null) trangThai = "HienThi";
    }
}
