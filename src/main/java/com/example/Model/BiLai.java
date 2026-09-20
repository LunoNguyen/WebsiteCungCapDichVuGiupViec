package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bang 29: BiLai */
@Entity @Table(name = "BiLai")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BiLai {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaBiLai", nullable = false, unique = true, length = 20) private String maBiLai;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "hoaDonId", nullable = false, unique = true) private HoaDon hoaDon;
    @Column(name = "NgayGioThuTien", nullable = false) private LocalDateTime ngayGioThuTien;
    @Column(name = "SoTienNhan", nullable = false, precision = 12, scale = 0) private BigDecimal soTienNhan;
    @Column(name = "HinhThucThanhToan", nullable = false, length = 20) private String hinhThucThanhToan;
    @Column(name = "NguoiNopTien", nullable = false, length = 100) private String nguoiNopTien;
    @Column(name = "NguoiThuTien", nullable = false, length = 100) private String nguoiThuTien;
}
