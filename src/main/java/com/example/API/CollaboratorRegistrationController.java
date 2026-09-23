package com.example.API;

import com.example.DTO.request.CollaboratorRegisterRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CustomerApiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * UC-KH02: Đăng ký làm cộng tác viên (Ứng viên)
 */
@RestController
@RequestMapping("/v1/collaborators")
@CrossOrigin(origins = "*")
public class CollaboratorRegistrationController {

    private final CustomerApiService customerApiService;

    public CollaboratorRegistrationController(CustomerApiService customerApiService) {
        this.customerApiService = customerApiService;
    }

    /**
     * Dòng sự kiện 1-5: Ứng viên gửi hồ sơ đăng ký kèm file MinIO (CCCD, chứng chỉ, ...)
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerCollaborator(@Valid @RequestBody CollaboratorRegisterRequest request) {
        try {
            Map<String, Object> data = customerApiService.registerCollaborator(request);
            return ResponseEntity.ok(ApiResponse.ok("Gửi hồ sơ đăng ký cộng tác viên thành công.", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi gửi hồ sơ CTV: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 7: Tra cứu kết quả xét duyệt hồ sơ theo số điện thoại
     */
    @GetMapping("/application-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(@RequestParam String soDienThoai) {
        try {
            Map<String, Object> data = customerApiService.getCollaboratorApplicationStatus(soDienThoai);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi tra cứu hồ sơ: " + e.getMessage()));
        }
    }
}
