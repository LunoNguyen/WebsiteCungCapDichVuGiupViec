package com.example.Service;

import com.example.Model.BangGiaDichVu;
import com.example.Model.DichVu;
import com.example.Model.LoaiDichVu;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử nghiệp vụ Task 1:
 * 1. Thêm dịch vụ mới kèm bảng giá khởi điểm tự động.
 * 2. Tách riêng các trường: Đơn vị tính, Thời lượng (giờ/phút), Số người làm, Số buổi/tháng.
 * 3. Ràng buộc thời gian (giờ làm việc, giờ áp dụng bảng giá).
 */
class ThemDichVuVaBangGiaTest {

    @Test
    void themDichVuMoiTheoLan_tachRiengGioVaNguoi() {
        LoaiDichVu ldv = LoaiDichVu.builder().id(1).tenLoaiDichVu("Dọn dẹp nhà").maLoaiDichVu("LDV01").build();

        // Giả lập dữ liệu nhập từ form đã tách
        double soGio = 3.0;
        int phut = (int) Math.round(soGio * 60);
        int soNguoi = 2;
        String donViTinh = "Buổi";
        BigDecimal donGia = BigDecimal.valueOf(350000);

        DichVu dv = DichVu.builder()
                .maDichVu("DV104")
                .loaiDichVu(ldv)
                .tenDichVu("Vệ sinh đệm sofa chuyên sâu")
                .thoiGianThucHien(phut)
                .soNguoiThucHien(soNguoi)
                .loaiHinhDat("TheoLan")
                .donViTinh(donViTinh)
                .giaHienTai(donGia)
                .trangThai("HoatDong")
                .build();

        // 1. Kiểm tra thuộc tính dịch vụ lưu tách bạch
        assertEquals(180, dv.getThoiGianThucHien(), "Thời lượng phải là 180 phút (3 giờ)");
        assertEquals(2, dv.getSoNguoiThucHien(), "Số người làm phải là 2 người");
        assertEquals("Buổi", dv.getDonViTinh(), "Đơn vị tính thuần túy là Buổi, không kèm chữ 3h");
        assertEquals("TheoLan", dv.getLoaiHinhDat());
        assertNull(dv.getSoBuoi(), "Đơn theo lần không có số buổi");

        // 2. Bảng giá khởi điểm được tự động tạo tương ứng
        BangGiaDichVu bg = BangGiaDichVu.builder()
                .maBangGia("BG-DV104-001")
                .dichVu(dv)
                .loaiHinhDat(dv.getLoaiHinhDat())
                .donViTinh(dv.getDonViTinh())
                .donGia(donGia)
                .ngayApDung(LocalDateTime.now())
                .trangThai("DangApDung")
                .build();

        assertNotNull(bg.getDichVu());
        assertEquals(BigDecimal.valueOf(350000), bg.getDonGia());
        assertEquals("DangApDung", bg.getTrangThai());
    }

    @Test
    void themDichVuGoiThang_coSoBuoi() {
        LoaiDichVu ldv = LoaiDichVu.builder().id(1).tenLoaiDichVu("Dọn dẹp nhà").build();

        double soGio = 2.0;
        int phut = (int) Math.round(soGio * 60);

        DichVu dv = DichVu.builder()
                .maDichVu("DV105")
                .loaiDichVu(ldv)
                .tenDichVu("Gói dọn dẹp định kỳ 12 buổi")
                .thoiGianThucHien(phut)
                .soNguoiThucHien(1)
                .loaiHinhDat("GoiThang")
                .donViTinh("Buổi")
                .soBuoi(12)
                .giaHienTai(BigDecimal.valueOf(1800000))
                .trangThai("HoatDong")
                .build();

        assertEquals(12, dv.getSoBuoi(), "Gói tháng phải lưu số buổi là 12");
        assertEquals(120, dv.getThoiGianThucHien(), "Mỗi buổi 2 giờ = 120 phút");
        assertEquals(1, dv.getSoNguoiThucHien());
    }

    @Test
    void rangBuocThoiGian_gioKetThucPhaiSauGioBatDau() {
        LocalDateTime tu = LocalDateTime.of(2026, 11, 1, 14, 0);
        LocalDateTime denLoi = LocalDateTime.of(2026, 11, 1, 10, 0); // cùng ngày nhưng kết thúc trước
        LocalDateTime denDung = LocalDateTime.of(2026, 11, 1, 18, 0); // cùng ngày nhưng kết thúc sau

        assertThrows(IllegalArgumentException.class, () ->
                BangGiaService.kiemTraKhoang(tu, denLoi, null),
                "Thời điểm kết thúc trước bắt đầu phải ném ngoại lệ");

        assertDoesNotThrow(() ->
                BangGiaService.kiemTraKhoang(tu, denDung, null),
                "Thời điểm kết thúc sau bắt đầu phải hợp lệ");
    }

    @Test
    void rangBuocThoiGian_ngayBatDauKhongOQuaKhu() {
        LocalDateTime quaKhu = LocalDateTime.now().minusDays(2);
        assertThrows(IllegalArgumentException.class, () ->
                BangGiaService.kiemTraKhoang(quaKhu, null, null),
                "Thời điểm áp dụng trong quá khứ phải bị từ chối");
    }

    @Test
    void ghepNgayGio_chuanXac() {
        LocalDateTime dt = BangGiaService.ghepNgayGio("2026-11-15", "08:30", false);
        assertNotNull(dt);
        assertEquals(2026, dt.getYear());
        assertEquals(11, dt.getMonthValue());
        assertEquals(15, dt.getDayOfMonth());
        assertEquals(8, dt.getHour());
        assertEquals(30, dt.getMinute());

        // Để trống giờ khi kết thúc -> mặc định 23:59:59
        LocalDateTime denMacDinh = BangGiaService.ghepNgayGio("2026-11-20", "", true);
        assertNotNull(denMacDinh);
        assertEquals(23, denMacDinh.getHour());
        assertEquals(59, denMacDinh.getMinute());
        assertEquals(59, denMacDinh.getSecond());
    }
}
