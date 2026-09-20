package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bang 33: BoiThuong */
@Entity @Table(name = "BoiThuong")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoiThuong {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaBoiThuong", nullable = false, unique = true, length = 20) private String maBoiThuong;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khieuNaiId", nullable = false) private KhieuNai khieuNai;
    @Column(name = "PhuongAnBoiThuong", nullable = false, columnDefinition = "TEXT") private String phuongAnBoiThuong;
    @Column(name = "GiaTriBoiThuong", nullable = false, precision = 12, scale = 0) private BigDecimal giaTriBoiThuong;
    @Column(name = "TrangThai", nullable = false, length = 30) private String trangThai;
    @Column(name = "NgayPheduyet") private LocalDateTime ngayPheduyet;
    @Column(name = "NgayThucHien") private LocalDateTime ngayThucHien;
    @PrePersist protected void onCreate() { if (giaTriBoiThuong == null) giaTriBoiThuong = BigDecimal.ZERO; if (trangThai == null) trangThai = "ChoPheduyet"; }
}
