package com.example.Service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Ràng buộc ngày đặt dịch vụ: theo lần trong 1 tháng, gói tháng bắt đầu trong 3 tháng. */
class RangBuocNgayDatTest {

    private static final LocalDateTime BAY_GIO = LocalDateTime.of(2026, 1, 31, 8, 0);
    private static final LocalTime CHIN_GIO = LocalTime.of(9, 0);

    @Test
    void ngayToiDaTinhTheoThangKeCaCuoiThang() {
        assertEquals(LocalDate.of(2026, 2, 28), RangBuocNgayDat.ngayToiDa("TheoLan", LocalDate.of(2026, 1, 31)));
        assertEquals(LocalDate.of(2026, 4, 30), RangBuocNgayDat.ngayToiDa("GoiThang", LocalDate.of(2026, 1, 31)));
    }

    @Test
    void theoLanChiTrongVong1Thang() {
        assertNull(RangBuocNgayDat.loiNgayDat("TheoLan", LocalDate.of(2026, 2, 28), CHIN_GIO, BAY_GIO));
        String loi = RangBuocNgayDat.loiNgayDat("TheoLan", LocalDate.of(2026, 3, 1), CHIN_GIO, BAY_GIO);
        assertNotNull(loi);
        assertTrue(loi.contains("trong vòng 1 tháng"), loi);
    }

    @Test
    void goiThangBatDauTrongVong3Thang() {
        assertNull(RangBuocNgayDat.loiNgayDat("GoiThang", LocalDate.of(2026, 4, 30), CHIN_GIO, BAY_GIO));
        String loi = RangBuocNgayDat.loiNgayDat("GoiThang", LocalDate.of(2026, 5, 1), CHIN_GIO, BAY_GIO);
        assertNotNull(loi);
        assertTrue(loi.contains("3 tháng"), loi);
    }

    @Test
    void khongChoQuaKhuVaCanSauItNhat1Gio() {
        assertTrue(RangBuocNgayDat.loiNgayDat("TheoLan", LocalDate.of(2026, 1, 30), CHIN_GIO, BAY_GIO).contains("quá khứ"));
        assertTrue(RangBuocNgayDat.loiNgayDat("TheoLan", LocalDate.of(2026, 1, 31), LocalTime.of(8, 30), BAY_GIO)
                .contains("ít nhất 1 giờ"));
        assertNull(RangBuocNgayDat.loiNgayDat("TheoLan", LocalDate.of(2026, 1, 31), CHIN_GIO, BAY_GIO));
    }

    @Test
    void thieuNgayHoacGio() {
        assertTrue(RangBuocNgayDat.loiNgayDat("TheoLan", null, CHIN_GIO, BAY_GIO).contains("chọn ngày và giờ"));
        assertTrue(RangBuocNgayDat.loiNgayDat("TheoLan", LocalDate.of(2026, 2, 1), null, BAY_GIO).contains("chọn ngày và giờ"));
    }
}
