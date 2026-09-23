package com.example.API;

import com.example.DTO.request.BookingCreateRequest;
import com.example.DTO.request.CalculatePriceRequest;
import com.example.DTO.request.ValidatePromotionRequest;
import com.example.DTO.response.ApiResponse;
import com.example.DTO.response.PriceCalculationResult;
import com.example.Service.CustomerApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-KH04: Đặt dịch vụ, tính giá & khuyến mãi
 * UC-KH05: Theo dõi lịch làm việc & lịch sử đơn
 */
@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class CustomerBookingController {

    private final CustomerApiService customerApiService;

    public CustomerBookingController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 4-6 (UC-KH04): Tính toán chi phí trước khi đặt (hỗ trợ mã giảm giá)
     */
    @PostMapping("/bookings/calculate-price")
    public ResponseEntity<ApiResponse<PriceCalculationResult>> calculatePrice(@Valid @RequestBody CalculatePriceRequest request) {
        try {
            PriceCalculationResult result = customerApiService.calculatePrice(request);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi tính giá: " + e.getMessage()));
        }
    }

    /**
     * Kiểm tra tính hợp lệ của mã khuyến mại
     */
    @PostMapping("/promotions/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validatePromotion(@Valid @RequestBody ValidatePromotionRequest request) {
        try {
            Map<String, Object> result = customerApiService.validatePromotion(request);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi kiểm tra mã khuyến mãi: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 7-9 (UC-KH04): Khách hàng xác nhận và tạo đơn đặt dịch vụ
     */
    @PostMapping("/bookings")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        try {
            Map<String, Object> result = customerApiService.createBooking(request);
            return ResponseEntity.ok(ApiResponse.ok("Đặt dịch vụ thành công.", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi tạo đơn đặt: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 1-3 (UC-KH05): Lấy danh sách lịch dịch vụ của khách hàng
     */
    @GetMapping("/customer/bookings")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCustomerBookings(
            @RequestParam Integer khachHangId,
            @RequestParam(required = false) String trangThai) {
        try {
            List<Map<String, Object>> list = customerApiService.getCustomerBookings(khachHangId, trangThai);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy danh sách đơn: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 4-5 (UC-KH05): Xem chi tiết đơn đặt dịch vụ
     */
    @GetMapping("/customer/bookings/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookingDetail(@PathVariable Integer id) {
        try {
            Map<String, Object> data = customerApiService.getBookingDetail(id);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy chi tiết đơn: " + e.getMessage()));
        }
    }

    /**
     * Biến thể UC-KH05: Hủy đơn đặt khi ở trạng thái "Chờ xác nhận"
     */
    @PutMapping("/customer/bookings/{id}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelBooking(
            @PathVariable Integer id,
            @RequestParam Integer khachHangId,
            @RequestParam(required = false) String lyDo) {
        try {
            Map<String, Object> res = customerApiService.cancelBooking(id, khachHangId, lyDo);
            return ResponseEntity.ok(ApiResponse.ok("Hủy đơn thành công.", res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi hủy đơn: " + e.getMessage()));
        }
    }
}
