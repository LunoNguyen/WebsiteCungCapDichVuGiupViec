package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 35: PhanHoiDanhGia */
@Entity @Table(name = "PhanHoiDanhGia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhanHoiDanhGia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "danhGiaId", nullable = false) private DanhGia danhGia;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "nhanVienId") private NhanVien nhanVien;
    @Column(name = "NoiDung", nullable = false, columnDefinition = "TEXT") private String noiDung;
    @Column(name = "ThoiGian", nullable = false, updatable = false) private LocalDateTime thoiGian;
    @PrePersist protected void onCreate() { if (thoiGian == null) thoiGian = LocalDateTime.now(); }
}
