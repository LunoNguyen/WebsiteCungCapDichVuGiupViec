package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 24: PhanCongCTV */
@Entity @Table(name = "PhanCongCTV", uniqueConstraints = @UniqueConstraint(columnNames = {"donDatId", "congTacVienId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhanCongCTV {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaPhanCong", nullable = false, unique = true, length = 20) private String maPhanCong;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false) private DonDatDichVu donDat;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "congTacVienId", nullable = false) private CongTacVien congTacVien;
    @Column(name = "TrangThai", nullable = false, length = 20) private String trangThai;
    @Column(name = "ThoiGianPhanCong", nullable = false, updatable = false) private LocalDateTime thoiGianPhanCong;
    @Column(name = "ThoiGianXacNhan") private LocalDateTime thoiGianXacNhan;
    @Column(name = "LyDoTuChoi", columnDefinition = "TEXT") private String lyDoTuChoi;
    @PrePersist protected void onCreate() { if (trangThai == null) trangThai = "ChoPhanCong"; if (thoiGianPhanCong == null) thoiGianPhanCong = LocalDateTime.now(); }
}
