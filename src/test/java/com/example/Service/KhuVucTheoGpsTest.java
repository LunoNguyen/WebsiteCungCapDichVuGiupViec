package com.example.Service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Đổi khu vực nhận việc theo GPS: chỉ ghi CSDL khi CTV sang quận khác (không cần CSDL). */
class KhuVucTheoGpsTest {

    @Test
    void quanDaDoi_chiKhiCoQuanMoiKhacQuanTruoc() {
        assertTrue(CollaboratorApiService.quanDaDoi("Quận Bình Thạnh", "Quận Tân Phú"));
        assertFalse(CollaboratorApiService.quanDaDoi("Quận Bình Thạnh", "Quận Bình Thạnh"));
        assertFalse(CollaboratorApiService.quanDaDoi("Quận Bình Thạnh", "quận bình thạnh "));
        // Không xác định được quận mới: không đổi gì
        assertFalse(CollaboratorApiService.quanDaDoi("Quận Bình Thạnh", ""));
        assertFalse(CollaboratorApiService.quanDaDoi("Quận Bình Thạnh", null));
        // Lần đầu ghi nhận vị trí (Redis chưa có quận): cần xét CSDL
        assertTrue(CollaboratorApiService.quanDaDoi("", "Quận Tân Phú"));
        assertTrue(CollaboratorApiService.quanDaDoi(null, "Quận Tân Phú"));
    }

    @Test
    void canGhiKhuVuc_doiQuanThiThayToanBo() {
        // Từ Bình Thạnh (id 1) sang Tân Phú (id 7): thay danh sách [1, 2, 3] bằng [7]
        assertTrue(CollaboratorApiService.canGhiKhuVuc(List.of(1, 2, 3), 7));
        // Sang quận vẫn nằm trong danh sách nhiều quận: vẫn thay bằng đúng quận đó
        assertTrue(CollaboratorApiService.canGhiKhuVuc(List.of(1, 7), 7));
        // Danh sách đã chỉ còn đúng quận mới: không ghi lại
        assertFalse(CollaboratorApiService.canGhiKhuVuc(List.of(7), 7));
    }

    @Test
    void canGhiKhuVuc_lanDauCungThayBangQuanDangDung() {
        // CTV không còn tự chọn quận: lần đầu có GPS cũng gọn danh sách cũ về đúng quận đang đứng
        assertTrue(CollaboratorApiService.canGhiKhuVuc(List.of(1, 7), 7));
        assertTrue(CollaboratorApiService.canGhiKhuVuc(List.of(1, 2), 7));
        assertTrue(CollaboratorApiService.canGhiKhuVuc(List.of(), 7));
        // Đã đúng là quận đang đứng (VD quận nơi ở lúc đăng ký): không ghi lại
        assertFalse(CollaboratorApiService.canGhiKhuVuc(List.of(7), 7));
    }
}
