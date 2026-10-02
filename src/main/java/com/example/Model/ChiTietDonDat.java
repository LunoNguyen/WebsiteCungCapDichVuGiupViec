package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/** Chi tiết đơn đặt: 1 đơn có nhiều dịch vụ */
@Entity @Table(name = "ChiTietDonDat")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietDonDat {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "donDatId", nullable = false)
    private DonDatDichVu donDat;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dichVuId", nullable = false)
    private DichVu dichVu;

    // Bản ghi giá áp dụng lúc đặt (có thể null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bangGiaId")
    private BangGiaDichVu bangGia;

    @Column(name = "SoLuong", nullable = false)
    @Builder.Default
    private Integer soLuong = 1;

    // Đơn giá chốt tại thời điểm đặt
    @Column(name = "DonGia", nullable = false, precision = 12, scale = 0)
    private BigDecimal donGia;

    @Column(name = "ThanhTien", nullable = false, precision = 12, scale = 0)
    private BigDecimal thanhTien;

    @Column(name = "GhiChu", columnDefinition = "TEXT")
    private String ghiChu;

    // Ngày trong tuần, vd: 2,4,6. Null nếu đặt theo lần
    @Column(name = "NgayThucHienTrongTuan", length = 50)
    private String ngayThucHienTrongTuan;

    @PrePersist
    protected void onCreate() {
        if (soLuong == null) soLuong = 1;
        if (thanhTien == null && donGia != null)
            thanhTien = donGia.multiply(BigDecimal.valueOf(soLuong));
    }
}