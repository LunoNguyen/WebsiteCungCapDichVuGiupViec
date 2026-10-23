package com.example.API;

import com.example.DTO.response.ApiResponse;
import com.example.Service.ChinhSachService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Chính sách của bTaskee cho ứng dụng (cùng nội dung với trang web /chinh-sach):
 * Điều khoản sử dụng, Quy trình khiếu nại, Chính sách bảo mật. Không cần đăng nhập.
 */
@RestController
@RequestMapping("/v1/chinh-sach")
@CrossOrigin(origins = "*")
public class ChinhSachController {

    private final ChinhSachService chinhSachService;

    public ChinhSachController(ChinhSachService chinhSachService) {
        this.chinhSachService = chinhSachService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> danhSach() {
        return ResponseEntity.ok(ApiResponse.ok(chinhSachService.danhSach()));
    }
}
