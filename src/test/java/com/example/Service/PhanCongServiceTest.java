package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DichVu;
import com.example.Model.DonDatDichVu;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Các buổi của đơn dùng để xếp lịch cộng tác viên (không cần CSDL). */
class PhanCongServiceTest {

    private static DonDatDichVu don(String loaiHinh, String thu, Integer soBuoi) {
        DichVu dv = new DichVu();
        dv.setTenDichVu("Gói dọn dẹp");
        dv.setSoBuoi(soBuoi);
        ChiTietDonDat ct = new ChiTietDonDat();
        ct.setDichVu(dv);
        ct.setNgayThucHienTrongTuan(thu);
        DonDatDichVu d = new DonDatDichVu();
        d.setLoaiHinhDat(loaiHinh);
        d.setNgayThucHien(LocalDate.of(2026, 10, 12)); // Thứ 2
        d.setGioBatDau(LocalTime.of(8, 0));
        d.setChiTietList(new ArrayList<>(List.of(ct)));
        return d;
    }

    @Test
    void donTheoLanChiCoMotBuoi() {
        assertEquals(List.of(LocalDate.of(2026, 10, 12)), PhanCongService.cacBuoi(don("TheoLan", null, null)));
    }

    @Test
    void goiThangCoDuCacBuoiTheoThu() {
        List<LocalDate> buoi = PhanCongService.cacBuoi(don("GoiThang", "2,4,6", 12));
        assertEquals(12, buoi.size());
        assertEquals(LocalDate.of(2026, 10, 12), buoi.get(0));
        assertEquals(LocalDate.of(2026, 10, 14), buoi.get(1));
        assertEquals(LocalDate.of(2026, 11, 6), buoi.get(11));
    }

    @Test
    void goiThangTinhTuNgayBatDauKhac() {
        List<LocalDate> buoi = PhanCongService.cacBuoi(don("GoiThang", "3,5", 4), LocalDate.of(2026, 10, 20));
        assertEquals(List.of(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 22),
                LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 29)), buoi);
    }
}
