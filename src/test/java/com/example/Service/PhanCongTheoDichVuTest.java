package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DichVu;
import com.example.Model.DonDatDichVu;
import com.example.Model.PhanCongCTV;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Đơn nhiều dịch vụ: mỗi dịch vụ (dòng chi tiết) một cộng tác viên nhận; đủ người khi mọi dòng có người. */
class PhanCongTheoDichVuTest {

    private static ChiTietDonDat ct(int id, int dichVuId) {
        return ChiTietDonDat.builder().id(id).dichVu(DichVu.builder().id(dichVuId).build()).build();
    }

    /** chiTietId null = phân công cả đơn (đơn một dịch vụ, dữ liệu cũ). */
    private static PhanCongCTV pc(Integer chiTietId, String trangThai) {
        return PhanCongCTV.builder().trangThai(trangThai)
                .chiTietDonDat(chiTietId == null ? null : ChiTietDonDat.builder().id(chiTietId).build()).build();
    }

    @Test
    void donMotDichVuCoNguoiNhanCaDonLaDuNguoi() {
        assertTrue(PhanCongTheoDichVu.duNguoi(List.of(ct(10, 1)), List.of(pc(null, "DaXacNhan"))));
    }

    @Test
    void phanCongTuChoiKhongTinh() {
        assertEquals(Set.of(10), PhanCongTheoDichVu.chiTietConTrong(List.of(ct(10, 1)), List.of(pc(null, "TuChoi"))));
    }

    @Test
    void nhanMotPhanChuaDuNguoi() {
        assertFalse(PhanCongTheoDichVu.duNguoi(List.of(ct(10, 1), ct(11, 2)), List.of(pc(10, "DaXacNhan"))));
    }

    @Test
    void moiDongCoNguoiLaDuNguoi() {
        assertTrue(PhanCongTheoDichVu.duNguoi(List.of(ct(10, 1), ct(11, 2)), List.of(pc(10, "DaXacNhan"), pc(11, "DaXacNhan"))));
    }

    @Test
    void nhanDongDaCoNguoiBaoLoi() {
        assertEquals("Dịch vụ bạn chọn vừa có người nhận. Vui lòng tải lại đơn.",
                PhanCongTheoDichVu.loiNhan(Set.of(10, 11), Set.of(11), Set.of(1, 2), Map.of(10, 1, 11, 2), List.of(10)));
    }

    @Test
    void nhanDongThieuKyNangBaoLoi() {
        assertEquals("Bạn chưa đăng ký kỹ năng cho dịch vụ đã chọn.",
                PhanCongTheoDichVu.loiNhan(Set.of(10, 11), Set.of(10, 11), Set.of(1), Map.of(10, 1, 11, 2), List.of(11)));
    }

    @Test
    void nhanDongKhongThuocDonBaoLoi() {
        assertEquals("Dịch vụ không thuộc đơn này.",
                PhanCongTheoDichVu.loiNhan(Set.of(10), Set.of(10), Set.of(1), Map.of(10, 1), List.of(99)));
    }

    @Test
    void nhanHopLe() {
        assertNull(PhanCongTheoDichVu.loiNhan(Set.of(10, 11), Set.of(10, 11), Set.of(1, 2), Map.of(10, 1, 11, 2), List.of(10, 11)));
    }

    @Test
    void macDinhNhanMoiDongConTrongCoKyNang() {
        assertEquals(List.of(10), PhanCongTheoDichVu.chonMacDinh(Set.of(10, 11), Set.of(1), Map.of(10, 1, 11, 2)));
    }

    @Test
    void donXongKhiMoiPhanCongHieuLucXong() {
        assertFalse(PhanCongTheoDichVu.moiPhanCongXong(List.of(pc(10, "HoanThanh"), pc(11, "DaXacNhan"))));
        assertTrue(PhanCongTheoDichVu.moiPhanCongXong(List.of(pc(10, "HoanThanh"), pc(11, "HoanThanh"), pc(11, "TuChoi"))));
    }

    @Test
    void tenDichVuCuaCtvChiLayDongCuaHo() {
        ChiTietDonDat a = ChiTietDonDat.builder().id(10).dichVu(DichVu.builder().id(1).tenDichVu("Dọn nhà").build()).build();
        ChiTietDonDat b = ChiTietDonDat.builder().id(11).dichVu(DichVu.builder().id(2).tenDichVu("Giặt rèm").build()).build();
        com.example.Model.DonDatDichVu don = com.example.Model.DonDatDichVu.builder().build();
        don.setChiTietList(new java.util.ArrayList<>(List.of(a, b)));
        PhanCongCTV cuaToi = PhanCongCTV.builder().trangThai("DaXacNhan").chiTietDonDat(b).build();
        assertEquals("Giặt rèm", PhanCongTheoDichVu.tenDichVuCuaCtv(don, List.of(cuaToi)));
        assertEquals("Dọn nhà + Giặt rèm", PhanCongTheoDichVu.tenDichVuCuaCtv(don, List.of(pc(null, "DaXacNhan"))));
    }

    // ---------- Tiền công theo dịch vụ CTV nhận ----------

    private static DonDatDichVu don2(long thanhTien, long gia1, long gia2) {
        DonDatDichVu d = DonDatDichVu.builder().id(1).thanhTien(BigDecimal.valueOf(thanhTien)).build();
        d.getChiTietList().add(ChiTietDonDat.builder().id(10).thanhTien(BigDecimal.valueOf(gia1)).build());
        d.getChiTietList().add(ChiTietDonDat.builder().id(11).thanhTien(BigDecimal.valueOf(gia2)).build());
        return d;
    }

    @Test
    void phanCongCaDonDuocCaTienDon() {
        DonDatDichVu d = don2(837000, 270000, 660000);
        assertEquals(BigDecimal.valueOf(837000), PhanCongTheoDichVu.tienCuaCtv(d, List.of(pc(null, "HoanThanh"))));
    }

    @Test
    void nhanMotDichVuChiDuocPhanCuaDichVuDoSauKhuyenMai() {
        DonDatDichVu d = don2(837000, 270000, 660000); // giảm 10% trên tổng 930.000đ
        assertEquals(0, BigDecimal.valueOf(243000).compareTo(PhanCongTheoDichVu.tienCuaCtv(d, List.of(pc(10, "HoanThanh")))));
        assertEquals(0, BigDecimal.valueOf(594000).compareTo(PhanCongTheoDichVu.tienCuaCtv(d, List.of(pc(11, "HoanThanh")))));
    }

    @Test
    void nhanCaHaiDichVuDuocCaDon() {
        DonDatDichVu d = don2(837000, 270000, 660000);
        assertEquals(0, BigDecimal.valueOf(837000).compareTo(
                PhanCongTheoDichVu.tienCuaCtv(d, List.of(pc(10, "HoanThanh"), pc(11, "HoanThanh")))));
    }

    @Test
    void phanCongTuChoiKhongDuocTien() {
        DonDatDichVu d = don2(837000, 270000, 660000);
        assertEquals(0, BigDecimal.ZERO.compareTo(PhanCongTheoDichVu.tienCuaCtv(d, List.of(pc(10, "TuChoi")))));
    }

    @Test
    void giaDongBangKhongThiChiaDeu() {
        DonDatDichVu d = don2(500000, 0, 0);
        assertEquals(0, BigDecimal.valueOf(250000).compareTo(PhanCongTheoDichVu.tienCuaCtv(d, List.of(pc(10, "HoanThanh")))));
    }
}
