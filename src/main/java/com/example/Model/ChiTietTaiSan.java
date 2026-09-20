package com.example.Model;

import jakarta.persistence.*;
import lombok.*;

/** Bang 26: ChiTietTaiSan */
@Entity @Table(name = "ChiTietTaiSan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChiTietTaiSan {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bieuMauId", nullable = false) private BieuMauTaiSan bieuMau;
    @Column(name = "KhuVucVatDung", nullable = false, length = 200) private String khuVucVatDung;
    @Column(name = "MoTaHienTrang", nullable = false, columnDefinition = "TEXT") private String moTaHienTrang;
    @Column(name = "HinhAnh", length = 500) private String hinhAnh;
}
