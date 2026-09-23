package com.example.API;

import com.example.DTO.request.PaymentReceiptRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UC-KH06: Thanh toán, hóa đơn và biên lai
 */
@RestController
@RequestMapping("/v1/payments")
@CrossOrigin(origins = "*")
public class PaymentApiController {

    private final CustomerApiService customerApiService;

    public PaymentApiController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-3: Lấy thông tin hóa đơn & mã QR VietQR chuyển khoản
     */
    @GetMapping("/invoice/{donDatId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoice(@PathVariable Integer donDatId) {
        try {
            Map<String, Object> data = customerApiService.getInvoiceInfo(donDatId);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy thông tin hóa đơn: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 5-6 & Biến thể: Xác nhận giao dịch / lập biên lai thanh toán
     */
    @PostMapping("/create-receipt")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReceipt(@Valid @RequestBody PaymentReceiptRequest request) {
        try {
            Map<String, Object> data = customerApiService.createReceipt(request);
            return ResponseEntity.ok(ApiResponse.ok("Thanh toán thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi xác nhận thanh toán: " + e.getMessage()));
        }
    }
}
