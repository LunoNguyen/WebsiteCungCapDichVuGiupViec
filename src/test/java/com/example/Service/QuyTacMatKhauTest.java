package com.example.Service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Quy tắc mật khẩu mới: ít nhất 8 ký tự, có chữ cái và chữ số, không khoảng trắng, không chứa số điện thoại. */
class QuyTacMatKhauTest {

    private static final String SDT = "0912345678";

    @Test
    void matKhauDatDuTieuChiLaHopLe() {
        assertTrue(QuyTacMatKhau.hopLe("matkhau1", SDT));
        assertTrue(QuyTacMatKhau.hopLe("Abc12345", SDT));
        assertTrue(QuyTacMatKhau.hopLe("đẹptrai9", SDT)); // chữ có dấu vẫn tính là chữ cái
        assertTrue(QuyTacMatKhau.hopLe("a1!@#$%^", null));
    }

    @Test
    void nganHon8KyTuBiTuChoi() {
        assertEquals(List.of(QuyTacMatKhau.LOI_DO_DAI), QuyTacMatKhau.kiemTra("abc1234", SDT));
        assertTrue(QuyTacMatKhau.hopLe("abc12345", SDT));
    }

    @Test
    void thieuChuCaiHoacChuSo() {
        assertEquals(List.of(QuyTacMatKhau.LOI_CHU_CAI), QuyTacMatKhau.kiemTra("12345678", null));
        assertEquals(List.of(QuyTacMatKhau.LOI_CHU_SO), QuyTacMatKhau.kiemTra("abcdefgh", null));
        assertEquals(List.of(QuyTacMatKhau.LOI_CHU_SO), QuyTacMatKhau.kiemTra("abcdefg١", null)); // số Ả Rập không tính
    }

    @Test
    void khongDuocChuaKhoangTrang() {
        assertEquals(List.of(QuyTacMatKhau.LOI_KHOANG_TRANG), QuyTacMatKhau.kiemTra("abc 12345", null));
        assertEquals(List.of(QuyTacMatKhau.LOI_KHOANG_TRANG), QuyTacMatKhau.kiemTra("abc\t12345", null));
    }

    @Test
    void khongDuocChuaSoDienThoai() {
        assertEquals(List.of(QuyTacMatKhau.LOI_SO_DIEN_THOAI), QuyTacMatKhau.kiemTra("a0912345678", SDT));
        assertEquals(List.of(QuyTacMatKhau.LOI_SO_DIEN_THOAI), QuyTacMatKhau.kiemTra("x912345678", SDT));
        assertEquals(List.of(QuyTacMatKhau.LOI_SO_DIEN_THOAI), QuyTacMatKhau.kiemTra("a0912345678", "+84 912 345 678"));
        assertEquals(List.of(QuyTacMatKhau.LOI_SO_DIEN_THOAI), QuyTacMatKhau.kiemTra("abc09884786", "09884786"));
        assertTrue(QuyTacMatKhau.hopLe("a091234567", SDT)); // chỉ trùng một phần
    }

    @Test
    void soDienThoaiTrongHoacQuaNganThiBoQua() {
        assertTrue(QuyTacMatKhau.hopLe("abc12345", null));
        assertTrue(QuyTacMatKhau.hopLe("abc12345", ""));
        assertTrue(QuyTacMatKhau.hopLe("abc12345", "123")); // số quá ngắn không coi là số điện thoại
    }

    @Test
    void baoTatCaTieuChiChuaDatTheoThuTu() {
        assertEquals(List.of(QuyTacMatKhau.LOI_DO_DAI, QuyTacMatKhau.LOI_CHU_CAI, QuyTacMatKhau.LOI_CHU_SO),
                QuyTacMatKhau.kiemTra("", null));
        assertEquals(List.of(QuyTacMatKhau.LOI_DO_DAI, QuyTacMatKhau.LOI_CHU_CAI, QuyTacMatKhau.LOI_CHU_SO),
                QuyTacMatKhau.kiemTra(null, null));
    }

    @Test
    void batBuocNemLoiDauTien() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> QuyTacMatKhau.batBuoc("123456", SDT));
        assertEquals(QuyTacMatKhau.LOI_DO_DAI, e.getMessage());
        assertDoesNotThrow(() -> QuyTacMatKhau.batBuoc("matkhau1", SDT));
    }

    @Test
    void matKhauNgauNhienLuonDatQuyTac() {
        for (int i = 0; i < 500; i++) {
            String mk = CapTaiKhoanCtvService.taoMatKhauNgauNhien();
            assertTrue(QuyTacMatKhau.hopLe(mk, SDT), mk);
        }
    }
}
