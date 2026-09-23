package com.example.API;

import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-KH09: Nhận thông báo của Khách hàng
 */
@RestController
@RequestMapping("/v1/customer/notifications")
@CrossOrigin(origins = "*")
public class CustomerNotificationController {

    private final CustomerApiService customerApiService;

    public CustomerNotificationController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-3 & Biến thể: Lấy toàn bộ danh sách thông báo của khách hàng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNotifications(@RequestParam Integer taiKhoanId) {
        try {
            List<Map<String, Object>> list = customerApiService.getCustomerNotifications(taiKhoanId);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy thông báo: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 4: Đánh dấu một thông báo đã đọc
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Integer id,
            @RequestParam Integer taiKhoanId) {
        try {
            customerApiService.markNotificationRead(id, taiKhoanId);
            return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu thông báo đã đọc.", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi cập nhật thông báo: " + e.getMessage()));
        }
    }

    /**
     * Đánh dấu tất cả thông báo đã đọc
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@RequestParam Integer taiKhoanId) {
        try {
            customerApiService.markAllNotificationsRead(taiKhoanId);
            return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu tất cả thông báo đã đọc.", null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi cập nhật tất cả thông báo: " + e.getMessage()));
        }
    }
}
