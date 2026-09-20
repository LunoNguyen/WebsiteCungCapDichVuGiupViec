package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 12: KhuVucCTV (CTV <-> KhuVuc junction) */
@Entity @Table(name = "KhuVucCTV")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhuVucCTV {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaKhuVucCTV", nullable = false, unique = true, length = 20)
    private String maKhuVucCTV;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congTacVienId", nullable = false)
    private CongTacVien congTacVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khuVucId", nullable = false)
    private KhuVuc khuVuc;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | TamNgung

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
    }
}
