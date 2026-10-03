package com.example.Controller;

import com.example.Model.ChungChiCTV;
import com.example.Model.CongTacVien;
import com.example.Model.HoSoCTV;
import com.example.Model.KhieuNai;
import com.example.Model.TaiLieuKhieuNai;
import com.example.Repository.ChungChiCTVRepository;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.HoSoCTVRepository;
import com.example.Repository.KhieuNaiRepository;
import com.example.Repository.TaiLieuKhieuNaiRepository;
import com.example.Service.MinioService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trả về danh sách file đính kèm (lưu trên MinIO) để trang quản trị hiển thị:
 *   - Hồ sơ + chứng chỉ của cộng tác viên (HCNS)
 *   - Tài liệu bằng chứng khiếu nại (CSKH, Giám đốc)
 *
 * Các đường dẫn nằm dưới /hcns, /cskh, /giam-doc nên đã được AdminAuthenticationInterceptor
 * kiểm tra đăng nhập và phân quyền. Giao diện dùng /js/file-viewer.js để gọi và hiển thị.
 */
@RestController
public class TaiLieuController {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CongTacVienRepository congTacVienRepository;
    private final HoSoCTVRepository hoSoCTVRepository;
    private final ChungChiCTVRepository chungChiCTVRepository;
    private final KhieuNaiRepository khieuNaiRepository;
    private final TaiLieuKhieuNaiRepository taiLieuKhieuNaiRepository;
    private final MinioService minioService;

    public TaiLieuController(CongTacVienRepository congTacVienRepository,
                             HoSoCTVRepository hoSoCTVRepository,
                             ChungChiCTVRepository chungChiCTVRepository,
                             KhieuNaiRepository khieuNaiRepository,
                             TaiLieuKhieuNaiRepository taiLieuKhieuNaiRepository,
                             MinioService minioService) {
        this.congTacVienRepository = congTacVienRepository;
        this.hoSoCTVRepository = hoSoCTVRepository;
        this.chungChiCTVRepository = chungChiCTVRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.taiLieuKhieuNaiRepository = taiLieuKhieuNaiRepository;
        this.minioService = minioService;
    }

    @GetMapping("/hcns/cong-tac-vien/{id}/tai-lieu")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> taiLieuCongTacVien(@PathVariable Integer id) {
        CongTacVien ctv = congTacVienRepository.findById(id).orElse(null);
        if (ctv == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> files = new ArrayList<>();
        for (HoSoCTV hs : hoSoCTVRepository.findByCongTacVien_Id(id)) {
            files.add(file("Hồ sơ", tenLoaiHoSo(hs.getLoaiTaiLieu()),
                    hs.getNgayTai() != null ? "Tải lên " + hs.getNgayTai().format(NGAY_GIO) : "",
                    hs.getDuongDanFile()));
        }
        for (ChungChiCTV cc : chungChiCTVRepository.findByCongTacVien_Id(id)) {
            List<String> moTa = new ArrayList<>();
            if (cc.getNoiCap() != null && !cc.getNoiCap().isBlank()) {
                moTa.add(cc.getNoiCap().trim());
            }
            if (cc.getNgayCap() != null) {
                moTa.add("cấp ngày " + cc.getNgayCap().format(NGAY));
            }
            files.add(file("Chứng chỉ, bằng cấp", cc.getTenChungChi(), String.join(", ", moTa), cc.getDuongDanFile()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tieuDe", "Hồ sơ của " + ctv.getHoTen());
        body.put("taiLieu", files);
        return ResponseEntity.ok(body);
    }

    @GetMapping({"/cskh/khieu-nai/{id}/tai-lieu", "/giam-doc/khieu-nai/{id}/tai-lieu"})
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> taiLieuKhieuNai(@PathVariable Integer id) {
        KhieuNai khieuNai = khieuNaiRepository.findById(id).orElse(null);
        if (khieuNai == null) {
            return ResponseEntity.notFound().build();
        }

        List<Map<String, Object>> files = new ArrayList<>();
        int stt = 1;
        for (TaiLieuKhieuNai tl : taiLieuKhieuNaiRepository.findByKhieuNai_Id(id)) {
            files.add(file("Bằng chứng đính kèm", tenLoaiFile(tl.getLoaiFile()) + " " + stt++,
                    tl.getNgayTai() != null ? "Tải lên " + tl.getNgayTai().format(NGAY_GIO) : "",
                    tl.getDuongDanFile()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tieuDe", "Tài liệu của khiếu nại " + khieuNai.getMaKhieuNai());
        body.put("taiLieu", files);
        return ResponseEntity.ok(body);
    }

    private Map<String, Object> file(String nhom, String ten, String moTa, String duongDan) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("nhom", nhom);
        item.put("ten", ten != null ? ten : "Tài liệu");
        item.put("moTa", moTa != null ? moTa : "");
        item.put("url", minioService.getViewUrl(duongDan));
        item.put("laAnh", minioService.isImage(minioService.toStoredValue(duongDan)));
        return item;
    }

    private static String tenLoaiHoSo(String loai) {
        if (loai == null) {
            return "Tài liệu khác";
        }
        return switch (loai) {
            case "AnhChanDung" -> "Ảnh chân dung";
            case "CCCD_Mat_Truoc" -> "CCCD mặt trước";
            case "CCCD_Mat_Sau" -> "CCCD mặt sau";
            default -> "Tài liệu khác";
        };
    }

    private static String tenLoaiFile(String loai) {
        if ("HinhAnh".equals(loai)) {
            return "Hình ảnh";
        }
        if ("Video".equals(loai)) {
            return "Video";
        }
        return "Tài liệu";
    }
}
