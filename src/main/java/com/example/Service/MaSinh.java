package com.example.Service;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Sinh mã nghiệp vụ không trùng (KH-..., DD-..., TB-...) kể cả khi nhiều người tạo dữ liệu cùng lúc.
 *
 * Cách cũ "tiền tố + (System.currentTimeMillis() % 1000000)" bị trùng khi hai yêu cầu rơi vào cùng
 * một mili-giây, và tự lặp lại sau mỗi 1.000 giây (~17 phút), gây lỗi vi phạm ràng buộc UNIQUE.
 *
 * Cách mới: một bộ đếm luôn tăng, khởi đầu từ thời điểm hiện tại, viết ở hệ 36 (0-9, A-Z).
 * Mỗi lần gọi cho một giá trị khác nhau, dài 10 ký tự, nên mã dài tối đa 17 ký tự (cột mã là VARCHAR(20)).
 */
public final class MaSinh {

    private static final AtomicLong BO_DEM = new AtomicLong(0);

    private MaSinh() {
    }

    /** Phần đuôi duy nhất, 10 ký tự. */
    public static String duoi() {
        long gia = BO_DEM.updateAndGet(truoc -> Math.max(truoc + 1, System.currentTimeMillis() * 1000));
        return Long.toString(gia, 36).toUpperCase();
    }

    /** Mã đầy đủ: tiền tố + đuôi duy nhất, ví dụ tao("KH-") → "KH-HZ3K9Q2M1A". */
    public static String tao(String tienTo) {
        return tienTo + duoi();
    }
}
