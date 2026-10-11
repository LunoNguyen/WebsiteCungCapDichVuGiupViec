package com.example.API;

import com.example.DTO.response.ApiResponse;
import com.example.Service.CollaboratorApiService;
import com.example.Service.CustomerApiService;
import com.example.Service.RealtimeService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * Realtime cho ứng dụng di động:
 * - GET /v1/realtime/stream?taiKhoanId=&vaiTro=KhachHang|CongTacVien (Server-Sent Events):
 *   sự kiện "thong-bao-moi" khi tài khoản có thông báo mới, "nhip" giữ kết nối.
 * - GET /v1/notifications/unread-count?taiKhoanId=&vaiTro=: số thông báo chưa đọc (badge trên biểu tượng chuông).
 */
@RestController
@RequestMapping("/v1")
@CrossOrigin(origins = "*")
public class AppRealtimeController {

    private final RealtimeService realtimeService;
    private final CustomerApiService customerApiService;
    private final CollaboratorApiService collaboratorApiService;

    public AppRealtimeController(RealtimeService realtimeService, CustomerApiService customerApiService,
                                 CollaboratorApiService collaboratorApiService) {
        this.realtimeService = realtimeService;
        this.customerApiService = customerApiService;
        this.collaboratorApiService = collaboratorApiService;
    }

    @GetMapping(value = "/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam Integer taiKhoanId,
                             @RequestParam(defaultValue = "KhachHang") String vaiTro,
                             HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return realtimeService.dangKyApp(taiKhoanId, "CongTacVien".equals(vaiTro) ? "CongTacVien" : "KhachHang");
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> soChuaDoc(@RequestParam Integer taiKhoanId,
                                                                      @RequestParam(defaultValue = "KhachHang") String vaiTro) {
        // Dùng chung hàm lấy danh sách (đồng bộ cả thông báo gửi theo nhóm) để số khớp với màn thông báo
        var ds = "CongTacVien".equals(vaiTro)
                ? collaboratorApiService.getCollaboratorNotifications(taiKhoanId)
                : customerApiService.getCustomerNotifications(taiKhoanId);
        long chuaDoc = ds.stream().filter(m -> !Boolean.TRUE.equals(m.get("daDoc"))).count();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("soChuaDoc", chuaDoc)));
    }
}
