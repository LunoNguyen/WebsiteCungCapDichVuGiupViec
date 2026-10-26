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
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, false, NGAY.atTime(11, 1)));            // đã có CTV nhận: xử lý qua "Xử lý quá hạn" của CTV, không tự hủy
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, true, NGAY.atTime(23, 0)));           // đã làm: không hủy
    }

    @Test
    void chuaAiNhanThiHuySau15PhutPhatHanh() {
        LocalDateTime phatHanh = LocalDateTime.of(2026, 10, 10, 9, 0);
        LocalDateTime han = phatHanh.plusMinutes(15);
        assertNull(TuHuyDonService.lyDoHetHanNhan(han, false, false, false, phatHanh.plusMinutes(14), 15));   // còn hạn
        assertNull(TuHuyDonService.lyDoHetHanNhan(han, false, false, false, han, 15));                         // đúng hạn: chưa hủy
        assertNotNull(TuHuyDonService.lyDoHetHanNhan(han, false, false, false, han.plusSeconds(1), 15));       // quá hạn
        assertNull(TuHuyDonService.lyDoHetHanNhan(han, true, false, false, han.plusHours(1), 15));             // đã có người nhận
        assertNull(TuHuyDonService.lyDoHetHanNhan(null, false, false, false, han.plusHours(1), 15));           // không rõ lúc phát hành
    }

    @Test
    void quaHanThanhToanThiHuy() {
        LocalDateTime tao = LocalDateTime.of(2026, 10, 16, 9, 0);
        assertNull(TuHuyDonService.lyDoQuaHanThanhToan("ChuyenKhoan", "ChuaThanhToan", tao, tao.plusMinutes(15), 15));
        String lyDo = TuHuyDonService.lyDoQuaHanThanhToan("ChuyenKhoan", "ChuaThanhToan", tao, tao.plusMinutes(15).plusSeconds(1), 15);
        assertNotNull(lyDo);
        assertTrue(lyDo.contains("chưa thanh toán"), lyDo);
        assertNull(TuHuyDonService.lyDoQuaHanThanhToan("ChuyenKhoan", "DaThanhToan", tao, tao.plusHours(1), 15));
        assertNull(TuHuyDonService.lyDoQuaHanThanhToan("TienMat", "ChuaThanhToan", tao, tao.plusHours(1), 15));
    }

    @Test
    void goiThangTinhTheoBuoiCuoi() {
        DonDatDichVu d = don("GoiThang", "2,4,6", 12);                                         // buổi cuối 06/11
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, false, LocalDateTime.of(2026, 10, 20, 12, 0)));
        assertNull(TuHuyDonService.lyDoQuaHan(d, true, false, LocalDateTime.of(2026, 11, 6, 11, 1)));   // đã có CTV nhận: không tự hủy
        // Chưa ai nhận gói: quá giờ buổi đầu là hủy cả gói
        assertNotNull(TuHuyDonService.lyDoQuaHan(d, false, false, NGAY.atTime(8, 30)));
    }

    @Test
    void nhanMotPhanHetHanThiHuyVoiLyDoChuaDu() {
        assertEquals("Hết 15 phút chờ (hạn 10:15 26/10/2026) nhưng chưa đủ cộng tác viên nhận đơn",
                TuHuyDonService.lyDoHetHanNhan(LocalDateTime.of(2026, 10, 26, 10, 15), false, true, false,
                        LocalDateTime.of(2026, 10, 26, 10, 16), 15));
    }

    @Test
    void duNguoiKhongHuy() {
        assertNull(TuHuyDonService.lyDoHetHanNhan(LocalDateTime.of(2026, 10, 26, 10, 15), true, true, false,
                LocalDateTime.of(2026, 10, 26, 10, 16), 15));
    }
}
