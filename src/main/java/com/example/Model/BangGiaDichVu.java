package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Bang 17: BangGiaDichVu */
@Entity @Table(name = "BangGiaDichVu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BangGiaDichVu {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaBangGia", nullable = false, unique = true, length = 20)
    private String maBangGia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dichVuId", nullable = false)
    private DichVu dichVu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khuVucId")
    private KhuVuc khuVuc; // NULL = ap dung toan quoc

    @Column(name = "LoaiHinhDat", nullable = false, length = 20)
    private String loaiHinhDat; // TheoLan | GoiThang

    @Column(name = "DonViTinh", nullable = false, length = 30)
    private String donViTinh;

    @Column(name = "DonGia", nullable = false, precision = 12, scale = 0)
    private BigDecimal donGia;

    @Column(name = "NgayApDung", nullable = false)
    private LocalDate ngayApDung;

    @Column(name = "NgayKetThuc")
    private LocalDate ngayKetThuc; // NULL = khong gioi han

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // DangApDung | HetHan

    @PrePersist
    protected void onCreate() {
        if (trangThai == null) trangThai = "DangApDung";
    }
}
