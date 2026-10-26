package com.example.Service;

import com.example.Model.DonDatDichVu;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** Quy tắc CSKH đổi trạng thái / xóa đơn và tự hoàn thành đơn quá hạn. */
class QuanLyDonServiceTest {

    @Test
    void chiChoChuyenTheoLuongHopLe() {
        assertNull(QuanLyDonService.loiChuyenTrangThai("ChoDuyet", "DangThucHien"));
        assertNull(QuanLyDonService.loiChuyenTrangThai("DaXacNhan", "DangThucHien"));
        assertNull(QuanLyDonService.loiChuyenTrangThai("DangThucHien", "HoanThanh"));
        assertNull(QuanLyDonService.loiChuyenTrangThai("ChoDuyet", "DaHuy"));
        assertNull(QuanLyDonService.loiChuyenTrangThai("DangThucHien", "DaHuy"));

        assertEquals("Đơn đã ở trạng thái này.", QuanLyDonService.loiChuyenTrangThai("ChoDuyet", "ChoDuyet"));
        assertEquals("Đơn đã kết thúc (Hoàn thành), không đổi trạng thái được nữa.",
                QuanLyDonService.loiChuyenTrangThai("HoanThanh", "DaHuy"));
        assertEquals("Đơn đã kết thúc (Đã hủy), không đổi trạng thái được nữa.",
                QuanLyDonService.loiChuyenTrangThai("DaHuy", "ChoDuyet"));
        assertEquals("Không chuyển được từ Chờ duyệt sang Hoàn thành.",
                QuanLyDonService.loiChuyenTrangThai("ChoDuyet", "HoanThanh"));
        assertEquals("Không chuyển được từ Đang thực hiện sang Chờ duyệt.",
                QuanLyDonService.loiChuyenTrangThai("DangThucHien", "ChoDuyet"));
        assertEquals("Trạng thái không hợp lệ.", QuanLyDonService.loiChuyenTrangThai("ChoDuyet", "Bay"));
    }

    @Test
    void chiXoaDonChuaPhatSinh() {
        assertNull(QuanLyDonService.lyDoKhongXoa("DaHuy", false, false, false));
        assertNull(QuanLyDonService.lyDoKhongXoa("ChoDuyet", false, false, false));
        assertEquals("Chỉ xóa được đơn Chờ duyệt hoặc Đã hủy. Đơn khác hãy chuyển sang Đã hủy.",
                QuanLyDonService.lyDoKhongXoa("HoanThanh", false, false, false));
        assertEquals("Đơn đã thanh toán, không xóa được. Hãy chuyển sang Đã hủy.",
                QuanLyDonService.lyDoKhongXoa("DaHuy", true, false, false));
        assertEquals("Đơn đã có phân công cộng tác viên, không xóa được. Hãy chuyển sang Đã hủy.",
                QuanLyDonService.lyDoKhongXoa("ChoDuyet", false, true, false));
        assertEquals("Đơn đã có đánh giá, khiếu nại hoặc biên bản, không xóa được.",
                QuanLyDonService.lyDoKhongXoa("DaHuy", false, false, true));
    }

    @Test
    void dangLamQuaGioKetThuc24GioThiTuHoanThanh() {
        DonDatDichVu don = DonDatDichVu.builder()
                .loaiHinhDat("TheoLan")
                .ngayThucHien(LocalDate.of(2026, 9, 28))
                .gioBatDau(LocalTime.of(14, 0))
                .gioKetThuc(LocalTime.of(17, 30))
                .build();
        assertFalse(QuanLyDonService.quaHanHoanThanh(don, LocalDateTime.of(2026, 9, 29, 17, 29)));
        assertTrue(QuanLyDonService.quaHanHoanThanh(don, LocalDateTime.of(2026, 9, 29, 17, 31)));
        assertFalse(QuanLyDonService.quaHanHoanThanh(DonDatDichVu.builder().build(), LocalDateTime.now()));
    }

    @Test
    void dangThucHienCanDuNguoiChoMoiDichVu() {
        assertEquals("Còn dịch vụ chưa có cộng tác viên nhận.", QuanLyDonService.loiDuNguoi(false));
        assertNull(QuanLyDonService.loiDuNguoi(true));
    }
}
