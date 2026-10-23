package com.example.API;

import com.example.DTO.response.ApiResponse;
import com.example.Service.DiaGioiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Danh mục địa giới cho form địa chỉ (web và app dùng chung):
 * tỉnh, thành → phường, xã (provinces.open-api.vn v2), mỗi phường kèm khu vực (quận, huyện) tương ứng.
 */
@RestController
@RequestMapping("/v1/dia-gioi")
@CrossOrigin(origins = "*")
public class DiaGioiController {

    private final DiaGioiService diaGioiService;

    public DiaGioiController(DiaGioiService diaGioiService) {
        this.diaGioiService = diaGioiService;
    }

    @GetMapping("/tinh")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tinh() {
        return ResponseEntity.ok(ApiResponse.ok(diaGioiService.tinhThanh()));
    }

    /** Phường, xã của tỉnh: [{ma, ten, khuVuc: [{id, tenKhuVuc}]}]; khuVuc rỗng = ngoài khu vực phục vụ. */
    @GetMapping("/tinh/{maTinh}/phuong-xa")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> phuongXa(@PathVariable int maTinh) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(diaGioiService.phuongXa(maTinh)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(ApiResponse.error(e.getMessage()));
        }
    }

    // =========================================================================
    // ENDPOINTS V1 (ĐỊA CHỈ TRUYỀN THỐNG 3 CẤP: TỈNH -> QUẬN -> PHƯỜNG)
    // =========================================================================

    @GetMapping("/v1/tinh")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tinhV1() {
        return ResponseEntity.ok(ApiResponse.ok(diaGioiService.tinhThanhV1()));
    }

    @GetMapping("/v1/tinh/{maTinh}/quan-huyen")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> quanHuyenV1(@PathVariable int maTinh) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(diaGioiService.quanHuyenV1(maTinh)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/v1/quan-huyen/{maQuan}/phuong-xa")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> phuongXaV1(@PathVariable int maQuan) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(diaGioiService.phuongXaV1(maQuan)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(ApiResponse.error(e.getMessage()));
        }
    }
}
