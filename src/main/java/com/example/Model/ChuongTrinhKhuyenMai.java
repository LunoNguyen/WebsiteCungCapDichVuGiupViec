package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Bang 19: ChuongTrinhKhuyenMai */
@Entity @Table(name = "ChuongTrinhKhuyenMai")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChuongTrinhKhuyenMai {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaChuongTrinh", nullable = false, unique = true, length = 20)
    private String maChuongTrinh;

    @Column(name = "TenChuongTrinh", nullable = false, length = 200)
    private String tenChuongTrinh;

    @Column(name = "MoTa", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "LoaiGiam", nullable = false, length = 20)
    private String loaiGiam; // PhanTram | SoTienCoDinh

    @Column(name = "GiaTriGiam", nullable = false, precision = 12, scale = 2)
    private BigDecimal giaTriGiam;

    @Column(name = "SoTienGiamToiDa", precision = 12, scale = 0)
    private BigDecimal soTienGiamToiDa;

    @Column(name = "DieuKienToiThieu", nullable = false, precision = 12, scale = 0)
    private BigDecimal dieuKienToiThieu;

    @Column(name = "NgayBatDau", nullable = false)
    private LocalDate ngayBatDau;

    @Column(name = "NgayKetThuc", nullable = false)
    private LocalDate ngayKetThuc;

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // DangHoatDong | SapDienRa | DaKetThuc

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "SapDienRa";
        if (dieuKienToiThieu == null) dieuKienToiThieu = BigDecimal.ZERO;
    }

    public BigDecimal getGiaTriGiamToiDa() {
        return this.soTienGiamToiDa;
    }

    public void setGiaTriGiamToiDa(BigDecimal giaTriGiamToiDa) {
        this.soTienGiamToiDa = giaTriGiamToiDa;
    }
}
