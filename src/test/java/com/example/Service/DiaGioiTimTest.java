package com.example.Service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Ô tìm địa chỉ: gõ tên tỉnh / quận, dò cả địa giới sau sáp nhập (v2) và trước sáp nhập (v1). */
class DiaGioiTimTest {

    private static Map<String, Object> tinh(int ma, String ten) {
        return Map.of("ma", ma, "ten", ten);
    }

    private static Map<String, Object> quan(int ma, String ten, int maTinh, String tenTinh) {
        return Map.of("ma", ma, "ten", ten, "maTinh", maTinh, "tenTinh", tenTinh);
    }

    private final List<Map<String, Object>> tinhMoi = List.of(
            tinh(79, "Thành phố Hồ Chí Minh"), tinh(1, "Thành phố Hà Nội"), tinh(68, "Tỉnh Lâm Đồng"));
    private final List<Map<String, Object>> tinhCu = List.of(
            tinh(79, "Thành phố Hồ Chí Minh"), tinh(74, "Tỉnh Bình Dương"), tinh(68, "Tỉnh Lâm Đồng"));
    private final List<Map<String, Object>> quanCu = List.of(
            quan(765, "Quận Bình Thạnh", 79, "Thành phố Hồ Chí Minh"),
            quan(785, "Huyện Bình Chánh", 79, "Thành phố Hồ Chí Minh"),
            quan(718, "Thành phố Thủ Dầu Một", 74, "Tỉnh Bình Dương"),
            quan(672, "Thành phố Đà Lạt", 68, "Tỉnh Lâm Đồng"));

    private List<Map<String, Object>> tim(String q) {
        return DiaGioiService.loc(q, tinhMoi, tinhCu, quanCu, 20);
    }

    @Test
    void goKhongDauVanTimDuocQuanCu() {
        List<Map<String, Object>> kq = tim("binh thanh");
        assertEquals(1, kq.size());
        assertEquals("quanCu", kq.get(0).get("loai"));
        assertEquals(765, kq.get(0).get("maQuan"));
        assertEquals(79, kq.get(0).get("maTinh"));
    }

    @Test
    void tenTinhDaSapNhapTraVeCaTinhMoiLanTinhCu() {
        List<Map<String, Object>> kq = tim("Lâm Đồng");
        assertEquals(List.of("tinhMoi", "tinhCu"), kq.stream().limit(2).map(m -> m.get("loai")).toList());
    }

    @Test
    void tinhCuDaBiSapNhapVanTimDuoc() {
        List<Map<String, Object>> kq = tim("bình dương");
        assertEquals("tinhCu", kq.get(0).get("loai"));
        assertEquals(74, kq.get(0).get("maTinh"));
        // các quận cũ của Bình Dương cũng hiện (khớp theo tên tỉnh)
        assertTrue(kq.stream().anyMatch(m -> Integer.valueOf(718).equals(m.get("maQuan"))));
    }

    @Test
    void khopDauTenXepTruocKhopGiua() {
        List<Map<String, Object>> kq = tim("binh");
        assertEquals("Tỉnh Bình Dương", kq.get(0).get("ten"));
        assertTrue(kq.stream().map(m -> (String) m.get("ten")).toList().contains("Quận Bình Thạnh"));
    }

    @Test
    void vietTatHcm() {
        List<Map<String, Object>> kq = tim("hcm");
        assertEquals("tinhMoi", kq.get(0).get("loai"));
        assertEquals(79, kq.get(0).get("maTinh"));
    }

    @Test
    void rongThiGoiYTinhSauSapNhap() {
        List<Map<String, Object>> kq = tim("  ");
        assertEquals(3, kq.size());
        assertTrue(kq.stream().allMatch(m -> "tinhMoi".equals(m.get("loai"))));
    }

    @Test
    void gioiHanSoKetQua() {
        assertEquals(2, DiaGioiService.loc("thanh pho", tinhMoi, tinhCu, quanCu, 2).size());
    }
}
