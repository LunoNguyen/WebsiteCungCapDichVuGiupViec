package com.example.Service;

import com.example.Model.KhuVuc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class DiaGioiServiceTest {

    private KhuVucService khuVucService;
    private DiaGioiService diaGioiService;

    @BeforeEach
    void setUp() {
        khuVucService = Mockito.mock(KhuVucService.class);
        KhuVuc kvTanPhu = KhuVuc.builder().id(7).tenKhuVuc("Quận Tân Phú").maKhuVuc("KV-Q767").build();
        when(khuVucService.timTheoTen(Mockito.contains("Tân Phú"))).thenReturn(Optional.of(kvTanPhu));
        when(khuVucService.danhSach()).thenReturn(List.of(kvTanPhu));

        diaGioiService = new DiaGioiService(khuVucService);
    }

    @Test
    void tinhThanhV1_coHcmDauTien() {
        List<Map<String, Object>> ds = diaGioiService.tinhThanhV1();
        assertNotNull(ds);
        assertFalse(ds.isEmpty());
        // TP.HCM (79) được ưu tiên lên đầu
        assertEquals(79, ds.get(0).get("ma"));
    }

    @Test
    void quanHuyenV1_tpCoQuanTanPhuVaBinhThanh() {
        List<Map<String, Object>> quanList = diaGioiService.quanHuyenV1(79);
        assertNotNull(quanList);
        assertFalse(quanList.isEmpty());

        boolean coTanPhu = quanList.stream().anyMatch(q -> "Quận Tân Phú".equals(q.get("ten")));
        boolean coBinhThanh = quanList.stream().anyMatch(q -> "Quận Bình Thạnh".equals(q.get("ten")));

        assertTrue(coTanPhu, "Danh sách quận v1 phải có Quận Tân Phú");
        assertTrue(coBinhThanh, "Danh sách quận v1 phải có Quận Bình Thạnh");
    }

    @Test
    void phuongXaV1_quanTanPhuCoPhuongTayThanh() {
        // Mã quận Tân Phú = 767
        List<Map<String, Object>> phuongList = diaGioiService.phuongXaV1(767);
        assertNotNull(phuongList);
        assertFalse(phuongList.isEmpty());

        boolean coTayThanh = phuongList.stream().anyMatch(p -> ((String) p.get("ten")).contains("Tây Thạnh"));
        assertTrue(coTayThanh, "Quận Tân Phú phải có Phường Tây Thạnh");
    }

    @Test
    void xacDinhV1_ghepDiaChiChuanVaGanKhuVucDung() {
        // 79: HCM, 767: Tân Phú, 27013: Tây Thạnh
        DiaGioiService.DiaChiDaXacDinh res = diaGioiService.xacDinhV1("123 Lũy Bán Bích", 79, 767, 27013);
        assertNotNull(res);
        assertNotNull(res.khuVuc());
        assertEquals("Quận Tân Phú", res.khuVuc().getTenKhuVuc());
        assertTrue(res.diaChiDayDu().contains("123 Lũy Bán Bích"));
        assertTrue(res.diaChiDayDu().contains("Quận Tân Phú"));
    }
}
