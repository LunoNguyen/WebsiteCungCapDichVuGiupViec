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

    @Test
    void xacDinhV1_khongPhuong_dungOQuan() {
        DiaGioiService.DiaChiDaXacDinh res = diaGioiService.xacDinhV1("123 Lũy Bán Bích", 79, 767, null);
        assertEquals("123 Lũy Bán Bích, Quận Tân Phú, Thành phố Hồ Chí Minh", res.diaChiDayDu());
        assertEquals(7, res.khuVuc().getId());
    }

    @Test
    void xacDinhChung_coQuanThiTheoV1() {
        DiaGioiService.DiaChiDaXacDinh res = diaGioiService.xacDinhChung("5 Tân Kỳ Tân Quý", 79, 767, null, null);
        assertTrue(res.diaChiDayDu().endsWith("Quận Tân Phú, Thành phố Hồ Chí Minh"));
        assertEquals(List.of(7), diaGioiService.khuVucCuaDiaChi(79, 767, null).stream().map(KhuVuc::getId).toList());
    }

    @Test
    void tinhGop_tinhCungTenChiMotDong_tinhChiConODanhMucCuGhiCu() {
        List<Map<String, Object>> ds = diaGioiService.tinhGop();
        List<Map<String, Object>> hcm = ds.stream().filter(t -> "Thành phố Hồ Chí Minh".equals(t.get("ten"))).toList();
        assertEquals(1, hcm.size(), "TP.HCM có ở cả hai danh mục: chỉ một dòng");
        assertEquals(Boolean.FALSE, hcm.get(0).get("cu"));
        assertEquals(79, hcm.get(0).get("maCu"));
        // Bình Dương đã sáp nhập: chỉ còn ở danh mục cũ
        Map<String, Object> binhDuong = ds.stream().filter(t -> "Tỉnh Bình Dương".equals(t.get("ten"))).findFirst().orElseThrow();
        assertEquals(Boolean.TRUE, binhDuong.get("cu"));
        assertEquals(Boolean.FALSE, ds.get(0).get("cu"));
        // "Tỉnh Quảng Ninh" (v1) và "Thành phố Quảng Ninh" (v2) chỉ khác chữ đầu: một dòng
        assertEquals(1, ds.stream().filter(t -> ((String) t.get("ten")).endsWith("Quảng Ninh")).count());
    }

    @Test
    @SuppressWarnings("unchecked")
    void duoiTinh_tinhCungTen_gopPhuongMoiVaQuanCu() {
        List<Map<String, Object>> ds = diaGioiService.duoiTinh(79, false);
        assertTrue(ds.stream().anyMatch(x -> Boolean.FALSE.equals(x.get("cu")) && ((String) x.get("ten")).startsWith("Phường")));
        Map<String, Object> tanPhu = ds.stream().filter(q -> "Quận Tân Phú".equals(q.get("ten"))).findFirst().orElseThrow();
        assertEquals(767, tanPhu.get("ma"));
        assertEquals(Boolean.TRUE, tanPhu.get("cu"));
        assertEquals(79, tanPhu.get("maTinh"));
        List<Map<String, Object>> kv = (List<Map<String, Object>>) tanPhu.get("khuVuc");
        assertEquals(7, kv.get(0).get("id"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void duoiTinh_tinhCu_laQuanHuyenKemKhuVuc() {
        List<Map<String, Object>> ds = diaGioiService.duoiTinh(79, true);
        Map<String, Object> tanPhu = ds.stream().filter(q -> "Quận Tân Phú".equals(q.get("ten"))).findFirst().orElseThrow();
        assertEquals(767, tanPhu.get("ma"));
        assertEquals(Boolean.TRUE, tanPhu.get("cu"));
        List<Map<String, Object>> kv = (List<Map<String, Object>>) tanPhu.get("khuVuc");
        assertEquals(7, kv.get(0).get("id"));
    }

    @Test
    void tach_diaChiCu_traVeMaQuan() {
        Map<String, Object> m = diaGioiService.tach("12 Lũy Bán Bích, Quận Tân Phú, Thành phố Hồ Chí Minh");
        assertEquals(79, m.get("maTinh"));
        assertEquals(767, m.get("maQuan"));
        assertNull(m.get("maPhuong"));
        assertEquals("12 Lũy Bán Bích", m.get("soNhaTenDuong"));

        // Địa chỉ 3 cấp cũ: phường cũ giữ trong phần số nhà
        Map<String, Object> m3 = diaGioiService.tach("12 Lũy Bán Bích, Phường Tây Thạnh, Quận Tân Phú, Thành phố Hồ Chí Minh");
        assertEquals(767, m3.get("maQuan"));
        assertEquals("12 Lũy Bán Bích, Phường Tây Thạnh", m3.get("soNhaTenDuong"));
    }
}
