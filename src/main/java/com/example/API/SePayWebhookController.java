package com.example.API;

import com.example.DTO.request.SePayWebhookRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Model.HoaDon;
import com.example.Repository.HoaDonRepository;
import com.example.Service.SePayPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API nhận Webhook từ SePay và cung cấp API giả lập test miễn phí.
 * API giả lập (POST /mock-pay) đánh dấu đơn đã thanh toán mà không cần đăng nhập, nên chỉ chạy khi
 * app.thanh-toan.cho-phep-gia-lap=true (mặc định tắt: trả về 404).
 */
@RestController
@RequestMapping("/api/v1/sepay")
@CrossOrigin(origins = "*")
public class SePayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(SePayWebhookController.class);

    private final SePayPaymentService sePayPaymentService;
    private final HoaDonRepository hoaDonRepository;
    private final boolean choPhepGiaLap;

    public SePayWebhookController(SePayPaymentService sePayPaymentService,
                                  HoaDonRepository hoaDonRepository,
                                  @Value("${app.thanh-toan.cho-phep-gia-lap:false}") boolean choPhepGiaLap) {
        this.sePayPaymentService = sePayPaymentService;
        this.hoaDonRepository = hoaDonRepository;
        this.choPhepGiaLap = choPhepGiaLap;
    }

    /**
     * Webhook nhận biến động số dư từ SePay
     * URL Webhook cấu hình trên SePay Dashboard: https://<domain-tunnel>/api/v1/sepay/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebhook(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody SePayWebhookRequest request) {

        // 1. Xác thực Token nếu có cấu hình
        if (!sePayPaymentService.verifyToken(authHeader)) {
            log.warn("[SePay Webhook] Từ chối webhook do sai API Token!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            Map<String, Object> result = sePayPaymentService.processWebhook(request);
            // SePay yêu cầu trả về status 200 OK cùng { "success": true } để xác nhận webhook đã nhận
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            log.warn("[SePay Webhook] Lỗi nghiệp vụ: {}", e.getMessage());
            // Trả về 200 kèm success:false để SePay không gửi lại nếu là lỗi do đơn đã thanh toán hoặc đơn huỷ
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("[SePay Webhook] Lỗi hệ thống khi xử lý webhook: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * API giả lập thanh toán (Dành cho việc test chức năng miễn phí không tốn tiền thật và không tốn quota SePay)
     */
    @PostMapping("/mock-pay/{donDatId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mockPay(@PathVariable Integer donDatId) {
        if (!choPhepGiaLap) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy"));
        }
        try {
            Map<String, Object> data = sePayPaymentService.mockPayment(donDatId);
            return ResponseEntity.ok(ApiResponse.ok("Giả lập thanh toán thành công", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi giả lập thanh toán: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra trạng thái thanh toán của đơn (dùng cho polling real-time trên Web và App)
     */
    @GetMapping("/status/{donDatId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(@PathVariable Integer donDatId) {
        HoaDon hd = hoaDonRepository.findByDonDat_Id(donDatId).orElse(null);
        if (hd == null) {
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "donDatId", donDatId,
                    "daThanhToan", false,
                    "trangThaiThanhToan", "ChuaCoHoaDon"
            )));
        }
        boolean daThanhToan = "DaThanhToan".equalsIgnoreCase(hd.getTrangThaiThanhToan());
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "donDatId", donDatId,
                "daThanhToan", daThanhToan,
                "trangThaiThanhToan", hd.getTrangThaiThanhToan(),
                "hinhThucThanhToan", hd.getHinhThucThanhToan() != null ? hd.getHinhThucThanhToan() : ""
        )));
    }
}
