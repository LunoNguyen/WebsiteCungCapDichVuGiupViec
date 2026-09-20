package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bang 8: ChungChiCTV */
@Entity @Table(name = "ChungChiCTV")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChungChiCTV {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaChungChi", nullable = false, unique = true, length = 20)
    private String maChungChi;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congTacVienId", nullable = false)
    private CongTacVien congTacVien;

    @Column(name = "LoaiChungChi", nullable = false, length = 50)
    private String loaiChungChi; // BangCap | ChungNhan | ChungChi

    @Column(name = "TenChungChi", nullable = false, length = 200)
    private String tenChungChi;

    @Column(name = "NoiCap", length = 200)
    private String noiCap;

    @Column(name = "NgayCap")
    private LocalDate ngayCap;

    @Column(name = "DuongDanFile", length = 500)
    private String duongDanFile;
}
