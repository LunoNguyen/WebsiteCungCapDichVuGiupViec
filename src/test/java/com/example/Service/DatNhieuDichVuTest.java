package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.ChuongTrinhKhuyenMai;
import com.example.Model.DonDatDichVu;
import com.example.Model.DichVu;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Đặt nhiều dịch vụ trong một đơn: chỉ theo lần, tối đa 5 dịch vụ, làm song song cùng giờ bắt đầu, xong trước 20:00. */
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
    void khongGioiHanTongThoiLuong() {
        // Các dịch vụ làm song song từ cùng giờ bắt đầu: tổng 8 giờ 30 phút vẫn đặt được
        assertNull(DatNhieuDichVu.loiDanhSach(List.of(dv("A", "TheoLan", "HoatDong", 240), dv("B", "TheoLan", "HoatDong", 270))));
    }

    @Test
    void donXongTheoDichVuLauNhat() {
        assertEquals(240, DatNhieuDichVu.phutDaiNhat(List.of(dv("A", "TheoLan", "HoatDong", 180), dv("B", "TheoLan", "HoatDong", 240))));
    }

    @Test
    void ctvNhanNhieuDichVuLamNoiTiep() {
        assertEquals(LocalTime.of(14, 0), DatNhieuDichVu.ketThucCuaCtv(LocalTime.of(9, 0),
                List.of(dv("A", "TheoLan", "HoatDong", 180), dv("B", "TheoLan", "HoatDong", 120))));
        assertEquals(LocalTime.of(12, 0), DatNhieuDichVu.ketThucCuaCtv(LocalTime.of(9, 0),
                List.of(dv("A", "TheoLan", "HoatDong", 180))));
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

    private static ChuongTrinhKhuyenMai ct(String loai, String giaTri, String toiDa) {
        return ChuongTrinhKhuyenMai.builder().loaiGiam(loai).giaTriGiam(new BigDecimal(giaTri))
                .soTienGiamToiDa(toiDa == null ? null : new BigDecimal(toiDa)).build();
    }

    @Test
    void congGiaCacDong() {
        assertEquals(new BigDecimal("411000"), DatNhieuDichVu.tongGoc(List.of(
                new DatNhieuDichVu.DongGia(1, "A", new BigDecimal("216000")),
                new DatNhieuDichVu.DongGia(2, "B", new BigDecimal("195000")))));
    }

    @Test
    void giamPhanTramCoTran() {
        assertEquals(new BigDecimal("50000"), DatNhieuDichVu.soTienGiam(ct("PhanTram", "20", "50000"), new BigDecimal("411000")));
    }

    @Test
    void giamTienKhongQuaTong() {
        assertEquals(new BigDecimal("100000"), DatNhieuDichVu.soTienGiam(ct("SoTienCoDinh", "150000", null), new BigDecimal("100000")));
    }

    @Test
    void tomTatMotDichVu() {
        assertEquals("Dọn nhà", DatNhieuDichVu.tomTatDichVu(List.of("Dọn nhà")));
    }

    @Test
    void tomTatHaiDichVu() {
        assertEquals("Dọn nhà + Giặt rèm", DatNhieuDichVu.tomTatDichVu(List.of("Dọn nhà", "Giặt rèm")));
    }

    @Test
    void tomTatNhieuDichVu() {
        assertEquals("Dọn nhà + 2 dịch vụ khác", DatNhieuDichVu.tomTatDichVu(List.of("Dọn nhà", "Giặt rèm", "Máy lạnh")));
    }

    @Test
    void tenDichVuCuaDonGhepCacDong() {
        DonDatDichVu don = DonDatDichVu.builder().build();
        don.setChiTietList(new ArrayList<>(List.of(
                ChiTietDonDat.builder().dichVu(dv("Dọn nhà", "TheoLan", "HoatDong", 120)).build(),
                ChiTietDonDat.builder().dichVu(dv("Giặt rèm", "TheoLan", "HoatDong", 60)).build())));
        assertEquals("Dọn nhà + Giặt rèm", DatNhieuDichVu.tenDichVuCuaDon(don, "Dịch vụ"));
    }

    @Test
    void tenDichVuCuaDonKhongChiTietDungMacDinh() {
        assertEquals("Dịch vụ", DatNhieuDichVu.tenDichVuCuaDon(DonDatDichVu.builder().build(), "Dịch vụ"));
    }
}
