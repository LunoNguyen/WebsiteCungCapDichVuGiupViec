package com.example.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Nội dung thông báo khi bTaskee trả lời đánh giá. */
class ThongBaoPhanHoiDanhGiaTest {

    @Test
    void noiDungNgan_giuNguyen() {
        assertEquals("Cảm ơn bạn! Xem chi tiết trong đơn hàng của bạn.",
                ThongBaoSuKienService.noiDungPhanHoiDanhGia("  Cảm ơn bạn!  "));
    }

    @Test
    void noiDungDai_cat200KyTu() {
        String dai = "a".repeat(250);
        assertEquals("a".repeat(200) + "… Xem chi tiết trong đơn hàng của bạn.",
                ThongBaoSuKienService.noiDungPhanHoiDanhGia(dai));
    }
}
