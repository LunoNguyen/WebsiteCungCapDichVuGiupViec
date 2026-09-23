package com.example.API;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.DTO.response.ApiResponse;
import com.example.Service.CollaboratorApiService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * UC-CTV01: Quản lý đơn được phân công (xem, nhận, từ chối, hoàn thành)
 * UC-CTV03: Xem thời gian biểu làm việc (lịch ngày/tuần/tháng)
 */
@RestController
@RequestMapping("/v1/collaborator")
@CrossOrigin(origins = "*")
public class CollaboratorJobController {

    private final CollaboratorApiService collaboratorApiService;

    public CollaboratorJobController(CollaboratorApiService collaboratorApiService) {
        this.collaboratorApiService = collaboratorApiService;
    }

    /**
     * Dòng sự kiện 1-3 (UC-CTV01): Danh sách đơn dịch vụ được phân công
     */
    @GetMapping("/assignments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignments(
            @RequestParam Integer congTacVienId,
            @RequestParam(required = false) String trangThai) {
        try {
            List<Map<String, Object>> list = collaboratorApiService.getAssignments(congTacVienId, trangThai);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy danh sách phân công: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 4 (UC-CTV01): Xem chi tiết đơn được phân công
     */
    @GetMapping("/assignments/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAssignmentDetail(@PathVariable Integer id) {
        try {
            Map<String, Object> data = collaboratorApiService.getAssignmentDetail(id);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy chi tiết phân công: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 4-5 (UC-CTV01): CTV xác nhận nhận đơn
     */
    @PutMapping("/assignments/{id}/accept")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptAssignment(@PathVariable Integer id) {
        try {
            Map<String, Object> res = collaboratorApiService.acceptAssignment(id);
            return ResponseEntity.ok(ApiResponse.ok("Đã nhận đơn thành công.", res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi nhận đơn: " + e.getMessage()));
        }
    }

    /**
     * Biến thể (UC-CTV01): CTV từ chối đơn kèm lý do
     */
    @PutMapping("/assignments/{id}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rejectAssignment(
            @PathVariable Integer id,
            @RequestBody(required = false) AssignmentActionRequest req) {
        try {
            AssignmentActionRequest body = req != null ? req : new AssignmentActionRequest();
            Map<String, Object> res = collaboratorApiService.rejectAssignment(id, body);
            return ResponseEntity.ok(ApiResponse.ok("Đã từ chối đơn.", res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi từ chối đơn: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 6-7 (UC-CTV01): CTV cập nhật trạng thái Hoàn thành kèm kết quả thực hiện
     */
    @PutMapping("/assignments/{id}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeAssignment(
            @PathVariable Integer id,
            @RequestBody(required = false) AssignmentActionRequest req) {
        try {
            AssignmentActionRequest body = req != null ? req : new AssignmentActionRequest();
            Map<String, Object> res = collaboratorApiService.completeAssignment(id, body);
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật hoàn thành đơn thành công.", res));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi cập nhật hoàn thành đơn: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 1-3 & Biến thể (UC-CTV03): Xem thời gian biểu làm việc (ngày/tuần/tháng)
     */
    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSchedules(
            @RequestParam Integer congTacVienId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String trangThai) {
        try {
            List<Map<String, Object>> list = collaboratorApiService.getSchedules(congTacVienId, fromDate, toDate, trangThai);
            return ResponseEntity.ok(ApiResponse.ok(list));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy thời gian biểu: " + e.getMessage()));
        }
    }

    /**
     * Dòng sự kiện 4 (UC-CTV03): Xem chi tiết ca làm việc
     */
    @GetMapping("/schedules/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getScheduleDetail(@PathVariable Integer id) {
        try {
            Map<String, Object> data = collaboratorApiService.getScheduleDetail(id);
            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi lấy chi tiết ca làm việc: " + e.getMessage()));
        }
    }
}
