package com.example.Service;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Nội dung chính sách đọc từ resources/chinh-sach.json (chuyển từ tài liệu docx). */
class ChinhSachServiceTest {

    private final ChinhSachService service = new ChinhSachService();

    @Test
    void docDuBaTaiLieuTheoThuTu() {
        List<Map<String, Object>> ds = service.danhSach();
        assertEquals(List.of("dieu-khoan", "khieu-nai", "bao-mat"), ds.stream().map(t -> t.get("ma")).toList());
        assertEquals("Điều khoản sử dụng", ds.get(0).get("ten"));
        ds.forEach(t -> assertFalse(((List<?>) t.get("noiDung")).isEmpty(), "tài liệu " + t.get("ma") + " rỗng"));
    }

    @Test
    void giuDuBonBangCuaQuyTrinhKhieuNai() {
        List<?> noiDung = (List<?>) service.theoMa("khieu-nai").orElseThrow().get("noiDung");
        List<Map<?, ?>> bang = noiDung.stream().<Map<?, ?>>map(k -> (Map<?, ?>) k).filter(k -> "bang".equals(k.get("loai"))).toList();
        assertEquals(4, bang.size());
        assertEquals(List.of("Loại khiếu nại", "Thời hạn tính từ"), bang.get(0).get("cot"));
    }

    @Test
    void maKhongCoThiRong() {
        assertTrue(service.theoMa("khong-co").isEmpty());
        assertTrue(service.theoMa(null).isEmpty());
    }
}
