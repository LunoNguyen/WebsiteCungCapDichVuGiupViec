package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 24: PhanCongCTV */
@EntityListeners(com.example.SuKien.DonDatListener.class)
@Entity @Table(name = "PhanCongCTV", uniqueConstraints = @UniqueConstraint(columnNames = {"donDatId", "congTacVienId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PhanCongCTV {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    @Column(name = "MaPhanCong", nullable = false, unique = true, length = 20) private String maPhanCong;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "donDatId", nullable = false) private DonDatDichVu donDat;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "congTacVienId", nullable = false) private CongTacVien congTacVien;
    /** Dịch vụ (dòng chi tiết) CTV này làm trong đơn nhiều dịch vụ; null = cả đơn (đơn một dịch vụ, dữ liệu cũ). */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "chiTietDonDatId") private ChiTietDonDat chiTietDonDat;
    @Column(name = "TrangThai", nullable = false, length = 20) private String trangThai;
    @Column(name = "ThoiGianPhanCong", nullable = false, updatable = false) private LocalDateTime thoiGianPhanCong;
    @Column(name = "ThoiGianXacNhan") private LocalDateTime thoiGianXacNhan;
    @Column(name = "LyDoTuChoi", columnDefinition = "TEXT") private String lyDoTuChoi;
    /** Trạng thái lúc tải / lưu gần nhất (chỉ trong bộ nhớ) – xem DonDatListener. */
    @Transient @com.fasterxml.jackson.annotation.JsonIgnore private String trangThaiGoc;
    public void chupTrangThai() { trangThaiGoc = trangThai; }
    @PrePersist protected void onCreate() { if (trangThai == null) trangThai = "ChoPhanCong"; if (thoiGianPhanCong == null) thoiGianPhanCong = LocalDateTime.now(); }
}
