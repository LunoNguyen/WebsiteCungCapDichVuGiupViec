package com.example.API;

import com.example.Service.MinioService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API quản lý file trên MinIO – Base URL: /api/storage
 *
 * Mọi API upload trả về:
 *   - objectName   : object key trong bucket → gửi giá trị này trong các request lưu dữ liệu
 *                    (duongDanFile, danhSachFileDinhKem...), đây là giá trị được lưu vào CSDL
 *   - url          : đường dẫn xem qua website (/files/<key>)
 *   - presignedUrl : URL tải trực tiếp từ MinIO, có thời hạn (dùng cho ứng dụng di động)
 */
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private final MinioService minioService;

    public StorageController(MinioService minioService) {
        this.minioService = minioService;
    }

    /**
     * POST /api/storage/upload
     * Param: file, folder (tùy chọn – vd "ctv/ho-so/", "khieu-nai/tai-lieu/")
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = MinioService.Folder.HO_SO) String folder) {
        // Thư mục công khai chỉ dành cho trang quản trị (ảnh danh mục...), không nhận qua API chung
        if (folder != null && folder.trim().replaceAll("^/+", "").startsWith(MinioService.Folder.CONG_KHAI)) {
            return loi(HttpStatus.BAD_REQUEST, "Không được tải file vào thư mục công khai qua API này.");
        }
        return upload(() -> minioService.uploadFile(file, folder), file);
    }

    /** Ảnh đại diện CTV – ghi đè ảnh cũ. */
    @PostMapping("/upload/ctv-avatar/{ctvId}")
    public ResponseEntity<Map<String, Object>> uploadCTVAvatar(
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer ctvId) {
        return upload(() -> minioService.uploadFile(file, MinioService.Folder.CTV_AVATAR, "ctv-" + ctvId), file);
    }

    /** Hồ sơ ứng viên CTV: ảnh chân dung, CCCD mặt trước/sau. */
    @PostMapping("/upload/ctv-ho-so")
    public ResponseEntity<Map<String, Object>> uploadHoSoCTV(@RequestParam("file") MultipartFile file) {
        return upload(() -> minioService.uploadFile(file, MinioService.Folder.CTV_HO_SO), file);
    }

    /** Chứng chỉ, bằng cấp của CTV. */
    @PostMapping({"/upload/ctv-chung-chi", "/upload/ctv-chung-chi/{ctvId}"})
    public ResponseEntity<Map<String, Object>> uploadChungChi(
            @RequestParam("file") MultipartFile file,
            @PathVariable(required = false) Integer ctvId) {
        return upload(() -> minioService.uploadFile(file, MinioService.Folder.CTV_CHUNG_CHI), file);
    }

    /** Ảnh/video bằng chứng khiếu nại. */
    @PostMapping({"/upload/khieu-nai", "/upload/khieu-nai/{khieuNaiId}"})
    public ResponseEntity<Map<String, Object>> uploadKhieuNai(
            @RequestParam("file") MultipartFile file,
            @PathVariable(required = false) Integer khieuNaiId) {
        return upload(() -> minioService.uploadFile(file, MinioService.Folder.KHIEU_NAI_TAILIEU), file);
    }

    /** Ảnh hiện trạng tài sản trước/sau dịch vụ. */
    @PostMapping("/upload/tai-san")
    public ResponseEntity<Map<String, Object>> uploadTaiSan(@RequestParam("file") MultipartFile file) {
        return upload(() -> minioService.uploadImage(file, MinioService.Folder.TAI_SAN), file);
    }

    /** GET /api/storage/url?object=ctv/ho-so/abc.jpg → URL để hiển thị file. */
    @GetMapping("/url")
    public ResponseEntity<Map<String, Object>> getUrl(@RequestParam("object") String objectName) {
        if (!minioService.fileExists(objectName)) {
            return loi(HttpStatus.NOT_FOUND, "Không tìm thấy file.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("objectName", minioService.toObjectKey(objectName));
        body.put("url", minioService.getPresignedUrl(objectName));
        body.put("viewUrl", minioService.getViewUrl(objectName));
        return ResponseEntity.ok(body);
    }

    /** GET /api/storage/download?object=... → chuyển sang /files/<key>?download=true */
    @GetMapping("/download")
    public ResponseEntity<Void> downloadFile(@RequestParam("object") String objectName) {
        String viewUrl = minioService.getViewUrl(minioService.toObjectKey(objectName));
        if (viewUrl == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", viewUrl + "?download=true")
                .build();
    }

    /** Xóa file – chỉ nhân viên đã đăng nhập trang quản trị. */
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @RequestParam("object") String objectName, HttpSession session) {
        Object role = session.getAttribute("userRole");
        if (!(role instanceof String s) || s.isBlank()) {
            return loi(HttpStatus.FORBIDDEN, "Bạn không có quyền xóa file.");
        }
        minioService.deleteFile(objectName);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("deleted", minioService.toObjectKey(objectName));
        return ResponseEntity.ok(body);
    }

    // ── Helper ────────────────────────────────────────────────────────
    private interface Uploader {
        String run();
    }

    private ResponseEntity<Map<String, Object>> upload(Uploader uploader, MultipartFile file) {
        try {
            String objectName = uploader.run();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("objectName", objectName);
            body.put("url", minioService.getViewUrl(objectName));
            body.put("presignedUrl", minioService.getPresignedUrl(objectName));
            body.put("fileName", file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
            body.put("size", file.getSize());
            body.put("isImage", minioService.isImage(objectName));
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return loi(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return loi(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> loi(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", message);
        return ResponseEntity.status(status).body(body);
    }
}
