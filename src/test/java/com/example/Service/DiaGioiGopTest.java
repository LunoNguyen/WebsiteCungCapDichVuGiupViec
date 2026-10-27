package com.example.Service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Form địa chỉ 2 ô: tỉnh gộp v1 + v2 theo mã; tỉnh có ở cả hai thì phường, xã lấy từ cả hai. */
class DiaGioiGopTest {

    private static Map<String, Object> tinh(int ma, String ten) {
        return Map.of("ma", ma, "ten", ten);
    }

    @Test
    void tinhTrungMaChiHienMotLanCoCaHaiNguon() {
        List<Map<String, Object>> kq = DiaGioiService.gopTinh(
                List.of(tinh(79, "Thành phố Hồ Chí Minh"), tinh(68, "Tỉnh Lâm Đồng")),
                List.of(tinh(79, "Thành phố Hồ Chí Minh"), tinh(74, "Tỉnh Bình Dương"), tinh(68, "Tỉnh Lâm Đồng")));
        assertEquals(List.of(79, 68, 74), kq.stream().map(m -> m.get("ma")).toList());
        assertEquals(true, kq.get(0).get("coMoi"));
        assertEquals(true, kq.get(0).get("coCu"));
        assertEquals(false, kq.get(2).get("coMoi"));
        assertEquals(true, kq.get(2).get("coCu"));
    }

    @Test
    void phuongGopCaMoiLanCuKemQuanCu() {
        List<Map<String, Object>> moi = List.of(Map.of("ma", 26734, "ten", "Phường Sài Gòn", "khuVuc", List.of()));
        List<Map<String, Object>> quanCu = List.of(Map.of("ma", 760, "ten", "Quận 1", "khuVucId", 1, "tenKhuVuc", "Quận 1",
                "wards", List.of(Map.of("ma", 26734, "ten", "Phường Bến Nghé"), Map.of("ma", 26737, "ten", "Phường Đa Kao"))));
        List<Map<String, Object>> kq = DiaGioiService.gopPhuong(moi, quanCu);
        assertEquals(3, kq.size());
        Map<String, Object> benNghe = kq.stream().filter(m -> "Phường Bến Nghé".equals(m.get("ten"))).findFirst().orElseThrow();
        assertEquals("cu", benNghe.get("loai"));
        assertEquals(760, benNghe.get("maQuan"));
        assertEquals("Quận 1", benNghe.get("tenQuan"));
        assertEquals(1, benNghe.get("khuVucId"));
        Map<String, Object> saiGon = kq.stream().filter(m -> "Phường Sài Gòn".equals(m.get("ten"))).findFirst().orElseThrow();
        assertEquals("moi", saiGon.get("loai"));
        assertFalse(saiGon.containsKey("maQuan"));
    }

    @Test
    void phuongXepTheoTenBoTienTo() {
        List<Map<String, Object>> moi = List.of(Map.of("ma", 1, "ten", "Xã Bình Lợi"));
        List<Map<String, Object>> quanCu = List.of(Map.of("ma", 760, "ten", "Quận 1",
                "wards", List.of(Map.of("ma", 2, "ten", "Phường An Khánh"))));
        assertEquals(List.of("Phường An Khánh", "Xã Bình Lợi"),
                DiaGioiService.gopPhuong(moi, quanCu).stream().map(m -> m.get("ten")).toList());
    }
}
