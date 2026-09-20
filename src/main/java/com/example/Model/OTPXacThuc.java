package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 3: OTPXacThuc */
@Entity @Table(name = "OTPXacThuc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OTPXacThuc {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taiKhoanId", nullable = false)
    private TaiKhoan taiKhoan;

    @Column(name = "MucDich", nullable = false, length = 50)
    private String mucDich; // DangKy | DatLaiMatKhau | XacNhanGD

    @Column(name = "MaCode", nullable = false, length = 10)
    private String maCode;

    @Column(name = "ThoiGianTao", nullable = false, updatable = false)
    private LocalDateTime thoiGianTao;

    @Column(name = "ThoiGianHetHan", nullable = false)
    private LocalDateTime thoiGianHetHan;

    @Column(name = "DaSuDung", nullable = false)
    private Boolean daSuDung;

    @PrePersist
    protected void onCreate() {
        if (thoiGianTao == null) thoiGianTao = LocalDateTime.now();
        if (daSuDung == null) daSuDung = false;
    }
}
