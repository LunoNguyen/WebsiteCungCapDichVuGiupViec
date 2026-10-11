package com.example.Controller;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DiaChiKhachHang;
import com.example.Model.DonDatDichVu;
import com.example.Model.PhanCongCTV;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.PhanCongCTVRepository;
import com.example.Service.CatalogService;
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
 * Chi tiết một đơn đặt dịch vụ cho khung "Chi tiết đơn" của CSKH, kèm địa chỉ đầy đủ
 * để hiển thị vị trí thực hiện dịch vụ trên bản đồ.
 *
 * GET /cskh/don-dat-dich-vu/{id}/chi-tiet  (đã được AdminAuthenticationInterceptor phân quyền)
 */
@RestController
public class DonDatChiTietController {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    private final DonDatDichVuRepository donDatDichVuRepository;
    private final PhanCongCTVRepository phanCongCTVRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.PhanCongService phanCongService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.ViTriCtvStore viTriCtvStore;
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Service.GeocodingService geocodingService;

    public DonDatChiTietController(DonDatDichVuRepository donDatDichVuRepository,
                                   PhanCongCTVRepository phanCongCTVRepository) {
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.phanCongCTVRepository = phanCongCTVRepository;
    }

    @GetMapping("/cskh/don-dat-dich-vu/{id}/chi-tiet")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> chiTiet(@PathVariable Integer id) {
        DonDatDichVu don = donDatDichVuRepository.findByIdWithDetails(id).orElse(null);
        if (don == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", don.getId());
        body.put("maDonDat", don.getMaDonDat() != null ? don.getMaDonDat() : "#DD-" + don.getId());
        body.put("khachHang", don.getKhachHang() != null ? don.getKhachHang().getHoTen() : "");
        body.put("soDienThoai", don.getKhachHang() != null ? don.getKhachHang().getSoDienThoai() : "");

        List<String> dichVu = new ArrayList<>();
        if (don.getChiTietList() != null) {
            for (ChiTietDonDat ct : don.getChiTietList()) {
                if (ct.getDichVu() == null) {
                    continue;
                }
                int soLuong = ct.getSoLuong() != null ? ct.getSoLuong() : 1;
                dichVu.add(ct.getDichVu().getTenDichVu() + (soLuong > 1 ? " × " + soLuong : ""));
            }
        }
        body.put("dichVu", dichVu);

        DiaChiKhachHang diaChi = don.getDiaChi();
        body.put("diaChi", diaChiDayDu(diaChi));
        body.put("dienTichNha", diaChi != null && diaChi.getDienTichNha() != null
                ? diaChi.getDienTichNha().stripTrailingZeros().toPlainString() + " m²" : "");
        body.put("luuYDiaChi", diaChi != null && diaChi.getLuuYDacBiet() != null ? diaChi.getLuuYDacBiet() : "");

        body.put("ngayThucHien", don.getNgayThucHien() != null ? don.getNgayThucHien().format(NGAY) : "");
        String gio = don.getGioBatDau() != null ? don.getGioBatDau().format(GIO) : "";
        if (don.getGioKetThuc() != null) {
            gio += " - " + don.getGioKetThuc().format(GIO);
        }
        body.put("gio", gio);

        List<String> ctv = new ArrayList<>();
        for (PhanCongCTV pc : phanCongCTVRepository.findAllByDonDat_Id(id)) {
            if (pc.getCongTacVien() != null && !"TuChoi".equalsIgnoreCase(pc.getTrangThai())) {
                ctv.add(pc.getCongTacVien().getHoTen());
            }
        }
        body.put("congTacVien", ctv);

        body.put("thanhTien", CatalogService.dinhDangTien(don.getThanhTien()));
        body.put("trangThai", don.getTrangThai());
        body.put("yeuCauDacBiet", don.getYeuCauDacBiet() != null ? don.getYeuCauDacBiet() : "");
        body.put("ghiChu", don.getGhiChu() != null ? don.getGhiChu() : "");
        return ResponseEntity.ok(body);
    }

    // ── Theo dõi đơn đang thực hiện trên bản đồ ─────────────────────────

    /**
     * Vị trí địa chỉ khách chọn thực hiện dịch vụ (tra từ địa chỉ, cache Redis) và vị trí mới nhất
     * của các CTV phụ trách (app CTV gửi lên Redis mỗi 5 giây). Trang CSKH gọi lại mỗi 5 giây.
     *
     * GET /cskh/don-dat-dich-vu/{id}/theo-doi
     */
    @GetMapping("/cskh/don-dat-dich-vu/{id}/theo-doi")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> theoDoi(@PathVariable Integer id) {
        DonDatDichVu don = donDatDichVuRepository.findById(id).orElse(null);
        if (don == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trangThai", don.getTrangThai());

        String diaChi = diaChiDayDu(don.getDiaChi());
        Map<String, Object> khach = new LinkedHashMap<>();
        khach.put("diaChi", diaChi);
        geocodingService.timToaDo(diaChi).ifPresent(td -> {
            khach.put("viDo", td.viDo());
            khach.put("kinhDo", td.kinhDo());
        });
        body.put("khachHang", khach);

        List<Map<String, Object>> ctvs = new ArrayList<>();
        for (PhanCongCTV pc : phanCongCTVRepository.findAllByDonDat_Id(id)) {
            if (pc.getCongTacVien() == null || "TuChoi".equalsIgnoreCase(pc.getTrangThai())) {
                continue;
            }
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", pc.getCongTacVien().getId());
            c.put("hoTen", pc.getCongTacVien().getHoTen());
            c.put("soDienThoai", pc.getCongTacVien().getSoDienThoai());
            viTriCtvStore.get(pc.getCongTacVien().getId()).ifPresent(vt -> {
                c.put("viDo", vt.viDo());
                c.put("kinhDo", vt.kinhDo());
                c.put("capNhatLuc", vt.thoiGian().format(DateTimeFormatter.ofPattern("HH:mm:ss dd/MM")));
            });
            ctvs.add(c);
        }
        body.put("congTacVien", ctvs);
        return ResponseEntity.ok(body);
    }

    // ── Phân công cộng tác viên ─────────────────────────────────────────

    /** Gợi ý CTV cho đơn (đã xếp hạng) + danh sách không xếp được kèm lý do. Thuật toán: PhanCongService. */
    @GetMapping("/cskh/don-dat-dich-vu/{id}/goi-y-phan-cong")
    public ResponseEntity<Map<String, Object>> goiYPhanCong(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(phanCongService.goiY(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** CSKH phân công thủ công một CTV cho đơn. */
    @org.springframework.web.bind.annotation.PostMapping("/cskh/don-dat-dich-vu/{id}/phan-cong")
    public ResponseEntity<Map<String, Object>> phanCong(@PathVariable Integer id,
                                                        @org.springframework.web.bind.annotation.RequestParam Integer ctvId,
                                                        jakarta.servlet.http.HttpSession session) {
        try {
            phanCongService.phanCong(id, ctvId, nguoiDangNhap(session));
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã phân công cộng tác viên."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(Map.of("success", false,
                    "message", "Dữ liệu vừa được người khác thay đổi. Tải lại gợi ý rồi thử lại."));
        }
    }

    /** Bỏ một phân công chưa thực hiện. */
    @org.springframework.web.bind.annotation.PostMapping("/cskh/don-dat-dich-vu/{id}/bo-phan-cong")
    public ResponseEntity<Map<String, Object>> boPhanCong(@PathVariable Integer id,
                                                          @org.springframework.web.bind.annotation.RequestParam Integer phanCongId,
                                                          jakarta.servlet.http.HttpSession session) {
        try {
            phanCongService.boPhanCong(id, phanCongId, nguoiDangNhap(session));
            return ResponseEntity.ok(Map.of("success", true, "message", "Đã bỏ phân công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(Map.of("success", false,
                    "message", "Dữ liệu vừa được người khác thay đổi. Tải lại rồi thử lại."));
        }
    }

    private static String nguoiDangNhap(jakarta.servlet.http.HttpSession session) {
        Object ten = session.getAttribute("authenticatedFullName");
        return ten != null ? "CSKH: " + ten : "CSKH";
    }

    /** Ghép "số nhà, đường" với phường/xã và tỉnh/thành của khu vực (bỏ phần đã có sẵn trong địa chỉ). */
    private static String diaChiDayDu(DiaChiKhachHang diaChi) {
        if (diaChi == null || diaChi.getDiaChiChiTiet() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(diaChi.getDiaChiChiTiet().trim());
        if (diaChi.getKhuVuc() != null) {
            for (String phan : new String[]{diaChi.getKhuVuc().getQuanHuyen(), diaChi.getKhuVuc().getTinhThanh()}) {
                if (phan != null && !phan.isBlank() && !"Chưa xác định".equalsIgnoreCase(phan.trim())
                        && !sb.toString().toLowerCase().contains(phan.trim().toLowerCase())) {
                    sb.append(", ").append(phan.trim());
                }
            }
        }
        return sb.toString();
    }
}
