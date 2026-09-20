package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

/** Bang 38: LichLamViec */
@Entity @Table(name = "LichLamViec")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichLamViec {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaLichLamViec", nullable = false, unique = true, length = 20) private String maLichLamViec;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "phanCongId", nullable = false) private PhanCongCTV phanCong;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "congTacVienId", nullable = false) private CongTacVien congTacVien;
    @Column(name = "NgayLam", nullable = false) private LocalDate ngayLam;
    @Column(name = "GioBatDau", nullable = false) private LocalTime gioBatDau;
    @Column(name = "GioKetThuc") private LocalTime gioKetThuc;
    @Column(name = "TrangThai", nullable = false, length = 20) private String trangThai;
    @Column(name = "KetQuaThucHien", columnDefinition = "TEXT") private String ketQuaThucHien;
    @PrePersist protected void onCreate() { if (trangThai == null) trangThai = "SapToi"; }
}
