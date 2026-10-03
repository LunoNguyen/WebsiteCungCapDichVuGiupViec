package com.example.Service;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Dịch vụ lưu trữ file trên MinIO cho toàn hệ thống Neatify.
 *
 * Quy ước:
 *   - Cơ sở dữ liệu chỉ lưu "object key" (đường dẫn trong bucket, vd: ctv/ho-so/abc.jpg),
 *     KHÔNG lưu URL đầy đủ, vì presigned URL hết hạn sau một thời gian.
 *   - Khi hiển thị, dùng {@link #getViewUrl(String)} để đổi key thành đường dẫn /files/<key>;
 *     FileViewController sẽ lấy file từ MinIO xuống và trả về trình duyệt.
 *   - Thư mục "cong-khai/" chứa ảnh ai cũng xem được (ảnh danh mục dịch vụ...).
 *     Các thư mục còn lại (hồ sơ CTV, khiếu nại...) chỉ nhân viên đã đăng nhập mới xem được.
 */
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    /** Đường dẫn công khai để xem file qua ứng dụng. */
    public static final String VIEW_PREFIX = "/files/";

    /** Phần mở rộng được phép tải lên và content-type tương ứng. */
    private static final Map<String, String> LOAI_FILE_CHO_PHEP = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("gif", "image/gif"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("mov", "video/quicktime"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    );

    private static final Set<String> DUOI_ANH = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.max-file-size-mb:20}")
    private long maxFileSizeMb;

    @Value("${minio.presigned-expiry-minutes:60}")
    private int presignedExpiryMinutes;

    private volatile boolean bucketReady = false;

    public MinioService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    // ── Bucket ─────────────────────────────────────────────────────────────
    /** Kiểm tra kết nối và tạo bucket khi ứng dụng khởi động xong (không làm sập app nếu MinIO chưa bật). */
    @EventListener(ApplicationReadyEvent.class)
    public void kiemTraKhiKhoiDong() {
        try {
            ensureBucket();
            log.info("MinIO sẵn sàng: {} (bucket '{}')", endpoint, bucketName);
        } catch (Exception e) {
            log.warn("MinIO chưa kết nối được tại {} – chức năng tải file tạm thời chưa hoạt động. "
                    + "Kiểm tra MinIO đã chạy chưa và minio.endpoint có trỏ đúng cổng API (9000) không. Chi tiết: {}",
                    endpoint, e.getMessage());
        }
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketReady) {
            return;
        }
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            log.info("MinIO: đã tạo bucket '{}'", bucketName);
        }
        bucketReady = true;
    }

    // ── Upload ─────────────────────────────────────────────────────────────
    /**
     * Upload file lên MinIO, tự sinh tên bằng UUID để không trùng.
     *
     * @param file       file từ form/ứng dụng di động
     * @param folderPath thư mục lưu (vd: "ctv/ho-so/")
     * @return object key đã lưu trong bucket – đây là giá trị cần lưu vào CSDL
     */
    public String uploadFile(MultipartFile file, String folderPath) {
        String ext = kiemTraFile(file, false);
        return put(file, chuanHoaThuMuc(folderPath) + UUID.randomUUID() + "." + ext, ext);
    }

    /** Upload với tên file cố định (ghi đè file cũ cùng tên), vd ảnh đại diện "ctv-12.jpg". */
    public String uploadFile(MultipartFile file, String folderPath, String customFileName) {
        String ext = kiemTraFile(file, false);
        String ten = customFileName == null ? "" : customFileName.replaceAll("\\.[A-Za-z0-9]+$", "");
        ten = ten.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        if (ten.isBlank()) {
            ten = UUID.randomUUID().toString();
        }
        return put(file, chuanHoaThuMuc(folderPath) + ten + "." + ext, ext);
    }

    /** Upload chỉ chấp nhận file ảnh (jpg, png, webp, gif). */
    public String uploadImage(MultipartFile file, String folderPath) {
        String ext = kiemTraFile(file, true);
        return put(file, chuanHoaThuMuc(folderPath) + UUID.randomUUID() + "." + ext, ext);
    }

    private String put(MultipartFile file, String objectName, String ext) {
        try (InputStream is = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(is, file.getSize(), -1)
                            // Content-type lấy theo phần mở rộng đã kiểm tra, không tin giá trị client gửi lên
                            .contentType(LOAI_FILE_CHO_PHEP.get(ext))
                            .build());
            log.info("MinIO upload OK: {}/{} ({} bytes)", bucketName, objectName, file.getSize());
            return objectName;
        } catch (Exception e) {
            log.error("MinIO upload thất bại ({}): {}", objectName, e.getMessage());
            throw new IllegalStateException("Không tải được file lên máy chủ lưu trữ (MinIO). "
                    + "Kiểm tra MinIO đã chạy và cấu hình minio.endpoint. Chi tiết: " + e.getMessage(), e);
        }
    }

    /** Kiểm tra file hợp lệ, trả về phần mở rộng (chữ thường). */
    private String kiemTraFile(MultipartFile file, boolean chiNhanAnh) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn file hoặc file rỗng.");
        }
        if (file.getSize() > maxFileSizeMb * 1024 * 1024) {
            throw new IllegalArgumentException("File vượt quá dung lượng cho phép (" + maxFileSizeMb + " MB).");
        }
        String ext = layDuoiFile(file.getOriginalFilename());
        if (!LOAI_FILE_CHO_PHEP.containsKey(ext)) {
            throw new IllegalArgumentException("Định dạng file không được hỗ trợ. Chỉ nhận: jpg, png, webp, gif, pdf, mp4, mov, doc, docx.");
        }
        if (chiNhanAnh && !DUOI_ANH.contains(ext)) {
            throw new IllegalArgumentException("Vui lòng chọn file ảnh (jpg, png, webp hoặc gif).");
        }
        return ext;
    }

    private static String layDuoiFile(String tenFile) {
        if (tenFile == null || !tenFile.contains(".")) {
            return "";
        }
        return tenFile.substring(tenFile.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /** Chỉ cho phép thư mục dạng chữ thường/số/gạch ngang, không cho ".." để tránh ghi ra ngoài. */
    private static String chuanHoaThuMuc(String folderPath) {
        String thuMuc = folderPath == null ? "" : folderPath.trim().toLowerCase(Locale.ROOT);
        thuMuc = thuMuc.replace('\\', '/').replaceAll("^/+", "").replaceAll("/+$", "");
        if (thuMuc.isEmpty()) {
            thuMuc = "khac";
        }
        if (!thuMuc.matches("[a-z0-9_-]+(/[a-z0-9_-]+)*")) {
            throw new IllegalArgumentException("Thư mục lưu file không hợp lệ: " + folderPath);
        }
        return thuMuc + "/";
    }

    // ── Đọc file ───────────────────────────────────────────────────────────
    /** File lấy từ MinIO kèm thông tin để trả về trình duyệt. Nhớ đóng stream sau khi dùng. */
    public record StoredFile(InputStream stream, String contentType, long size, String etag) {
    }

    /**
     * Mở file trong MinIO.
     *
     * @return null nếu file không tồn tại hoặc MinIO không kết nối được
     */
    public StoredFile openFile(String objectName) {
        String key = toObjectKey(objectName);
        if (key == null) {
            return null;
        }
        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucketName).object(key).build());
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(key).build());
            String contentType = stat.contentType();
            String theoDuoi = LOAI_FILE_CHO_PHEP.get(layDuoiFile(key));
            if (theoDuoi != null) {
                contentType = theoDuoi;
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }
            return new StoredFile(stream, contentType, stat.size(), stat.etag());
        } catch (Exception e) {
            log.warn("MinIO không đọc được file '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /** Lấy InputStream của file (giữ lại cho các chỗ đang dùng). */
    public InputStream downloadFile(String objectName) {
        StoredFile file = openFile(objectName);
        if (file == null) {
            throw new IllegalArgumentException("Không tìm thấy file: " + objectName);
        }
        return file.stream();
    }

    public boolean fileExists(String objectName) {
        String key = toObjectKey(objectName);
        if (key == null) {
            return false;
        }
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucketName).object(key).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── URL ────────────────────────────────────────────────────────────────
    /**
     * Đổi giá trị lưu trong CSDL thành đường dẫn hiển thị được trên trình duyệt.
     *   - object key MinIO  → /files/<key>   (ứng dụng lấy file từ MinIO xuống)
     *   - đường dẫn tĩnh "/images/..." hoặc URL ngoài → giữ nguyên
     */
    public String getViewUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        if (v.startsWith(VIEW_PREFIX)) {
            return v;
        }
        if (v.startsWith("/")) {
            return v; // tài nguyên tĩnh của ứng dụng
        }
        if (laUrl(v) && !laUrlMinio(v)) {
            return v; // ảnh từ nguồn bên ngoài
        }
        String key = toObjectKey(v);
        return key == null ? null : VIEW_PREFIX + key;
    }

    /**
     * Chuẩn hóa giá trị client gửi lên (key, /files/<key>, URL MinIO hoặc presigned URL) về object key.
     * Dùng trước khi lưu vào CSDL.
     */
    public String toObjectKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();
        int hoi = v.indexOf('?');
        if (hoi >= 0) {
            v = v.substring(0, hoi); // bỏ chữ ký của presigned URL
        }
        if (laUrl(v)) {
            int viTri = v.indexOf("/" + bucketName + "/");
            if (viTri >= 0) {
                v = v.substring(viTri + bucketName.length() + 2);
            } else {
                int files = v.indexOf(VIEW_PREFIX);
                if (files < 0) {
                    return null; // URL ngoài, không phải file trong MinIO
                }
                v = v.substring(files + VIEW_PREFIX.length());
            }
        } else if (v.startsWith(VIEW_PREFIX)) {
            v = v.substring(VIEW_PREFIX.length());
        }
        v = v.replaceAll("^/+", "");
        if (v.isEmpty() || v.contains("..")) {
            return null;
        }
        return v;
    }

    /** Giá trị để lưu CSDL: object key nếu là file MinIO, ngược lại giữ nguyên. */
    public String toStoredValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String key = toObjectKey(value);
        return key != null ? key : value.trim();
    }

    private static boolean laUrl(String v) {
        return v.startsWith("http://") || v.startsWith("https://");
    }

    private boolean laUrlMinio(String v) {
        return v.startsWith(endpoint) || v.contains("/" + bucketName + "/");
    }

    /** File trong thư mục công khai – không cần đăng nhập để xem. */
    public boolean isPublic(String objectName) {
        String key = toObjectKey(objectName);
        return key != null && key.startsWith(Folder.CONG_KHAI);
    }

    /** Giá trị cho cột TaiLieuKhieuNai.LoaiFile: HinhAnh | Video | TaiLieuKhac. */
    public String phanLoaiFile(String objectName) {
        String ext = layDuoiFile(toStoredValue(objectName));
        if (DUOI_ANH.contains(ext)) {
            return "HinhAnh";
        }
        if (ext.equals("mp4") || ext.equals("mov")) {
            return "Video";
        }
        return "TaiLieuKhac";
    }

    public boolean isImage(String objectName) {
        return objectName != null && DUOI_ANH.contains(layDuoiFile(objectName));
    }

    /**
     * Tạo presigned URL có thời hạn để ứng dụng di động tải file trực tiếp từ MinIO.
     *
     * @return null nếu không tạo được
     */
    public String getPresignedUrl(String objectName) {
        String key = toObjectKey(objectName);
        if (key == null) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(key)
                            .expiry(presignedExpiryMinutes, TimeUnit.MINUTES)
                            .build());
        } catch (Exception e) {
            log.warn("MinIO không tạo được presigned URL ({}): {}", key, e.getMessage());
            return null;
        }
    }

    // ── Xóa ────────────────────────────────────────────────────────────────
    public void deleteFile(String objectName) {
        String key = toObjectKey(objectName);
        if (key == null) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(key).build());
            log.info("MinIO xóa OK: {}/{}", bucketName, key);
        } catch (Exception e) {
            log.warn("MinIO xóa thất bại ({}): {}", key, e.getMessage());
        }
    }

    // ── Thư mục theo loại file ─────────────────────────────────────────────
    public static final class Folder {
        /** Mọi thứ dưới thư mục này đều xem được không cần đăng nhập. */
        public static final String CONG_KHAI         = "cong-khai/";
        public static final String LOAI_DICH_VU      = "cong-khai/loai-dich-vu/";

        public static final String CTV_HO_SO         = "ctv/ho-so/";
        public static final String CTV_CHUNG_CHI     = "ctv/chung-chi/";
        public static final String CTV_AVATAR        = "ctv/avatar/";
        public static final String KH_AVATAR         = "khach-hang/avatar/";
        public static final String DON_DAT_TAILIEU   = "don-dat/tai-lieu/";
        public static final String TAI_SAN           = "don-dat/tai-san/";
        public static final String KHIEU_NAI_TAILIEU = "khieu-nai/tai-lieu/";
        public static final String HO_SO             = "ho-so/";
        public static final String NHAN_VIEN_AVATAR  = "nhan-vien/avatar/";

        private Folder() {
        }
    }
}
