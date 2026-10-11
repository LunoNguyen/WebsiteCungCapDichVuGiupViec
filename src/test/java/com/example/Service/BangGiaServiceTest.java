package com.example.Service;

import com.example.Model.BangGiaDichVu;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Quy tắc chọn bảng giá theo thời điểm (không cần CSDL). */
class BangGiaServiceTest {

    private static int id = 0;
    private static final LocalDateTime D = LocalDateTime.of(2026, 10, 1, 0, 0);

    private static BangGiaDichVu bg(long gia, LocalDateTime tu, LocalDateTime den, String trangThai) {
        BangGiaDichVu b = new BangGiaDichVu();
        b.setId(++id);
        b.setDonGia(BigDecimal.valueOf(gia));
        b.setNgayApDung(tu);
        b.setNgayKetThuc(den);
        b.setTrangThai(trangThai);
        return b;
    }

    private static long gia(List<BangGiaDichVu> ds, LocalDateTime luc) {
        return BangGiaService.chonGia(ds, luc).orElseThrow().getDonGia().longValue();
    }

    @Test
    void giaTamHetHanThiQuayVeGiaTruocDo() {
        BangGiaDichVu goc = bg(216000, D.minusMonths(6), null, "DangApDung");
        // Giá tạm từ 08:00 ngày 11 đến 17:30 ngày 21
        BangGiaDichVu tam = bg(180000, D.plusDays(10).withHour(8), D.plusDays(20).withHour(17).withMinute(30), "DangApDung");
        List<BangGiaDichVu> ds = List.of(goc, tam);

        assertEquals(216000, gia(ds, D.plusDays(10).withHour(7).withMinute(59)));  // 1 phút trước giờ bắt đầu
        assertEquals(180000, gia(ds, D.plusDays(10).withHour(8)));                 // đúng giờ bắt đầu
        assertEquals(180000, gia(ds, D.plusDays(20).withHour(17).withMinute(30))); // đúng giờ kết thúc
        assertEquals(216000, gia(ds, D.plusDays(20).withHour(17).withMinute(31))); // hết đợt: quay về giá gốc
    }

    @Test
    void layBangGiaChuaKetThucMoiNhat() {
        BangGiaDichVu cu = bg(200000, D.minusMonths(6), null, "DangApDung");
        BangGiaDichVu moi = bg(230000, D.minusDays(5), null, "DangApDung");
        BangGiaDichVu tam = bg(190000, D.plusDays(3), D.plusDays(5).withHour(23).withMinute(59).withSecond(59), "DangApDung");
        List<BangGiaDichVu> ds = List.of(cu, moi, tam);

        assertEquals(230000, gia(ds, D));
        assertEquals(190000, gia(ds, D.plusDays(4)));
        assertEquals(230000, gia(ds, D.plusDays(6)));   // về giá chưa kết thúc mới nhất (230k, không phải 200k)
    }

    @Test
    void giaGocChiHienKhiDangChayDotGiaTam() {
        BangGiaDichVu goc = bg(216000, D.minusMonths(6), null, "DangApDung");
        BangGiaDichVu tam = bg(194000, D.minusDays(1), D.plusDays(3), "DangApDung");   // giảm 10% có thời hạn
        List<BangGiaDichVu> ds = List.of(goc, tam);
        BangGiaDichVu dang = BangGiaService.chonGia(ds, D).orElseThrow();
        assertEquals(216000, BangGiaService.giaGoc(ds, dang, D).orElseThrow().longValue());

        // Đổi giá lâu dài (không thời hạn): giá mới là giá chính, không gạch giá cũ
        BangGiaDichVu tangLauDai = bg(230000, D.minusDays(1), null, "DangApDung");
        List<BangGiaDichVu> ds2 = List.of(goc, tangLauDai);
        assertTrue(BangGiaService.giaGoc(ds2, BangGiaService.chonGia(ds2, D).orElseThrow(), D).isEmpty());
    }

    @Test
    void boQuaGoiDaNgungThuCong() {
        BangGiaDichVu goc = bg(216000, D.minusMonths(6), null, "DangApDung");
        BangGiaDichVu ngung = bg(150000, D.minusDays(1), null, "HetHan");
        assertEquals(216000, gia(List.of(goc, ngung), D));
    }

    @Test
    void trangThaiTheoLuc() {
        // CSDL chỉ nhận DangApDung / HetHan (CHECK chk_BG_trangThai): chưa tới giờ vẫn là DangApDung
        assertEquals("DangApDung", BangGiaService.trangThaiTheoLuc(bg(1, D.plusMinutes(1), null, "DangApDung"), D));
        assertEquals("ChuaApDung", BangGiaService.hienThi(bg(1, D.plusMinutes(1), null, "DangApDung"), D));
        assertEquals("DangApDung", BangGiaService.hienThi(bg(1, D, D.plusHours(1), "DangApDung"), D));
        assertEquals("HetHan", BangGiaService.trangThaiTheoLuc(bg(1, D.minusDays(9), D.minusSeconds(1), "DangApDung"), D));
        assertEquals("HetHan", BangGiaService.trangThaiTheoLuc(bg(1, D.minusDays(9), null, "HetHan"), D));
    }

    @Test
    void ghepNgayGioTuForm() {
        assertEquals(LocalDateTime.of(2026, 10, 12, 0, 0), BangGiaService.ghepNgayGio("2026-10-12", "", false));
        assertEquals(LocalDateTime.of(2026, 10, 12, 23, 59, 59), BangGiaService.ghepNgayGio("2026-10-12", null, true));
        assertEquals(LocalDateTime.of(2026, 10, 12, 8, 30), BangGiaService.ghepNgayGio("2026-10-12", "08:30", false));
        assertEquals(LocalDateTime.of(2026, 10, 12, 23, 59, 59), BangGiaService.ghepNgayGio("2026-10-12", "23:59", true));
        assertEquals(LocalDateTime.of(2026, 10, 12, 8, 30), BangGiaService.ghepNgayGio("2026-10-12T08:30", null, false));
        assertNull(BangGiaService.ghepNgayGio("", "08:00", false));
    }

    @Test
    void kiemTraKhoang() {
        LocalDateTime sau = LocalDateTime.now().plusHours(2);
        assertDoesNotThrow(() -> BangGiaService.kiemTraKhoang(sau, sau.plusDays(1), null));
        assertThrows(IllegalArgumentException.class, () -> BangGiaService.kiemTraKhoang(LocalDateTime.now().minusHours(1), null, null));
        LocalDateTime cu = LocalDateTime.now().minusDays(3);
        assertDoesNotThrow(() -> BangGiaService.kiemTraKhoang(cu, null, cu)); // giữ nguyên thời điểm cũ khi sửa
        assertThrows(IllegalArgumentException.class, () -> BangGiaService.kiemTraKhoang(sau, sau, null));
    }
}
