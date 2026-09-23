package com.example.API;

import com.example.DTO.request.ReviewCreateRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Model.DanhGia;
import com.example.Service.CustomerApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * UC-KH07: Đánh giá và phản hồi dịch vụ
 */
@RestController
@RequestMapping("/v1/reviews")
@CrossOrigin(origins = "*")
public class ReviewApiController {

    private final CustomerApiService customerApiService;

    public ReviewApiController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-6: Khách hàng gửi đánh giá sao và nhận xét cho đơn hoàn thành
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        try {
            Map<String, Object> data = customerApiService.createReview(request);
            return ResponseEntity.ok(ApiResponse.ok("Gửi đánh giá thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi gửi đánh giá: " + e.getMessage()));
        }
    }

    /**
     * Xem các đánh giá công khai của một dịch vụ
     */
    @GetMapping("/service/{dichVuId}")
    public ResponseEntity<ApiResponse<List<DanhGia>>> getReviewsByService(@PathVariable Integer dichVuId) {
        try {
            List<DanhGia> list = customerApiService.getReviewsByService(dichVuId);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy danh sách đánh giá: " + e.getMessage()));
        }
    }
}
