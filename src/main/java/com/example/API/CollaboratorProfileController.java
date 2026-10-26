package com.example.API;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.DTO.request.LocationRequest;
import com.example.Model.CongTacVien;
import com.example.Service.CollaboratorApiService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/v1/collaborators")
public class CollaboratorProfileController {

    @Autowired
    private CollaboratorApiService collaboratorApiService;

    @Autowired
    private com.example.Service.KhuVucCtvService khuVucCtvService;

    /** Quận, huyện CTV đang nhận việc. */
    @GetMapping("/{id}/areas")
    public ResponseEntity<?> getAreas(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of("success", true,
                "data", com.example.Service.KhuVucCtvService.toMap(khuVucCtvService.dangNhan(id))));
    }

    /** Khu vực nhận việc không còn chọn tay: tự cập nhật theo quận CTV đang đứng (PUT /{id}/location). */
    @PutMapping("/{id}/areas")
    public ResponseEntity<?> updateAreas(@PathVariable Integer id) {
        return ResponseEntity.badRequest().body(Map.of("success", false,
                "message", "Khu vực nhận việc tự cập nhật theo vị trí GPS."));
    }

    /** Lấy danh sách đơn hàng */
    @GetMapping("/assignments")
    public ResponseEntity<?> getAssignments(@RequestParam Integer congTacVienId, @RequestParam(required = false) String trangThai) {
        return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.getAssignments(congTacVienId, trangThai)));
    }

    /** Lấy lịch làm việc */
    @GetMapping("/schedules")
    public ResponseEntity<?> getSchedules(@RequestParam Integer congTacVienId) {
        return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.getSchedules(congTacVienId, null, null, null)));
    }

    /** Nhận đơn */
    @PutMapping("/assignments/{id}/accept")
    public ResponseEntity<?> acceptAssignment(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.acceptAssignment(id)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Bắt đầu ca làm việc (yêu cầu kèm ảnh tài sản Trước khi thực hiện dịch vụ) */
    @PutMapping("/assignments/{id}/start")
    public ResponseEntity<?> startAssignment(@PathVariable Integer id,
                                             @RequestBody(required = false) AssignmentActionRequest req) {
        try {
            AssignmentActionRequest body = req != null ? req : new AssignmentActionRequest();
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.startAssignment(id, body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Xử lý ca làm quá hạn sau khi CTV thống nhất với Khách hàng */
    @PutMapping("/assignments/{id}/resolve-overdue")
    public ResponseEntity<?> resolveOverdueAssignment(@PathVariable Integer id,
                                                      @RequestBody Map<String, Object> req) {
        try {
            Map<String, Object> data = collaboratorApiService.resolveOverdueAssignment(id, req);
            String msg = data.get("message") != null ? data.get("message").toString() : "Đã xử lý ca quá hạn thành công!";
            return ResponseEntity.ok(Map.of("success", true, "message", msg, "data", data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Từ chối đơn, body tuỳ chọn: { "lyDoTuChoi": "..." } */
    @PutMapping("/assignments/{id}/reject")
    public ResponseEntity<?> rejectAssignment(@PathVariable Integer id,
                                              @RequestBody(required = false) AssignmentActionRequest req) {
        try {
            AssignmentActionRequest body = req != null ? req : new AssignmentActionRequest();
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.rejectAssignment(id, body)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Hoàn thành đơn, body yêu cầu: danhSachHinhAnh (SauDichVu), ketQuaThucHien */
    @PutMapping("/assignments/{id}/complete")
    public ResponseEntity<?> completeAssignment(@PathVariable Integer id,
                                                @RequestBody(required = false) AssignmentActionRequest req) {
        try {
            AssignmentActionRequest body = req != null ? req : new AssignmentActionRequest();
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.completeAssignment(id, body)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Lấy thông tin hồ sơ */
    @GetMapping("/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.getCollaboratorProfile(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Lấy hồ sơ theo tài khoản, chưa có thì tạo hồ sơ rỗng (dùng sau khi CTV đăng nhập app) */
    @PostMapping("/profile/ensure")
    public ResponseEntity<?> ensureProfile(@RequestBody Map<String, Integer> req) {
        Integer taiKhoanId = req.get("taiKhoanId");
        if (taiKhoanId == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu taiKhoanId"));
        }
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.ensureCollaboratorProfile(taiKhoanId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Cập nhật trạng thái Profile */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestBody Map<String, String> req) {
        return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.updateCollaboratorStatus(id, req.get("trangThai"))));
    }

    /** GPS: app CTV gửi vị trí hiện tại mỗi 10 giây, lưu trên Redis (ViTriCtvStore) & đồng bộ DB khi đổi khu vực */
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Integer id, @Valid @RequestBody LocationRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật vị trí.",
                    "data", collaboratorApiService.updateLocation(id, req.getViDo(), req.getKinhDo(),
                            req.getQuanHuyen(), req.getTinhThanh(), req.getDiaChi())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** [POOL] Lấy danh sách đơn đang tìm CTV */
    @GetMapping("/available-orders")
    public ResponseEntity<?> getAvailableOrders(@RequestParam Integer congTacVienId) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data",
                    collaboratorApiService.getAvailableOrders(congTacVienId)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** [POOL] CTV nhận đơn từ pool (atomic) */
    @PostMapping("/orders/{donId}/accept")
    public ResponseEntity<?> acceptOrderFromPool(
            @PathVariable Integer donId,
            @RequestBody Map<String, Integer> req) {
        try {
            Integer congTacVienId = req.get("congTacVienId");
            if (congTacVienId == null) congTacVienId = req.get("taiKhoanId");
            if (congTacVienId == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu congTacVienId"));
            return ResponseEntity.ok(Map.of("success", true, "data",
                    collaboratorApiService.acceptOrderFromPool(congTacVienId, donId)));
        } catch (IllegalStateException e) {
            // Đơn đã có người nhận → 409 Conflict
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** [POOL] CTV từ chối đơn từ pool */
    @PostMapping("/orders/{donId}/reject")
    public ResponseEntity<?> rejectOrderFromPool(
            @PathVariable Integer donId,
            @RequestBody Map<String, Object> req) {
        try {
            Object ctvIdObj = req.get("congTacVienId") != null ? req.get("congTacVienId") : req.get("taiKhoanId");
            if (ctvIdObj == null) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Thiếu congTacVienId"));
            Integer congTacVienId = Integer.valueOf(ctvIdObj.toString());
            String lyDo = req.getOrDefault("lyDo", "").toString();
            return ResponseEntity.ok(Map.of("success", true, "data",
                    collaboratorApiService.rejectOrderFromPool(congTacVienId, donId, lyDo)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** [TEST] Tạo đơn pool test + thông báo broadcast tất cả CTV */
    @PostMapping("/test-create-pool-order")
    public ResponseEntity<?> createTestPoolOrder(@RequestBody(required = false) Map<String, Object> req) {
        try {
            String maDon = (req != null && req.get("maDon") != null) ? req.get("maDon").toString() : null;
            String tenDichVu = (req != null && req.get("tenDichVu") != null) ? req.get("tenDichVu").toString() : "Dọn dẹp nhà cửa";
            Integer thanhTien = (req != null && req.get("thanhTien") != null) ? Integer.valueOf(req.get("thanhTien").toString()) : 320000;
            return ResponseEntity.ok(Map.of("success", true, "data",
                    collaboratorApiService.createTestPoolOrder(maDon, tenDichVu, thanhTien)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
