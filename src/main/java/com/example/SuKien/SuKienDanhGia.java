package com.example.SuKien;

/**
 * Sự kiện nghiệp vụ của đánh giá đơn, xử lý sau khi giao dịch ghi xong để gửi thông báo (ThongBaoSuKienService).
 */
public final class SuKienDanhGia {

    private SuKienDanhGia() {
    }

    /** Nhân viên bTaskee vừa trả lời dưới một đánh giá. */
    public record PhanHoiMoi(Integer danhGiaId) { }
}
