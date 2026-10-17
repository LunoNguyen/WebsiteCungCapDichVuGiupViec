package com.example.Service;

/**
 * Mô tả chương trình khuyến mãi hiển thị cho khách hàng.
 * Dữ liệu cũ từng lưu thông tin nội bộ vào mô tả ("Tạo lúc 21:07 · 11/10/2026 [DA_THONG_BAO]"):
 * bỏ phần đó, còn lại rỗng thì không hiện.
 */
public final class KhuyenMaiHienThi {

    private KhuyenMaiHienThi() {
    }

    public static String moTaChoKhach(String moTa) {
        if (moTa == null) return null;
        String s = moTa.replaceAll("\\[[A-Z_]+\\]", "")                       // [DA_THONG_BAO]...
                .replaceAll("(?i)^\\s*Tạo lúc\\s+[0-9:]+\\s*·\\s*[0-9/]+\\s*", "") // "Tạo lúc 21:07 · 11/10/2026"
                .trim();
        return s.isEmpty() ? null : s;
    }
}
