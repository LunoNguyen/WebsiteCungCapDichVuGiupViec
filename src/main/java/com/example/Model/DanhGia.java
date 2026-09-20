package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 34: DanhGia */
@Entity @Table(name = "DanhGia")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DanhGia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaDanhGia", nullable = false, unique = true, length = 20) private String maDanhGia;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false, unique = true) private DonDatDichVu donDat;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khachHangId", nullable = false) private KhachHang khachHang;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "congTacVienId", nullable = false) private CongTacVien congTacVien;
    @Column(name = "DiemChatLuong", nullable = false) private Integer diemChatLuong;
    @Column(name = "DiemThaiDo", nullable = false) private Integer diemThaiDo;
    @Column(name = "NhanXet", columnDefinition = "TEXT") private String nhanXet;
    @Column(name = "NgayDanhGia", nullable = false, updatable = false) private LocalDateTime ngayDanhGia;
    @Column(name = "TrangThai", nullable = false, length = 20) private String trangThai;
    @PrePersist protected void onCreate() { if (ngayDanhGia == null) ngayDanhGia = LocalDateTime.now(); if (trangThai == null) trangThai = "ChoDuyet"; }
}
