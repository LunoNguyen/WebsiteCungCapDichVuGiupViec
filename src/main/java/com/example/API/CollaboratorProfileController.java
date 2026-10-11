package com.example.API;

import com.example.DTO.request.AssignmentActionRequest;
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

    @Autowired
    private com.example.Service.KhuVucCtvService khuVucCtvService;

    /** Quận, huyện CTV đang nhận việc. */
    @GetMapping("/{id}/areas")
    public ResponseEntity<?> getAreas(@PathVariable Integer id) {
        return ResponseEntity.ok(Map.of("success", true,
                "data", com.example.Service.KhuVucCtvService.toMap(khuVucCtvService.dangNhan(id))));
    }

    /** Đặt lại khu vực nhận việc, body: { "khuVucIds": [1, 2] } (1 đến 5 quận, huyện). */
    @PutMapping("/{id}/areas")
    public ResponseEntity<?> updateAreas(@PathVariable Integer id, @RequestBody Map<String, java.util.List<Integer>> req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật khu vực nhận việc.",
                    "data", com.example.Service.KhuVucCtvService.toMap(khuVucCtvService.capNhat(id, req.get("khuVucIds")))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
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

    /** Hoàn thành đơn, body tuỳ chọn: { "ketQuaThucHien": "..." } */
    @PutMapping("/assignments/{id}/complete")
    public ResponseEntity<?> completeAssignment(@PathVariable Integer id,
                                                @RequestBody(required = false) AssignmentActionRequest req) {
        try {
            AssignmentActionRequest body = req != null ? req : new AssignmentActionRequest();
            return ResponseEntity.ok(Map.of("success", true, "data", collaboratorApiService.completeAssignment(id, body)));
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

    /** GPS: app CTV gửi vị trí hiện tại mỗi 5 giây, lưu trên Redis (ViTriCtvStore) */
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Integer id, @jakarta.validation.Valid @RequestBody com.example.DTO.request.LocationRequest req) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã cập nhật vị trí.",
                    "data", collaboratorApiService.updateLocation(id, req.getViDo(), req.getKinhDo())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
