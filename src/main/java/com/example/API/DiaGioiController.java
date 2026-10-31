package com.example.API;

import com.example.DTO.response.ApiResponse;
import com.example.Service.DiaGioiService;
import com.example.Service.TamQuanHcm;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final TamQuanHcm tamQuanHcm;

    @Autowired
    public DiaGioiController(DiaGioiService diaGioiService, TamQuanHcm tamQuanHcm) {
        this.diaGioiService = diaGioiService;
        this.tamQuanHcm = tamQuanHcm;
    }

    public DiaGioiController(DiaGioiService diaGioiService) {
        this(diaGioiService, new TamQuanHcm());
    }

    /** Danh sách tâm toạ độ GPS các quận/huyện TP.HCM từ file data/tam-quan-hcm.json. */
    @GetMapping("/tam-quan")
    public ResponseEntity<ApiResponse<List<TamQuanHcm.Tam>>> tamQuan() {
        return ResponseEntity.ok(ApiResponse.ok(tamQuanHcm.danhSach()));
    }

    /** Ô tìm địa chỉ: gõ tên tỉnh / quận, gợi ý cả địa giới sau sáp nhập và trước sáp nhập. */
    @GetMapping("/tim")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tim(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.ok(diaGioiService.timTinhQuan(q)));
    }

    /** Ô tỉnh của form địa chỉ: tỉnh sau sáp nhập + tỉnh trước sáp nhập (cu = true). */
    @GetMapping("/tinh-gop")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> tinhGop() {
        return ResponseEntity.ok(ApiResponse.ok(diaGioiService.tinhGop()));
    }

    /** Ô thứ hai: phường, xã của tỉnh mới, hoặc quận, huyện của tỉnh cũ (cu=true); kèm khu vực. */
    @GetMapping("/tinh-gop/{maTinh}/duoi")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> duoiTinh(@PathVariable int maTinh,
                                                                          @RequestParam(defaultValue = "false") boolean cu) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(diaGioiService.duoiTinh(maTinh, cu)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(ApiResponse.error(e.getMessage()));
        }
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
