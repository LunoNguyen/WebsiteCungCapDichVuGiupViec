package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 36: ThongBao */
@Entity @Table(name = "ThongBao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThongBao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaThongBao", nullable = false, unique = true, length = 20) private String maThongBao;
    @Column(name = "TieuDe", nullable = false, length = 200) private String tieuDe;
    @Column(name = "NoiDung", nullable = false, columnDefinition = "TEXT") private String noiDung;
    @Column(name = "NguoiGui", nullable = false, length = 50) private String nguoiGui;
    @Column(name = "NhomNhan", nullable = false, length = 30) private String nhomNhan;
    @Column(name = "ThoiGianGui", nullable = false, updatable = false) private LocalDateTime thoiGianGui;
    @Column(name = "TrangThai", nullable = false, length = 20) private String trangThai;
    @PrePersist protected void onCreate() { if (thoiGianGui == null) thoiGianGui = LocalDateTime.now(); if (trangThai == null) trangThai = "Nhap"; }
}
