package com.example.API;

import com.example.DTO.response.ApiResponse;
import com.example.Model.KhuVuc;
import com.example.Model.LoaiDichVu;
import com.example.Service.CustomerApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * UC-KH03: Xem thông tin dịch vụ, bảng giá, danh mục và khu vực
 */
@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class ServiceCatalogController {

    private final CustomerApiService customerApiService;

    public ServiceCatalogController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-3 & Biến thể tìm kiếm: Lấy danh sách dịch vụ kèm bộ lọc
     */
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getServices(
            @RequestParam(required = false) Integer loaiDichVuId,
            @RequestParam(required = false) Integer khuVucId,
            @RequestParam(required = false) String loaiHinhDat,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        try {
            List<Map<String, Object>> services = customerApiService.getServices(
                    loaiDichVuId, khuVucId, loaiHinhDat, tuKhoa, minPrice, maxPrice);
            return ResponseEntity.ok(ApiResponse.ok(services));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy danh sách dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 4-6: Chi tiết dịch vụ kèm bảng giá, gói tháng, CTV và đánh giá
     */
    @GetMapping("/services/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServiceDetail(@PathVariable Integer id) {
        try {
            Map<String, Object> data = customerApiService.getServiceDetail(id);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy chi tiết dịch vụ: " + e.getMessage()));
        }
    }

    /**
     * Danh mục loại dịch vụ
     */
    @GetMapping("/service-types")
    public ResponseEntity<ApiResponse<List<LoaiDichVu>>> getServiceTypes() {
        return ResponseEntity.ok(ApiResponse.ok(customerApiService.getServiceTypes()));
    }

    /**
     * Danh mục khu vực phục vụ
     */
    @GetMapping("/areas")
    public ResponseEntity<ApiResponse<List<KhuVuc>>> getAreas() {
        return ResponseEntity.ok(ApiResponse.ok(customerApiService.getAreas()));
    }
}
