package com.example.Model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Bang 1: TaiKhoan */
@Entity @Table(name = "TaiKhoan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TaiKhoan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "MaTaiKhoan", nullable = false, unique = true, length = 20)
    private String maTaiKhoan;

    @Column(name = "TenDangNhap", nullable = false, unique = true, length = 100)
    private String tenDangNhap;

    @Column(name = "MatKhau", nullable = false, length = 255)
    private String matKhau;

    @Column(name = "Email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "SoDienThoai", nullable = false, unique = true, length = 15)
    private String soDienThoai;

    @Column(name = "LoaiTaiKhoan", nullable = false, length = 20)
    private String loaiTaiKhoan; // KhachHang | CongTacVien | NhanVien

    @Column(name = "TrangThai", nullable = false, length = 20)
    private String trangThai; // HoatDong | BiKhoa | ChoDuyet

    @Column(name = "NgayTao", nullable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "NgayCapNhat")
    private LocalDateTime ngayCapNhat;

    @PrePersist
    protected void onCreate() {
        ngayTao = LocalDateTime.now();
        if (trangThai == null) trangThai = "HoatDong";
    }

    @PreUpdate
    protected void onUpdate() { ngayCapNhat = LocalDateTime.now(); }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getMaTaiKhoan() { return maTaiKhoan; }
    public void setMaTaiKhoan(String maTaiKhoan) { this.maTaiKhoan = maTaiKhoan; }
    public String getTenDangNhap() { return tenDangNhap; }
    public void setTenDangNhap(String tenDangNhap) { this.tenDangNhap = tenDangNhap; }
    public String getMatKhau() { return matKhau; }
    public void setMatKhau(String matKhau) { this.matKhau = matKhau; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSoDienThoai() { return soDienThoai; }
    public void setSoDienThoai(String soDienThoai) { this.soDienThoai = soDienThoai; }
    public String getLoaiTaiKhoan() { return loaiTaiKhoan; }
    public void setLoaiTaiKhoan(String loaiTaiKhoan) { this.loaiTaiKhoan = loaiTaiKhoan; }
    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }
    public LocalDateTime getNgayTao() { return ngayTao; }
    public void setNgayTao(LocalDateTime ngayTao) { this.ngayTao = ngayTao; }
    public LocalDateTime getNgayCapNhat() { return ngayCapNhat; }
    public void setNgayCapNhat(LocalDateTime ngayCapNhat) { this.ngayCapNhat = ngayCapNhat; }
}
