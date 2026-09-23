package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Bang 22: LichSuSuDungKhuyenMai */
@Entity @Table(name = "LichSuSuDungKhuyenMai", uniqueConstraints = @UniqueConstraint(columnNames = {"khuyenMaiId", "donDatId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LichSuSuDungKhuyenMai {
    
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Integer id;
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "khuyenMaiId", nullable = false) 
    private MaKhuyenMai khuyenMai;
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "khachHangId", nullable = false) 
    private KhachHang khachHang;
    
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "donDatId", nullable = false) 
    private DonDatDichVu donDat;
    
    @Column(name = "NgaySuDung", nullable = false, updatable = false) 
    private LocalDateTime ngaySuDung;
    
    @Column(name = "SoTienDuocGiam", nullable = false, precision = 12, scale = 0) 
    private BigDecimal soTienDuocGiam;
    
    @PrePersist 
    protected void onCreate() { 
        if (ngaySuDung == null) ngaySuDung = LocalDateTime.now(); 
    }
}