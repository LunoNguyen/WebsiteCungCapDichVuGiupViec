package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DichVu;
import com.example.Model.DonDatDichVu;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Quy tắc tự hủy đơn quá hạn (không cần CSDL). */
class TuHuyDonServiceTest {

    private static final LocalDate NGAY = LocalDate.of(2026, 10, 12); // Thứ 2

    private static DonDatDichVu don(String loaiHinh, String thu, Integer soBuoi) {
        DichVu dv = new DichVu();
        dv.setTenDichVu("Dọn dẹp");
        dv.setSoBuoi(soBuoi);
        ChiTietDonDat ct = new ChiTietDonDat();
        ct.setDichVu(dv);
        ct.setNgayThucHienTrongTuan(thu);
        DonDatDichVu d = new DonDatDichVu();
        d.setLoaiHinhDat(loaiHinh);
        d.setNgayThucHien(NGAY);
        d.setGioBatDau(LocalTime.of(8, 0));
        d.setGioKetThuc(LocalTime.of(11, 0));
        d.setChiTietList(new ArrayList<>(List.of(ct)));
        return d;
    }

    @Test
    void chuaAiNhanThiHuyKhiQuaGioBatDau() {
        DonDatDichVu d = don("TheoLan", null, null);
        assertNull(TuHuyDonService.lyDoQuaHan(d, false, false, NGAY.atTime(8, 0)));          // đúng giờ: chưa hủy
        assertNotNull(TuHuyDonService.lyDoQuaHan(d, false, false, NGAY.atTime(8, 1)));       // quá giờ bắt đầu
    }

    @Test
    void daNhanNhungChuaLamThiHuyKhiQuaGioKetThuc() {
        DonDatDichVu d = don("TheoLan", null, null);
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, false, NGAY.atTime(10, 59)));         // CTV có thể đang tới muộn
        assertNotNull(TuHuyDonService.lyDoQuaHan(d, true, false, NGAY.atTime(11, 1)));
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, true, NGAY.atTime(23, 0)));           // đã làm: không hủy
    }

    @Test
    void goiThangTinhTheoBuoiCuoi() {
        DonDatDichVu d = don("GoiThang", "2,4,6", 12);                                         // buổi cuối 06/11
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, false, LocalDateTime.of(2026, 10, 20, 12, 0)));
        assertNotNull(TuHuyDonService.lyDoQuaHan(d, true, false, LocalDateTime.of(2026, 11, 6, 11, 1)));
        // Chưa ai nhận gói: quá giờ buổi đầu là hủy cả gói
        assertNotNull(TuHuyDonService.lyDoQuaHan(d, false, false, NGAY.atTime(8, 30)));
    }
}
