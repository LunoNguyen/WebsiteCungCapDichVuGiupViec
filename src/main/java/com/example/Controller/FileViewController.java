package com.example.Controller;

import com.example.Service.MinioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Hiển thị file lưu trên MinIO: GET /files/<object key>
 *
 * Ứng dụng lấy file từ MinIO xuống rồi trả về trình duyệt, nên bucket không cần mở công khai
 * và thẻ <img src="/files/..."> luôn dùng được (không hết hạn như presigned URL).
 *
 *   - cong-khai/...  : ai cũng xem được
 *   - còn lại        : chỉ nhân viên đã đăng nhập trang quản trị
 */
@Controller
public class FileViewController {

    private final MinioService minioService;

    public FileViewController(MinioService minioService) {
        this.minioService = minioService;
    }

    @GetMapping("/files/**")
    public ResponseEntity<InputStreamResource> xemFile(
            HttpServletRequest request,
            @RequestParam(value = "download", defaultValue = "false") boolean download) {

        String prefix = request.getContextPath() + MinioService.VIEW_PREFIX;
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith(prefix)) {
            return ResponseEntity.notFound().build();
        }
        String key = minioService.toObjectKey(URLDecoder.decode(uri.substring(prefix.length()), StandardCharsets.UTF_8));
        if (key == null) {
            return ResponseEntity.notFound().build();
        }

        boolean congKhai = minioService.isPublic(key);
        if (!congKhai && !daDangNhapQuanTri(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        MinioService.StoredFile file = minioService.openFile(key);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        String tenFile = key.substring(key.lastIndexOf('/') + 1);
        String contentType = file.contentType();
        // Chỉ ảnh, video, PDF được mở trực tiếp; loại khác luôn tải về
        boolean moTrucTiep = contentType.startsWith("image/") || contentType.startsWith("video/")
                || contentType.equals(MediaType.APPLICATION_PDF_VALUE);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition((download || !moTrucTiep
                ? ContentDisposition.attachment()
                : ContentDisposition.inline()).filename(tenFile, StandardCharsets.UTF_8).build());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setCacheControl(congKhai
                ? CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic()
                : CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate());
        if (file.size() >= 0) {
            headers.setContentLength(file.size());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(contentType))
                .body(new InputStreamResource(file.stream()));
    }

    private boolean daDangNhapQuanTri(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object role = session != null ? session.getAttribute("userRole") : null;
        return role instanceof String s && !s.isBlank();
    }
}
