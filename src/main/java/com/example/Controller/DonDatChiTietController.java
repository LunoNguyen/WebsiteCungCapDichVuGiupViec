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
