package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 5: ChucVu */
@Entity @Table(name = "ChucVu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChucVu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaChucVu", nullable = false, unique = true, length = 20)
    private String maChucVu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phongBanId", nullable = false)
    private PhongBan phongBan;

    @Column(name = "TenChucVu", nullable = false, length = 100)
    private String tenChucVu;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;
}
