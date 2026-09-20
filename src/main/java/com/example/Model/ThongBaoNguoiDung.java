package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 37: ThongBaoNguoiDung */
@Entity @Table(name = "ThongBaoNguoiDung", uniqueConstraints = @UniqueConstraint(columnNames = {"thongBaoId", "taiKhoanId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ThongBaoNguoiDung {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "thongBaoId", nullable = false) private ThongBao thongBao;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "taiKhoanId", nullable = false) private TaiKhoan taiKhoan;
    @Column(name = "DaDoc", nullable = false) private Boolean daDoc;
    @Column(name = "ThoiGianDoc") private LocalDateTime thoiGianDoc;
    @Column(name = "TrangThai", nullable = false, length = 20) private String trangThai;
    @PrePersist protected void onCreate() { if (daDoc == null) daDoc = false; if (trangThai == null) trangThai = "DaGui"; }
}
