package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 9: HoSoCTV */
@Entity @Table(name = "HoSoCTV")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HoSoCTV {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaHoSo", nullable = false, unique = true, length = 20)
    private String maHoSo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congTacVienId", nullable = false)
    private CongTacVien congTacVien;

    @Column(name = "LoaiTaiLieu", nullable = false, length = 50)
    private String loaiTaiLieu; // AnhChanDung | CCCD_Mat_Truoc | CCCD_Mat_Sau | TaiLieuKhac

    @Column(name = "DuongDanFile", nullable = false, length = 500)
    private String duongDanFile;

    @Column(name = "NgayTai", nullable = false, updatable = false)
    private LocalDateTime ngayTai;

    @PrePersist
    protected void onCreate() {
        if (ngayTai == null) ngayTai = LocalDateTime.now();
    }
}
