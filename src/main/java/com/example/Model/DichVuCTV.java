package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/** Bang 16: DichVuCTV (CTV <-> DichVu junction) */
@Entity @Table(name = "DichVuCTV")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DichVuCTV {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaDichVuCTV", nullable = false, unique = true, length = 20)
    private String maDichVuCTV;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congTacVienId", nullable = false)
    private CongTacVien congTacVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dichVuId", nullable = false)
    private DichVu dichVu;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | TamNgung

    @Column(name = "NgayBatDau")
    private LocalDate ngayBatDau;

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "HoatDong";
    }
}
