package com.example.API;

import com.example.Service.MinioService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

/**
 * REST API cho quản lý file qua MinIO Storage
 * Base URL: /api/storage
 */
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private final MinioService minioService;

    public StorageController(MinioService minioService) {
        this.minioService = minioService;
    }

    // ── Upload ────────────────────────────────────────────────────────
    /**
     * POST /api/storage/upload
     * Param: file (MultipartFile), folder (String, optional)
     * VD folder: "ctv/avatar/", "khieu-nai/tai-lieu/"
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "ho-so/") String folder) {
        try {
            String objectName = minioService.uploadFile(file, folder);
            String url = minioService.getPresignedUrl(objectName);
            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "objectName", objectName,
                    "url",        url,
                    "fileName",   file.getOriginalFilename() != null ? file.getOriginalFilename() : "",
                    "size",       file.getSize()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error",   e.getMessage()
            ));
        }
    }

    // ── Upload avatar CTV ─────────────────────────────────────────────
    @PostMapping("/upload/ctv-avatar/{ctvId}")
    public ResponseEntity<Map<String, Object>> uploadCTVAvatar(
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer ctvId) {
        try {
            String objectName = minioService.uploadFile(file, MinioService.Folder.CTV_AVATAR,
                    "ctv-" + ctvId + getExt(file));
            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "objectName", objectName,
                    "url",        minioService.getPresignedUrl(objectName)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Upload chứng chỉ CTV ──────────────────────────────────────────
    @PostMapping("/upload/ctv-chung-chi/{ctvId}")
    public ResponseEntity<Map<String, Object>> uploadChungChi(
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer ctvId) {
        try {
            String objectName = minioService.uploadFile(file, MinioService.Folder.CTV_CHUNG_CHI);
            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "objectName", objectName,
                    "url",        minioService.getPresignedUrl(objectName)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Upload tài liệu khiếu nại ─────────────────────────────────────
    @PostMapping("/upload/khieu-nai/{khieuNaiId}")
    public ResponseEntity<Map<String, Object>> uploadKhieuNai(
            @RequestParam("file") MultipartFile file,
            @PathVariable Integer khieuNaiId) {
        try {
            String objectName = minioService.uploadFile(file, MinioService.Folder.KHIEU_NAI_TAILIEU);
            return ResponseEntity.ok(Map.of(
                    "success",    true,
                    "objectName", objectName,
                    "url",        minioService.getPresignedUrl(objectName)
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Lấy presigned URL ─────────────────────────────────────────────
    /**
     * GET /api/storage/url?object=ctv/avatar/abc.jpg
     */
    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getUrl(
            @RequestParam("object") String objectName) {
        try {
            if (!minioService.fileExists(objectName)) {
                return ResponseEntity.notFound().build();
            }
            String url = minioService.getPresignedUrl(objectName);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Download file (stream) ────────────────────────────────────────
    /**
     * GET /api/storage/download?object=ho-so/abc.pdf
     */
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @RequestParam("object") String objectName) {
        try {
            InputStream is = minioService.downloadFile(objectName);
            String filename = objectName.contains("/")
                    ? objectName.substring(objectName.lastIndexOf('/') + 1)
                    : objectName;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(is));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── Xóa file ─────────────────────────────────────────────────────
    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @RequestParam("object") String objectName) {
        try {
            minioService.deleteFile(objectName);
            return ResponseEntity.ok(Map.of("success", true, "deleted", objectName));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────
    private String getExt(MultipartFile file) {
        String name = file.getOriginalFilename();
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.'));
        }
        return "";
    }
}
