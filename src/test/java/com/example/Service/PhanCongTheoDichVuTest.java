package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DichVu;
import com.example.Model.PhanCongCTV;
import org.junit.jupiter.api.Test;

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
}
