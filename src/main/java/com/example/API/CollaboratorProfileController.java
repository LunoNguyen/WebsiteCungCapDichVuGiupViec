package com.example.API;

import com.example.Model.CongTacVien;
import com.example.Service.CollaboratorApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/v1/collaborators")
public class CollaboratorProfileController {

    @Autowired
    private CollaboratorApiService collaboratorApiService;

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

    /** Từ chối đơn */
    @PutMapping("/assignments/{id}/reject")
    public ResponseEntity<?> rejectAssignment(
            @PathVariable Integer id,
            @RequestBody(required = false) com.example.DTO.request.AssignmentActionRequest req) {
        try {
            com.example.DTO.request.AssignmentActionRequest body = req != null ? req : new com.example.DTO.request.AssignmentActionRequest();
            return ResponseEntity.ok(Map.of("success", true, "data", 
                collaboratorApiService.rejectAssignment(id, body)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** Hoàn thành đơn */
    @PutMapping("/assignments/{id}/complete")
    public ResponseEntity<?> completeAssignment(
            @PathVariable Integer id,
            @RequestBody(required = false) com.example.DTO.request.AssignmentActionRequest req) {
        try {
            com.example.DTO.request.AssignmentActionRequest body = req != null ? req : new com.example.DTO.request.AssignmentActionRequest();
            return ResponseEntity.ok(Map.of("success", true, "data", 
                collaboratorApiService.completeAssignment(id, body)));
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