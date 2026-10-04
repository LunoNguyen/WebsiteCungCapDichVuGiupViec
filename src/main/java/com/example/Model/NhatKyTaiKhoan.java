package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 2: NhatKyTaiKhoan */
@Entity @Table(name = "NhatKyTaiKhoan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NhatKyTaiKhoan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taiKhoanId", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "HanhDong", nullable = false, length = 100)
    private String hanhDong;

    @Column(name = "DiaChiIP", length = 45)
    private String diaChiIP;

    @Column(name = "ThietBi", length = 200)
    private String thietBi;

    @Column(name = "ThoiGian", nullable = false, updatable = false)
    private LocalDateTime thoiGian;

    @Column(name = "KetQua", nullable = false, length = 20)
    private String ketQua; // ThanhCong | ThatBai

    @PrePersist
    protected void onCreate() {
        if (thoiGian == null) thoiGian = LocalDateTime.now();
        // Cắt theo độ dài cột để một dòng nhật ký dài không làm hỏng cả thao tác nghiệp vụ
        hanhDong = catNgan(hanhDong, 100);
        diaChiIP = catNgan(diaChiIP, 45);
        thietBi = catNgan(thietBi, 200);
    }

    private static String catNgan(String s, int toiDa) {
        return s != null && s.length() > toiDa ? s.substring(0, toiDa - 1) + "…" : s;
    }
}
