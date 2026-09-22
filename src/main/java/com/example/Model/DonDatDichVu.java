package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

/** Bang 21: DonDatDichVu */
@Entity @Table(name = "DonDatDichVu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonDatDichVu {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaDonDat", nullable = false, unique = true, length = 20) private String maDonDat;
    
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khachHangId", nullable = false) private KhachHang khachHang;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "diaChiId", nullable = false) private DiaChiKhachHang diaChi;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "nhanVienTiepNhanId") private NhanVien nhanVienTiepNhan;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "dichVuId", nullable = false) private DichVu dichVu;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "bangGiaId") private BangGiaDichVu bangGia;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "goiDichVuId") private GoiDichVu goiDichVu;
    
    // --- ĐỔI TÊN KHÓA NGOẠI: couponId -> khuyenMaiId ---
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "khuyenMaiId") private MaKhuyenMai khuyenMai;
    
    @Column(name = "LoaiHinhDat", nullable = false, length = 20) private String loaiHinhDat;
    @Column(name = "NgayThucHien", nullable = false) private LocalDate ngayThucHien;
    @Column(name = "GioBatDau", nullable = false) private LocalTime gioBatDau;
    @Column(name = "GioKetThuc") private LocalTime gioKetThuc;
    @Column(name = "YeuCauDacBiet", columnDefinition = "TEXT") private String yeuCauDacBiet;
    @Column(name = "ChiPhiGoc", nullable = false, precision = 12, scale = 0) private BigDecimal chiPhiGoc;
    @Column(name = "SoTienGiam", nullable = false, precision = 12, scale = 0) private BigDecimal soTienGiam;
    @Column(name = "ThanhTien", nullable = false, precision = 12, scale = 0) private BigDecimal thanhTien;
    @Column(name = "TrangThai", nullable = false, length = 30) private String trangThai;
    @Column(name = "NgayTao", nullable = false, updatable = false) private LocalDateTime ngayTao;
    @Column(name = "GhiChu", columnDefinition = "TEXT") private String ghiChu;
    
    @PrePersist protected void onCreate() { 
        if (soTienGiam == null) soTienGiam = BigDecimal.ZERO; 
        if (trangThai == null) trangThai = "ChoDuyet"; 
        if (ngayTao == null) ngayTao = LocalDateTime.now(); 
    }
}