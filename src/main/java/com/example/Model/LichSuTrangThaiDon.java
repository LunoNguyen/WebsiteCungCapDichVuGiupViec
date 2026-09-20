package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 23: LichSuTrangThaiDon */
@Entity @Table(name = "LichSuTrangThaiDon")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuTrangThaiDon {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false) private DonDatDichVu donDat;
    @Column(name = "TrangThaiCu", length = 30) private String trangThaiCu;
    @Column(name = "TrangThaiMoi", nullable = false, length = 30) private String trangThaiMoi;
    @Column(name = "NguoiThucHien", length = 50) private String nguoiThucHien;
    @Column(name = "ThoiGian", nullable = false, updatable = false) private LocalDateTime thoiGian;
    @Column(name = "GhiChu", columnDefinition = "TEXT") private String ghiChu;
    @PrePersist protected void onCreate() { if (thoiGian == null) thoiGian = LocalDateTime.now(); }
}
