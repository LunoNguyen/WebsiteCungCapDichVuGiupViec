package com.example.API;

import com.example.DTO.request.ComplaintCreateRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-KH08: Gửi khiếu nại và tra cứu tiến độ xử lý
 */
@RestController
@RequestMapping("/v1/complaints")
@CrossOrigin(origins = "*")
public class ComplaintApiController {

    private final CustomerApiService customerApiService;

    public ComplaintApiController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-4: Khách hàng gửi khiếu nại kèm link file/ảnh bằng chứng (lưu trên MinIO)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createComplaint(@Valid @RequestBody ComplaintCreateRequest request) {
        try {
            Map<String, Object> data = customerApiService.createComplaint(request);
            return ResponseEntity.ok(ApiResponse.ok("Gửi khiếu nại thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi gửi khiếu nại: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 5-6: Xem danh sách khiếu nại và kết quả giải quyết của khách hàng
     */
    @GetMapping("/customer/{khachHangId}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCustomerComplaints(@PathVariable Integer khachHangId) {
        try {
            List<Map<String, Object>> list = customerApiService.getCustomerComplaints(khachHangId);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy danh sách khiếu nại: " + e.getMessage()));
        }
    }
}
