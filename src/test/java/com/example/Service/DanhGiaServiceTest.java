package com.example.Service;

import com.example.Model.DanhGia;
import com.example.Model.NhanVien;
import com.example.Model.PhanHoiDanhGia;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Quy tắc kiểm duyệt đánh giá và chọn phản hồi công khai (không cần CSDL). */
class DanhGiaServiceTest {

    private static final LocalDateTime D = LocalDateTime.of(2026, 10, 17, 10, 0);

    private static PhanHoiDanhGia ph(int id, int phut, boolean laNhanVien) {
        return PhanHoiDanhGia.builder()
                .id(id)
                .nhanVien(laNhanVien ? new NhanVien() : null)
                .noiDung("ph" + id)
                .thoiGian(D.plusMinutes(phut))
                .build();
    }

    private static DanhGia dg(String trangThai) {
        DanhGia d = new DanhGia();
        d.setTrangThai(trangThai);
        return d;
    }

    @Test
    void loiTrangThai_chiNhanBaGiaTri() {
        assertNull(DanhGiaService.loiTrangThai("ChoDuyet"));
        assertNull(DanhGiaService.loiTrangThai("HienThi"));
        assertNull(DanhGiaService.loiTrangThai("An"));
        assertEquals("Trạng thái không hợp lệ.", DanhGiaService.loiTrangThai("DaDuyet"));
        assertEquals("Trạng thái không hợp lệ.", DanhGiaService.loiTrangThai(null));
    }

    @Test
    void loiDoiTrangThai_canChonIt1() {
        assertEquals("Chưa chọn đánh giá nào.", DanhGiaService.loiDoiTrangThai(List.of(), "HienThi"));
        assertEquals("Chưa chọn đánh giá nào.", DanhGiaService.loiDoiTrangThai(null, "HienThi"));
        assertEquals("Trạng thái không hợp lệ.", DanhGiaService.loiDoiTrangThai(List.of(1), "DaAn"));
        assertNull(DanhGiaService.loiDoiTrangThai(List.of(1, 2), "An"));
    }

    @Test
    void trangThaiSauKhiSua_hienThiVeChoDuyet() {
        assertEquals("ChoDuyet", DanhGiaService.trangThaiSauKhiSua("HienThi"));
        assertEquals("An", DanhGiaService.trangThaiSauKhiSua("An"));
        assertEquals("ChoDuyet", DanhGiaService.trangThaiSauKhiSua("ChoDuyet"));
    }

    @Test
    void laCongKhai_chiHienThi() {
        assertTrue(DanhGiaService.laCongKhai(dg("HienThi")));
        assertFalse(DanhGiaService.laCongKhai(dg("ChoDuyet")));
        assertFalse(DanhGiaService.laCongKhai(dg("An")));
        assertFalse(DanhGiaService.laCongKhai(dg(null)));
    }

    @Test
    void phanHoiMoiNhat_boQuaBinhLuanKhach() {
        PhanHoiDanhGia nv1 = ph(1, 0, true);
        PhanHoiDanhGia kh = ph(2, 60, false);
        PhanHoiDanhGia nv2 = ph(3, 30, true);
        assertSame(nv2, DanhGiaService.phanHoiMoiNhatCuaNhanVien(List.of(nv1, kh, nv2)));
    }

    @Test
    void phanHoiMoiNhat_nullKhiChuaCoNhanVien() {
        assertNull(DanhGiaService.phanHoiMoiNhatCuaNhanVien(List.of(ph(1, 0, false))));
        assertNull(DanhGiaService.phanHoiMoiNhatCuaNhanVien(List.of()));
    }
}
