package com.example.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Thông báo đơn mới cho CSKH: loại sự kiện và dòng ghi chú theo hình thức thanh toán. */
class ThongBaoDonCskhTest {

    @Test
    void donTienMatMoiTaoLaDonMoiCanTimCtv() {
        assertArrayEquals(new String[]{"don-moi", "Tiền mặt – đang tìm cộng tác viên"},
                RealtimeSuKienDonService.thongBaoCskh(true, "TienMat", false));
    }

    @Test
    void donChuyenKhoanMoiTaoThiChoThanhToan() {
        assertArrayEquals(new String[]{"don-moi", "Chuyển khoản – chờ khách thanh toán"},
                RealtimeSuKienDonService.thongBaoCskh(true, "ChuyenKhoan", false));
    }

    @Test
    void chuyenKhoanVuaTraXongThiBaoDaThanhToan() {
        assertArrayEquals(new String[]{"da-thanh-toan", "Đã chuyển khoản – đang tìm cộng tác viên"},
                RealtimeSuKienDonService.thongBaoCskh(false, "ChuyenKhoan", true));
    }

    @Test
    void khongPhaiDonMoiCungChuaThanhToanThiKhongBao() {
        assertNull(RealtimeSuKienDonService.thongBaoCskh(false, "TienMat", false));
        assertNull(RealtimeSuKienDonService.thongBaoCskh(false, "ChuyenKhoan", false));
    }

    @Test
    void thieuHinhThucThanhToanCoiNhuTienMat() {
        assertEquals("don-moi", RealtimeSuKienDonService.thongBaoCskh(true, null, false)[0]);
    }
}
