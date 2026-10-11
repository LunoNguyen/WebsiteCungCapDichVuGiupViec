package com.example.SuKien;

/**
 * Sự kiện nghiệp vụ của đơn đặt và phân công, phát ra từ entity listener (mọi nơi lưu đơn đều đi qua),
 * xử lý sau khi giao dịch ghi xong để gửi thông báo (ThongBaoSuKienService).
 */
public final class SuKienDon {

    private SuKienDon() {
    }

    /** Khách vừa đặt đơn mới. */
    public record DonMoi(Integer donId) { }

    /** Đơn đổi trạng thái (ChoDuyet, DaXacNhan, DangThucHien, HoanThanh, DaHuy). */
    public record DoiTrangThai(Integer donId, String cu, String moi) { }

    /** Đơn đổi ngày, giờ hoặc địa chỉ thực hiện; moTa VD "giờ bắt đầu 08:00 → 09:00". */
    public record DoiThongTin(Integer donId, String moTa) { }

    /** Cộng tác viên được phân công / mời nhận đơn (trangThai: ChoPhanCong hoặc DaXacNhan). */
    public record PhanCongMoi(Integer phanCongId, String trangThai) { }

    /** Phân công đổi trạng thái (VD ChoPhanCong → DaXacNhan khi CTV nhận đơn). */
    public record PhanCongDoiTrangThai(Integer phanCongId, String cu, String moi) { }
}
