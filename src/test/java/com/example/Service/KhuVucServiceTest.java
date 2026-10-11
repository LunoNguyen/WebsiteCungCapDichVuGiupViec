package com.example.Service;

import com.example.Model.KhuVuc;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Đoán quận, huyện từ địa chỉ cũ khi chuyển khu vực từ phường sang quận (không cần CSDL). */
class KhuVucServiceTest {

    private static final List<KhuVuc> QUAN = List.of(
            kv("Quận 1"), kv("Quận 3"), kv("Quận 10"), kv("Quận 12"),
            kv("Quận Bình Thạnh"), kv("Quận Tân Phú"), kv("Thành phố Thủ Đức"), kv("Huyện Củ Chi"));

    private static KhuVuc kv(String ten) {
        return KhuVuc.builder().tenKhuVuc(ten).quanHuyen(ten).build();
    }

    private static String doan(String diaChi) {
        KhuVucService s = new KhuVucService(null, null, null, null,
                new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
                    @Override protected Object doGetTransaction() { return new Object(); }
                    @Override protected void doBegin(Object t, org.springframework.transaction.TransactionDefinition d) { }
                    @Override protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus st) { }
                    @Override protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus st) { }
                });
        Optional<KhuVuc> q = s.doanQuan(diaChi, QUAN);
        return q.map(KhuVuc::getTenKhuVuc).orElse(null);
    }

    @Test
    void quanTheoSo() {
        assertEquals("Quận 3", doan("45 Lê Văn Sỹ, Phường 13, Quận 3, TP.HCM"));
        assertEquals("Quận 10", doan("12 Ba Tháng Hai, P.12, Q.10"));       // không nhầm thành Quận 1
        assertEquals("Quận 1", doan("72 Lê Thánh Tôn, Q1"));
        assertEquals("Quận 12", doan("số 5 đường TX25, quan 12"));
    }

    @Test
    void quanTheoTen() {
        // Có tiền tố "Quận" được ưu tiên hơn tên trơn ("Tan Phu" là tên phường cũ trong địa chỉ)
        assertEquals("Quận Bình Thạnh", doan("23/44 Nguyen Huu Tien, Tay Thanh, Tan Phu, Quận Bình Thạnh"));
        assertEquals("Thành phố Thủ Đức", doan("10 Võ Văn Ngân, Thủ Đức"));
        assertEquals("Huyện Củ Chi", doan("ấp 3, xã Tân An Hội, Củ Chi"));
    }

    @Test
    void khongDoanDuoc() {
        assertNull(doan("72 Lê Thánh Tôn, Phường Bến Thành, Thành phố Hồ Chí Minh"));
        assertNull(doan("12 Đường thử giao diện"));
    }
}
