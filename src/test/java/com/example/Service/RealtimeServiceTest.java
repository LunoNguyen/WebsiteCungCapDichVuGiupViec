package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DichVu;
import com.example.Model.DiaChiKhachHang;
import com.example.Model.DonDatDichVu;
import com.example.Model.KhuVuc;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Kênh realtime quản trị: sự kiện có nội dung chỉ tới đúng vai trò, không lộ thông tin liên hệ khách. */
class RealtimeServiceTest {

    /** Emitter giả ghi lại tên các sự kiện được gửi. */
    static class EmitterGhi extends SseEmitter {
        final List<String> suKien = new ArrayList<>();

        EmitterGhi() {
            super(0L);
        }

        @Override
        public void send(SseEventBuilder builder) {
            for (var d : builder.build()) {
                String s = String.valueOf(d.getData());
                if (s.startsWith("event:")) suKien.add(s.substring(6).split("\n")[0].trim());
            }
        }
    }

    private static List<String> loai(EmitterGhi e, String ten) {
        return e.suKien.stream().filter(ten::equals).toList();
    }

    @Test
    void guiNhanVienChiToiDungVaiTro() {
        RealtimeService rt = new RealtimeService();
        EmitterGhi cskh = new EmitterGhi();
        EmitterGhi hcns = new EmitterGhi();
        rt.dangKy("ROLE_CSKH", cskh);
        rt.dangKy("ROLE_HCNS", hcns);

        rt.guiNhanVien(Set.of("ROLE_CSKH"), "don-moi", "{}");

        assertEquals(1, loai(cskh, "don-moi").size());
        assertEquals(0, loai(hcns, "don-moi").size());
    }

    @Test
    void phatSuKienVanToiMoiKetNoi() {
        RealtimeService rt = new RealtimeService();
        EmitterGhi cskh = new EmitterGhi();
        EmitterGhi hcns = new EmitterGhi();
        rt.dangKy("ROLE_CSKH", cskh);
        rt.dangKy("ROLE_HCNS", hcns);

        rt.phatSuKien("/x");

        assertEquals(1, loai(cskh, "du-lieu-thay-doi").size());
        assertEquals(1, loai(hcns, "du-lieu-thay-doi").size());
    }

    @Test
    void jsonDonKhongCoThongTinLienHeKhach() {
        KhuVuc kv = new KhuVuc();
        kv.setTenKhuVuc("Quận 1");
        DiaChiKhachHang dc = new DiaChiKhachHang();
        dc.setDiaChiChiTiet("12 Lê Lợi, Quận 1");
        dc.setKhuVuc(kv);
        DichVu dv = new DichVu();
        dv.setTenDichVu("Dọn dẹp nhà theo giờ (2 giờ)");
        ChiTietDonDat ct = new ChiTietDonDat();
        ct.setDichVu(dv);
        DonDatDichVu don = new DonDatDichVu();
        don.setId(7);
        don.setMaDonDat("DD-7");
        don.setDiaChi(dc);
        don.setNgayThucHien(LocalDate.of(2026, 10, 20));
        don.setGioBatDau(LocalTime.of(9, 0));
        don.setChiTietList(new ArrayList<>(List.of(ct)));

        String json = RealtimeService.jsonDon("don-moi", don, "ChuyenKhoan", false, "Chờ khách chuyển khoản");

        assertTrue(json.contains("\"maDonDat\":\"DD-7\""), json);
        assertTrue(json.contains("\"khuVuc\":\"Quận 1\""), json);
        assertTrue(json.contains("\"url\":\"/cskh/don-dat-dich-vu?moDon=7\""), json);
        assertFalse(json.contains("soDienThoai"), json);
        assertFalse(json.contains("diaChi"), json);
        assertFalse(json.contains("Lê Lợi"), json);
    }
}
