package com.example.Controller;

import com.example.Service.RealtimeService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Kênh nhận sự kiện thời gian thực cho các trang quản trị: GET /realtime/stream (Server-Sent Events).
 * Chỉ nhân viên đã đăng nhập mới được kết nối.
 */
@RestController
public class RealtimeController {

    private final RealtimeService realtimeService;

    public RealtimeController(RealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @GetMapping(value = "/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(HttpSession session, HttpServletResponse response) {
        Object role = session.getAttribute("userRole");
        if (!(role instanceof String s) || s.isBlank()) {
            return ResponseEntity.status(403).build();
        }
        // Không cho proxy (nginx...) gom/đệm dữ liệu, để sự kiện tới trình duyệt ngay
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        return ResponseEntity.ok(realtimeService.dangKy());
    }
}
