package com.example.Service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Phương thức thanh toán và thời điểm đơn được phát hành cho cộng tác viên (không cần CSDL). */
class HanNhanDonServiceTest {

    private static final LocalDateTime TAO = LocalDateTime.of(2026, 10, 16, 9, 0);

    @Test
    void chuanHoaHinhThucMacDinhTienMat() {
        assertEquals("TienMat", HanNhanDonService.chuanHoaHinhThuc(null));
        assertEquals("TienMat", HanNhanDonService.chuanHoaHinhThuc(" "));
        assertEquals("TienMat", HanNhanDonService.chuanHoaHinhThuc("TienMat"));
        assertEquals("ChuyenKhoan", HanNhanDonService.chuanHoaHinhThuc("chuyenkhoan"));
        assertThrows(IllegalArgumentException.class, () -> HanNhanDonService.chuanHoaHinhThuc("Momo"));
    }

    @Test
    void tienMatPhatHanhNgayKhiDat() {
        assertEquals(TAO, HanNhanDonService.tinhLucPhatHanh(null, TAO, "TienMat", "ChuaThanhToan", null));
    }

    @Test
    void chuyenKhoanChuaTraThiChuaPhatHanh() {
        assertNull(HanNhanDonService.tinhLucPhatHanh(TAO, TAO, "ChuyenKhoan", "ChuaThanhToan", null));
    }

    @Test
    void chuyenKhoanDaTraThiPhatHanhLucThanhToan() {
        assertEquals(TAO.plusMinutes(6),
                HanNhanDonService.tinhLucPhatHanh(TAO, TAO, "ChuyenKhoan", "DaThanhToan", TAO.plusMinutes(6)));
    }

    @Test
    void donBiTraLaiSauKhiTraTienThiTinhTuLanChoDuyetMoi() {
        assertEquals(TAO.plusHours(2),
                HanNhanDonService.tinhLucPhatHanh(TAO.plusHours(2), TAO, "ChuyenKhoan", "DaThanhToan", TAO.plusMinutes(6)));
    }

    @Test
    void khongCoHoaDonThiTinhTuLucTao() {
        assertEquals(TAO, HanNhanDonService.tinhLucPhatHanh(null, TAO, null, null, null));
    }
}
