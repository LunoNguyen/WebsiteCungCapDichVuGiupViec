package com.example.Repository;

import com.example.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Số điện thoại / tên đăng nhập chỉ duy nhất theo từng loại tài khoản (uq_TK_sdt_loai, uq_TK_tenDN_loai):
 * một số có thể có cả tài khoản khách hàng lẫn tài khoản cộng tác viên, nên tra theo số trả về danh sách.
 */
@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {
    List<TaiKhoan> findAllByTenDangNhap(String tenDangNhap);
    List<TaiKhoan> findAllBySoDienThoai(String soDienThoai);
    Optional<TaiKhoan> findByEmail(String email);

    /** Mọi tài khoản đăng nhập được bằng chuỗi này (tên đăng nhập hoặc số điện thoại), không trùng lặp. */
    default List<TaiKhoan> timTheoDinhDanh(String dinhDanh) {
        java.util.LinkedHashMap<Integer, TaiKhoan> kq = new java.util.LinkedHashMap<>();
        findAllByTenDangNhap(dinhDanh).forEach(t -> kq.put(t.getId(), t));
        findAllBySoDienThoai(dinhDanh).forEach(t -> kq.put(t.getId(), t));
        return List.copyOf(kq.values());
    }

    /**
     * Trong [ds] (cùng số điện thoại hoặc cùng tên đăng nhập) có tài khoản khác [boQuaId] xung đột với tài khoản
     * loại [loai] không: cùng loại, hoặc một bên là nhân viên (khách hàng và CTV được trùng số với nhau).
     */
    static boolean trungTheoLoai(List<TaiKhoan> ds, String loai, Integer boQuaId) {
        return ds.stream().anyMatch(t -> !t.getId().equals(boQuaId)
                && (t.getLoaiTaiKhoan() == null || loai == null
                    || t.getLoaiTaiKhoan().equalsIgnoreCase(loai)
                    || "NhanVien".equalsIgnoreCase(t.getLoaiTaiKhoan()) || "NhanVien".equalsIgnoreCase(loai)));
    }
}
