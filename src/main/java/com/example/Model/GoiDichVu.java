package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** Bang 18: GoiDichVu */
@Entity @Table(name = "GoiDichVu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoiDichVu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaGoi", nullable = false, unique = true, length = 20)
    private String maGoi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dichVuId", nullable = false)
    private DichVu dichVu;

    @Column(name = "TenGoi", nullable = false, length = 150)
    private String tenGoi;

    @Column(name = "SoBuoi", nullable = false)
    private Integer soBuoi;

    @Column(name = "TanSuat", nullable = false, length = 100)
    private String tanSuat;

    @Column(name = "GiaGoi", nullable = false, precision = 12, scale = 0)
    private BigDecimal giaGoi;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | An

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
    }
}
