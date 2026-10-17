package com.example.Service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Xác định quận cũ của TP.HCM từ toạ độ GPS theo tâm quận gần nhất (không cần CSDL). */
class TamQuanHcmTest {

    private static final List<TamQuanHcm.Tam> DS = TamQuanHcm.docMacDinh();

    @Test
    void file_du22QuanHuyen() {
        assertEquals(22, DS.size());
    }

    @Test
    void quanGanNhat_theoToaDo() {
        assertEquals("Quận Tân Phú", TamQuanHcm.quanGanNhat(DS, 10.7901, 106.6297));
        assertEquals("Quận 1", TamQuanHcm.quanGanNhat(DS, 10.7756, 106.7019)); // chợ Bến Thành
        assertEquals("Quận Bình Thạnh", TamQuanHcm.quanGanNhat(DS, 10.8031, 106.7101)); // Hàng Xanh
        assertEquals("Huyện Củ Chi", TamQuanHcm.quanGanNhat(DS, 11.0600, 106.4950));
        assertEquals("Thành phố Thủ Đức", TamQuanHcm.quanGanNhat(DS, 10.8500, 106.7720));
    }

    @Test
    void quanGanNhat_ngoaiTpHcmThiNull() {
        // Vị trí mặc định của máy ảo Android (Mountain View, California)
        assertNull(TamQuanHcm.quanGanNhat(DS, 37.4219983, -122.084));
        // Hà Nội
        assertNull(TamQuanHcm.quanGanNhat(DS, 21.0285, 105.8542));
    }
}
