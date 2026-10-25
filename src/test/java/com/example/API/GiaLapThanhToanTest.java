package com.example.API;

import com.example.Service.SePayPaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** API giả lập thanh toán chỉ chạy khi bật app.thanh-toan.cho-phep-gia-lap (mặc định tắt). */
class GiaLapThanhToanTest {

    @Test
    void macDinhTatThiTraVe404VaKhongDanhDauDon() {
        SePayPaymentService thanhToan = mock(SePayPaymentService.class);
        var api = new SePayWebhookController(thanhToan, null, false);

        var res = api.mockPay(5);

        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
        verifyNoInteractions(thanhToan);
    }

    @Test
    void batCauHinhThiGiaLapDuoc() {
        SePayPaymentService thanhToan = mock(SePayPaymentService.class);
        when(thanhToan.mockPayment(5)).thenReturn(Map.of("donDatId", 5));
        var api = new SePayWebhookController(thanhToan, null, true);

        var res = api.mockPay(5);

        assertEquals(HttpStatus.OK, res.getStatusCode());
        verify(thanhToan).mockPayment(5);
    }
}
