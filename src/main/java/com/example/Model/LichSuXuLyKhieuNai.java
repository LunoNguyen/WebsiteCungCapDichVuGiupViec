package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 32: LichSuXuLyKhieuNai */
@Entity @Table(name = "LichSuXuLyKhieuNai")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuXuLyKhieuNai {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khieuNaiId", nullable = false) private KhieuNai khieuNai;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "nhanVienId") private NhanVien nhanVien;
    @Column(name = "HanhDong", nullable = false, columnDefinition = "TEXT") private String hanhDong;
    @Column(name = "KetQua", length = 50) private String ketQua;
    @Column(name = "ThoiGian", nullable = false, updatable = false) private LocalDateTime thoiGian;
    @PrePersist protected void onCreate() { if (thoiGian == null) thoiGian = LocalDateTime.now(); }
}
