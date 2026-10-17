package com.example.API;

import com.example.DTO.request.ReviewCreateRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerApiService;
import com.example.Service.DanhGiaService;
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
    private final DanhGiaService danhGiaService;

    public ReviewApiController(CustomerApiService customerApiService, DanhGiaService danhGiaService) {
        this.customerApiService = customerApiService;
        this.danhGiaService = danhGiaService;
    }

    /** Đánh giá của một đơn kèm trao đổi với CSKH (data = null nếu chưa đánh giá). */
    @GetMapping("/order/{donDatId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByOrder(@PathVariable Integer donDatId) {
        return ResponseEntity.ok(ApiResponse.ok(danhGiaService.cuaDon(donDatId)));
    }

    /** Khách sửa đánh giá (trong 7 ngày sau khi gửi). */
    @PutMapping("/order/{donDatId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateReview(@PathVariable Integer donDatId,
                                                                         @RequestBody ReviewCreateRequest request) {
        return xuLy("Đã cập nhật đánh giá.", () -> danhGiaService.suaDanhGia(donDatId, request.getKhachHangId(),
                request.getDiemChatLuong(), request.getDiemThaiDo(), request.getNhanXet()));
    }

    /** Khách gửi bình luận / phản hồi thêm dưới đánh giá của đơn. Body: {khachHangId, noiDung}. */
    @PostMapping("/order/{donDatId}/comments")
    public ResponseEntity<ApiResponse<Map<String, Object>>> comment(@PathVariable Integer donDatId,
                                                                    @RequestBody Map<String, Object> body) {
        Integer khachHangId = body.get("khachHangId") != null ? Integer.valueOf(body.get("khachHangId").toString()) : null;
        Object noiDung = body.get("noiDung");
        return xuLy("Đã gửi bình luận.", () -> danhGiaService.binhLuanCuaKhach(donDatId, khachHangId,
                noiDung != null ? noiDung.toString() : null));
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> xuLy(String thongBao,
                                                                 java.util.function.Supplier<Map<String, Object>> viec) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(thongBao, viec.get()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
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
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReviewsByService(@PathVariable Integer dichVuId) {
        try {
            List<Map<String, Object>> list = customerApiService.getReviewsByService(dichVuId);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy danh sách đánh giá: " + e.getMessage()));
        }
    }
}
