package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DichVu;
import com.example.Model.DonDatDichVu;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Đánh giá, bình luận, phản hồi tính theo mã dịch vụ: mọi thời lượng / gói cùng mã dùng chung một bộ đánh giá. */
class DanhGiaTheoMaDichVuTest {

    private static DichVu dv(int id, String ma) {
        DichVu d = new DichVu();
        d.setId(id);
        d.setMaDichVu(ma);
        return d;
    }

    private static DonDatDichVu don(DichVu... dichVus) {
        DonDatDichVu d = new DonDatDichVu();
        d.setChiTietList(java.util.Arrays.stream(dichVus).map(x -> {
            ChiTietDonDat ct = new ChiTietDonDat();
            ct.setDichVu(x);
            return ct;
        }).toList());
        return d;
    }

    @Test
    void donCuaThoiLuongKhacCungMaVanTinh() {
        DichVu hai = dv(1, "DV101"), ba = dv(319, "DV101"), tongVeSinh = dv(6, "DV102");
        assertTrue(DanhGiaService.donThuocDichVu(don(ba), hai));
        assertTrue(DanhGiaService.donThuocDichVu(don(hai), hai));
        assertFalse(DanhGiaService.donThuocDichVu(don(tongVeSinh), hai));
    }

    @Test
    void dichVuChuaCoMaThiTheoId() {
        DichVu khongMa = dv(50, null), khac = dv(51, null);
        assertTrue(DanhGiaService.donThuocDichVu(don(khongMa), khongMa));
        assertFalse(DanhGiaService.donThuocDichVu(don(khac), khongMa));
    }

    @Test
    void donRongKhongThuocDichVuNao() {
        assertFalse(DanhGiaService.donThuocDichVu(null, dv(1, "DV101")));
        DonDatDichVu rong = new DonDatDichVu();
        rong.setChiTietList(List.of());
        assertFalse(DanhGiaService.donThuocDichVu(rong, dv(1, "DV101")));
    }
}
