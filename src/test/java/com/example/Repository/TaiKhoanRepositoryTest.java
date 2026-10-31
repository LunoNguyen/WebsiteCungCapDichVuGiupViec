package com.example.Repository;

import com.example.Model.TaiKhoan;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaiKhoanRepositoryTest {

    private static TaiKhoan tk(int id, String loai) {
        return TaiKhoan.builder().id(id).loaiTaiKhoan(loai).soDienThoai("0912000111").tenDangNhap("0912000111").build();
    }

    @Test
    void trungTheoLoai_khachHangVaCtvDuocTrungSo() {
        List<TaiKhoan> ds = List.of(tk(1, "KhachHang"));
        assertFalse(TaiKhoanRepository.trungTheoLoai(ds, "CongTacVien", null));
        assertTrue(TaiKhoanRepository.trungTheoLoai(ds, "KhachHang", null));
    }

    @Test
    void trungTheoLoai_nhanVienKhongDuocTrungVoiAi() {
        assertTrue(TaiKhoanRepository.trungTheoLoai(List.of(tk(1, "NhanVien")), "CongTacVien", null));
        assertTrue(TaiKhoanRepository.trungTheoLoai(List.of(tk(1, "KhachHang")), "NhanVien", null));
    }

    @Test
    void trungTheoLoai_boQuaChinhTaiKhoanDangSua() {
        List<TaiKhoan> ds = List.of(tk(5, "CongTacVien"));
        assertFalse(TaiKhoanRepository.trungTheoLoai(ds, "CongTacVien", 5));
        assertTrue(TaiKhoanRepository.trungTheoLoai(ds, "CongTacVien", 6));
    }
}
