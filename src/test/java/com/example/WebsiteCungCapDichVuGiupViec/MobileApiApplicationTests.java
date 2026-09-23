package com.example.WebsiteCungCapDichVuGiupViec;

import com.example.DTO.request.*;
import com.example.DTO.response.ApiResponse;
import com.example.DTO.response.PriceCalculationResult;
import com.example.Service.CollaboratorApiService;
import com.example.Service.CustomerApiService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@SpringBootTest
class MobileApiApplicationTests {

    @Autowired
    private CustomerApiService customerApiService;

    @Autowired
    private CollaboratorApiService collaboratorApiService;

    @Test
    void testMobileApiCustomerAndCollaboratorFlows() {
        long testSuffix = System.currentTimeMillis() % 1000000;

        // 1. UC-KH01: Đăng ký khách hàng & xác thực OTP
        CustomerRegisterRequest regReq = new CustomerRegisterRequest();
        regReq.setHoTen("Nguyễn Văn Test " + testSuffix);
        regReq.setSoDienThoai("098" + testSuffix);
        regReq.setEmail("test" + testSuffix + "@mobile.test");
        regReq.setMatKhau("123456");
        regReq.setDiaChiChiTiet("123 Đường Test, Quận 1, TP.HCM");

        Map<String, Object> regRes = customerApiService.registerCustomer(regReq);
        Assertions.assertNotNull(regRes.get("taiKhoanId"));
        Assertions.assertNotNull(regRes.get("otpCode"));

        String otpCode = (String) regRes.get("otpCode");
        OtpVerifyRequest otpReq = new OtpVerifyRequest();
        otpReq.setIdentifier(regReq.getSoDienThoai());
        otpReq.setMaCode(otpCode);
        otpReq.setMucDich("DangKy");

        Map<String, Object> verifyRes = customerApiService.verifyOtp(otpReq);
        Assertions.assertEquals(true, verifyRes.get("verified"));

        // 2. UC-KH03: Lấy danh mục dịch vụ
        List<Map<String, Object>> services = customerApiService.getServices(null, null, null, null, null, null);
        Assertions.assertNotNull(services);
        Assertions.assertFalse(services.isEmpty(), "Danh sách dịch vụ không được rỗng");
        Integer testDichVuId = (Integer) services.get(0).get("id");

        Map<String, Object> serviceDetail = customerApiService.getServiceDetail(testDichVuId);
        Assertions.assertEquals(testDichVuId, serviceDetail.get("id"));

        // 3. UC-KH04: Tính giá & Đặt dịch vụ
        CalculatePriceRequest calcReq = new CalculatePriceRequest();
        calcReq.setDichVuId(testDichVuId);
        calcReq.setLoaiHinhDat("TheoLan");
        PriceCalculationResult priceResult = customerApiService.calculatePrice(calcReq);
        Assertions.assertTrue(priceResult.getChiPhiGoc().compareTo(BigDecimal.ZERO) > 0);

        Integer khId = (Integer) regRes.get("khachHangId");
        BookingCreateRequest bookReq = new BookingCreateRequest();
        bookReq.setKhachHangId(khId);
        bookReq.setDichVuId(testDichVuId);
        bookReq.setLoaiHinhDat("TheoLan");
        bookReq.setNgayThucHien(LocalDate.now().plusDays(1));
        bookReq.setGioBatDau(LocalTime.of(9, 0));
        bookReq.setDiaChiChiTiet("456 Lê Lợi, Quận 1");

        Map<String, Object> bookRes = customerApiService.createBooking(bookReq);
        Assertions.assertNotNull(bookRes.get("donDatId"));
        Assertions.assertNotNull(bookRes.get("hoaDonId"));
        Integer donDatId = (Integer) bookRes.get("donDatId");

        // 4. UC-KH05: Xem danh sách và chi tiết đơn
        List<Map<String, Object>> myBookings = customerApiService.getCustomerBookings(khId, null);
        Assertions.assertFalse(myBookings.isEmpty());

        Map<String, Object> bookingDetail = customerApiService.getBookingDetail(donDatId);
        Assertions.assertEquals("ChoDuyet", bookingDetail.get("trangThai"));

        // 5. UC-KH06: Hóa đơn & Biên lai thanh toán
        Map<String, Object> invoice = customerApiService.getInvoiceInfo(donDatId);
        Assertions.assertNotNull(invoice.get("maHoaDon"));
        Assertions.assertNotNull(invoice.get("thongTinNganHang"));

        PaymentReceiptRequest receiptReq = new PaymentReceiptRequest();
        receiptReq.setDonDatId(donDatId);
        receiptReq.setHinhThucThanhToan("ChuyenKhoan");
        Map<String, Object> receiptRes = customerApiService.createReceipt(receiptReq);
        Assertions.assertEquals("DaThanhToan", receiptRes.get("trangThaiThanhToan"));

        // 6. UC-KH02: Đăng ký ứng viên Cộng tác viên
        CollaboratorRegisterRequest ctvReq = new CollaboratorRegisterRequest();
        ctvReq.setHoTen("Lê Văn CTV " + testSuffix);
        ctvReq.setSoDienThoai("097" + testSuffix);
        ctvReq.setEmail("ctv" + testSuffix + "@mobile.test");
        ctvReq.setMatKhau("123456");
        ctvReq.setNoiCuTru("Quận Bình Thạnh, TP.HCM");
        ctvReq.setDanhSachDichVuId(List.of(testDichVuId));

        Map<String, Object> ctvRes = customerApiService.registerCollaborator(ctvReq);
        Assertions.assertNotNull(ctvRes.get("congTacVienId"));
        Assertions.assertEquals("ChoDuyet", ctvRes.get("trangThai"));

        // Tra cứu hồ sơ ứng tuyển
        Map<String, Object> ctvStatus = customerApiService.getCollaboratorApplicationStatus(ctvReq.getSoDienThoai());
        Assertions.assertEquals("ChoDuyet", ctvStatus.get("trangThai"));

        System.out.println("=== Tất cả các use case API Mobile UC-KH01 -> UC-KH09 & UC-CTV01 -> UC-CTV03 kiểm thử thành công! ===");
    }
}
