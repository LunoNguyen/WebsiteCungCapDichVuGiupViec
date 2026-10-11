package com.example.Service;

import com.example.Config.CacheNames;
import com.example.Model.CongTacVien;
import com.example.Model.DanhGia;
import com.example.Model.DonDatDichVu;
import com.example.Model.NhanVien;
import com.example.Model.PhanCongCTV;
import com.example.Model.PhanHoiDanhGia;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.DanhGiaRepository;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.NhanVienRepository;
import com.example.Repository.PhanCongCTVRepository;
import com.example.Repository.PhanHoiDanhGiaRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đánh giá đơn đã hoàn thành và trao đổi sau đánh giá.
 *
 * - Khách chấm 2 mục (chất lượng, thái độ) kèm nhận xét, mỗi đơn 1 đánh giá; sửa được trong {@link #SO_NGAY_SUA} ngày.
 * - Sau khi đánh giá, khách và CSKH bình luận qua lại trong bảng PhanHoiDanhGia:
 *   dòng có nhanVienId là của nhân viên bTaskee, dòng không có nhanVienId là của khách hàng.
 * - Trạng thái đánh giá theo CSDL: ChoDuyet (mới gửi) | HienThi (được ghim công khai) | An.
 */
@Service
@Transactional
public class DanhGiaService {

    public static final int SO_NGAY_SUA = 7;
    private static final int TOI_DA_NHAN_XET = 1000;
    private static final int TOI_DA_BINH_LUAN = 1000;

    private final DanhGiaRepository danhGiaRepository;
    private final PhanHoiDanhGiaRepository phanHoiRepository;
    private final DonDatDichVuRepository donDatRepository;
    private final PhanCongCTVRepository phanCongRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final NhanVienRepository nhanVienRepository;
    private final CacheManager cacheManager;

    public DanhGiaService(DanhGiaRepository danhGiaRepository, PhanHoiDanhGiaRepository phanHoiRepository,
                          DonDatDichVuRepository donDatRepository, PhanCongCTVRepository phanCongRepository,
                          CongTacVienRepository congTacVienRepository, NhanVienRepository nhanVienRepository,
                          CacheManager cacheManager) {
        this.danhGiaRepository = danhGiaRepository;
        this.phanHoiRepository = phanHoiRepository;
        this.donDatRepository = donDatRepository;
        this.phanCongRepository = phanCongRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.cacheManager = cacheManager;
    }

    // =========================================================================
    // KHÁCH HÀNG
    // =========================================================================

    public Map<String, Object> danhGia(Integer donDatId, Integer khachHangId,
                                       Integer diemChatLuong, Integer diemThaiDo, String nhanXet) {
        DonDatDichVu don = donCuaKhach(donDatId, khachHangId);
        if (!"HoanThanh".equalsIgnoreCase(don.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ đánh giá được đơn đã hoàn thành.");
        }
        if (danhGiaRepository.findByDonDat_Id(donDatId).isPresent()) {
            throw new IllegalArgumentException("Đơn này đã được đánh giá. Bạn có thể sửa đánh giá hoặc gửi bình luận.");
        }
        kiemTraDiem(diemChatLuong, diemThaiDo);

        CongTacVien ctv = ctvLamDon(donDatId);
        DanhGia dg = danhGiaRepository.save(DanhGia.builder()
                .maDanhGia(MaSinh.tao("DG-"))
                .donDat(don)
                .khachHang(don.getKhachHang())
                .congTacVien(ctv)
                .diemChatLuong(diemChatLuong)
                .diemThaiDo(diemThaiDo)
                .nhanXet(nhanXet(nhanXet))
                .ngayDanhGia(LocalDateTime.now())
                .trangThai("ChoDuyet")
                .build());
        capNhatDiemCtv(ctv);
        xoaCache();
        return toMap(dg);
    }

    public Map<String, Object> suaDanhGia(Integer donDatId, Integer khachHangId,
                                          Integer diemChatLuong, Integer diemThaiDo, String nhanXet) {
        donCuaKhach(donDatId, khachHangId);
        DanhGia dg = danhGiaRepository.findByDonDat_Id(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Đơn này chưa có đánh giá."));
        if (!conSuaDuoc(dg)) {
            throw new IllegalArgumentException("Chỉ sửa được đánh giá trong " + SO_NGAY_SUA
                    + " ngày sau khi gửi. Bạn vẫn có thể gửi bình luận bổ sung.");
        }
        kiemTraDiem(diemChatLuong, diemThaiDo);
        dg.setDiemChatLuong(diemChatLuong);
        dg.setDiemThaiDo(diemThaiDo);
        dg.setNhanXet(nhanXet(nhanXet));
        danhGiaRepository.save(dg);
        capNhatDiemCtv(dg.getCongTacVien());
        xoaCache();
        return toMap(dg);
    }

    /** Khách gửi thêm phản hồi / bình luận dưới đánh giá của đơn mình. */
    public Map<String, Object> binhLuanCuaKhach(Integer donDatId, Integer khachHangId, String noiDung) {
        donCuaKhach(donDatId, khachHangId);
        DanhGia dg = danhGiaRepository.findByDonDat_Id(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Hãy đánh giá đơn trước khi bình luận."));
        phanHoiRepository.save(PhanHoiDanhGia.builder()
                .danhGia(dg)
                .noiDung(noiDungBinhLuan(noiDung))
                .thoiGian(LocalDateTime.now())
                .build());
        return toMap(dg);
    }

    /** Đánh giá của một đơn (kèm trao đổi), null nếu chưa đánh giá. */
    @Transactional(readOnly = true)
    public Map<String, Object> cuaDon(Integer donDatId) {
        return danhGiaRepository.findByDonDat_Id(donDatId).map(this::toMap).orElse(null);
    }

    // =========================================================================
    // CSKH
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> chiTiet(Integer danhGiaId) {
        return toMap(timDanhGia(danhGiaId));
    }

    /** Nhân viên CSKH trả lời khách: lưu kèm nhân viên để khách thấy là phản hồi của bTaskee. */
    public Map<String, Object> traLoiCuaNhanVien(Integer danhGiaId, Integer taiKhoanId, String noiDung) {
        DanhGia dg = timDanhGia(danhGiaId);
        NhanVien nv = taiKhoanId == null ? null : nhanVienRepository.findByTaiKhoan_Id(taiKhoanId).orElse(null);
        if (nv == null) {
            throw new IllegalArgumentException("Không xác định được nhân viên đang đăng nhập.");
        }
        phanHoiRepository.save(PhanHoiDanhGia.builder()
                .danhGia(dg)
                .nhanVien(nv)
                .noiDung(noiDungBinhLuan(noiDung))
                .thoiGian(LocalDateTime.now())
                .build());
        return toMap(dg);
    }

    /** Id các đánh giá đã có nhân viên trả lời (cột "Phản hồi" của trang CSKH). */
    @Transactional(readOnly = true)
    public java.util.Set<Integer> daCoNhanVienTraLoi() {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (PhanHoiDanhGia ph : phanHoiRepository.findAll()) {
            if (ph.getNhanVien() != null) ids.add(ph.getDanhGia().getId());
        }
        return ids;
    }

    // =========================================================================
    // NỘI BỘ
    // =========================================================================

    public static boolean conSuaDuoc(DanhGia dg) {
        return dg.getNgayDanhGia() != null
                && !LocalDateTime.now().isAfter(dg.getNgayDanhGia().plusDays(SO_NGAY_SUA));
    }

    private DonDatDichVu donCuaKhach(Integer donDatId, Integer khachHangId) {
        DonDatDichVu don = donDatRepository.findById(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        if (khachHangId == null || don.getKhachHang() == null || !don.getKhachHang().getId().equals(khachHangId)) {
            throw new IllegalArgumentException("Không tìm thấy đơn.");
        }
        return don;
    }

    private DanhGia timDanhGia(Integer danhGiaId) {
        return danhGiaRepository.findById(danhGiaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá."));
    }

    /** Cộng tác viên đã làm đơn: ưu tiên phân công hoàn thành, bỏ qua người đã từ chối. */
    private CongTacVien ctvLamDon(Integer donDatId) {
        return phanCongRepository.findAllByDonDat_Id(donDatId).stream()
                .filter(pc -> !"TuChoi".equalsIgnoreCase(pc.getTrangThai()))
                .min(Comparator.comparing((PhanCongCTV pc) -> !"HoanThanh".equalsIgnoreCase(pc.getTrangThai()))
                        .thenComparing(PhanCongCTV::getId))
                .map(PhanCongCTV::getCongTacVien)
                .orElseThrow(() -> new IllegalArgumentException("Đơn này chưa có cộng tác viên thực hiện."));
    }

    private static void kiemTraDiem(Integer cl, Integer td) {
        if (cl == null || td == null || cl < 1 || cl > 5 || td < 1 || td > 5) {
            throw new IllegalArgumentException("Vui lòng chấm đủ hai mục chất lượng và thái độ (1 đến 5 sao).");
        }
    }

    private static String nhanXet(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        if (t.length() > TOI_DA_NHAN_XET) {
            throw new IllegalArgumentException("Nhận xét tối đa " + TOI_DA_NHAN_XET + " ký tự.");
        }
        return t;
    }

    private static String noiDungBinhLuan(String s) {
        if (s == null || s.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập nội dung bình luận.");
        }
        String t = s.trim();
        if (t.length() > TOI_DA_BINH_LUAN) {
            throw new IllegalArgumentException("Bình luận tối đa " + TOI_DA_BINH_LUAN + " ký tự.");
        }
        return t;
    }

    private void capNhatDiemCtv(CongTacVien ctv) {
        double tb = danhGiaRepository.findByCongTacVien_Id(ctv.getId()).stream()
                .mapToDouble(d -> (d.getDiemChatLuong() + d.getDiemThaiDo()) / 2.0)
                .average().orElse(5.0);
        ctv.setDiemDanhGia(BigDecimal.valueOf(tb).setScale(2, RoundingMode.HALF_UP));
        congTacVienRepository.save(ctv);
    }

    private void xoaCache() {
        for (String ten : List.of(CacheNames.DANH_GIA, CacheNames.DICH_VU)) {
            Cache c = cacheManager.getCache(ten);
            if (c != null) c.clear();
        }
    }

    private Map<String, Object> toMap(DanhGia dg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", dg.getId());
        m.put("maDanhGia", dg.getMaDanhGia());
        m.put("donDatId", dg.getDonDat() != null ? dg.getDonDat().getId() : null);
        m.put("maDonDat", dg.getDonDat() != null ? dg.getDonDat().getMaDonDat() : null);
        m.put("khachHang", dg.getKhachHang() != null ? dg.getKhachHang().getHoTen() : null);
        m.put("congTacVien", dg.getCongTacVien() != null ? dg.getCongTacVien().getHoTen() : null);
        m.put("diemChatLuong", dg.getDiemChatLuong());
        m.put("diemThaiDo", dg.getDiemThaiDo());
        m.put("nhanXet", dg.getNhanXet());
        m.put("ngayDanhGia", dg.getNgayDanhGia());
        m.put("trangThai", dg.getTrangThai());
        m.put("coTheSua", conSuaDuoc(dg));
        m.put("hanSua", dg.getNgayDanhGia() != null ? dg.getNgayDanhGia().plusDays(SO_NGAY_SUA) : null);

        List<Map<String, Object>> traoDoi = new ArrayList<>();
        phanHoiRepository.findByDanhGia_IdOrderByThoiGianAscIdAsc(dg.getId()).forEach(ph -> {
            Map<String, Object> p = new LinkedHashMap<>();
            boolean laNhanVien = ph.getNhanVien() != null;
            p.put("id", ph.getId());
            p.put("noiDung", ph.getNoiDung());
            p.put("thoiGian", ph.getThoiGian());
            p.put("laNhanVien", laNhanVien);
            p.put("nguoiGui", laNhanVien ? "bTaskee · " + ph.getNhanVien().getHoTen()
                    : (dg.getKhachHang() != null ? dg.getKhachHang().getHoTen() : "Khách hàng"));
            traoDoi.add(p);
        });
        m.put("traoDoi", traoDoi);
        return m;
    }
}
