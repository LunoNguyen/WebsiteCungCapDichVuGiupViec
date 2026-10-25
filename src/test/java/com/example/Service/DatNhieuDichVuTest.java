package com.example.Service;

import com.example.Model.DichVu;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Đặt nhiều dịch vụ trong một đơn: chỉ theo lần, tối đa 5 dịch vụ, tổng ≤ 8 giờ, xong trước 20:00. */
class DatNhieuDichVuTest {

    private static DichVu dv(String ten, String loai, String trangThai, Integer phut) {
        return DichVu.builder().tenDichVu(ten).loaiHinhDat(loai).trangThai(trangThai).thoiGianThucHien(phut).build();
    }

    private static List<DichVu> nCai(int n, int phut) {
        List<DichVu> ds = new ArrayList<>();
        for (int i = 0; i < n; i++) ds.add(dv("DV" + i, "TheoLan", "HoatDong", phut));
        return ds;
    }

    @Test
    void danhSachRongBaoLoi() {
        assertEquals("Vui lòng chọn ít nhất một dịch vụ.", DatNhieuDichVu.loiDanhSach(List.of()));
    }

    @Test
    void quaNamDichVuBaoLoi() {
        assertEquals("Mỗi lần đặt tối đa 5 dịch vụ.", DatNhieuDichVu.loiDanhSach(nCai(6, 60)));
    }

    @Test
    void goiThangKhongDatChung() {
        assertEquals("Dịch vụ \"Gói A\" là gói tháng, vui lòng đặt riêng.",
                DatNhieuDichVu.loiDanhSach(List.of(dv("Gói A", "GoiThang", "HoatDong", 60), dv("B", "TheoLan", "HoatDong", 60))));
    }

    @Test
    void dichVuDaAnBaoLoi() {
        assertEquals("Dịch vụ \"B\" hiện không còn nhận đặt.",
                DatNhieuDichVu.loiDanhSach(List.of(dv("A", "TheoLan", "HoatDong", 60), dv("B", "TheoLan", "An", 60))));
    }

    @Test
    void tongQuaTamGioBaoLoi() {
        assertEquals("Tổng thời lượng tối đa 8 giờ (đang chọn 8 giờ 30 phút).",
                DatNhieuDichVu.loiDanhSach(List.of(dv("A", "TheoLan", "HoatDong", 240), dv("B", "TheoLan", "HoatDong", 270))));
    }

    @Test
    void hopLe() {
        assertNull(DatNhieuDichVu.loiDanhSach(List.of(dv("A", "TheoLan", "HoatDong", 120), dv("B", "TheoLan", "HoatDong", null))));
    }

    @Test
    void thieuThoiGianTinh120() {
        assertEquals(240, DatNhieuDichVu.tongPhut(List.of(dv("A", "TheoLan", "HoatDong", 120), dv("B", "TheoLan", "HoatDong", null))));
    }

    @Test
    void gioKetThucCongDon() {
        assertEquals(LocalTime.of(10, 45), DatNhieuDichVu.gioKetThuc(LocalTime.of(8, 0), 165));
    }

    @Test
    void ketThucDung20GioHopLe() {
        assertNull(DatNhieuDichVu.loiGioKetThuc(LocalTime.of(17, 0), 180));
    }

    @Test
    void ketThucSau20GioBaoLoi() {
        assertEquals("Các dịch vụ phải xong trước 20:00 (dự kiến xong lúc 20:30). Vui lòng chọn giờ sớm hơn.",
                DatNhieuDichVu.loiGioKetThuc(LocalTime.of(17, 0), 210));
    }
}
