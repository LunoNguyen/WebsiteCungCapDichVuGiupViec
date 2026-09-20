package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 31: TaiLieuKhieuNai */
@Entity @Table(name = "TaiLieuKhieuNai")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaiLieuKhieuNai {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khieuNaiId", nullable = false) private KhieuNai khieuNai;
    @Column(name = "LoaiFile", nullable = false, length = 50) private String loaiFile;
    @Column(name = "DuongDanFile", nullable = false, length = 500) private String duongDanFile;
    @Column(name = "NgayTai", nullable = false, updatable = false) private LocalDateTime ngayTai;
    @PrePersist protected void onCreate() { if (ngayTai == null) ngayTai = LocalDateTime.now(); }
}
