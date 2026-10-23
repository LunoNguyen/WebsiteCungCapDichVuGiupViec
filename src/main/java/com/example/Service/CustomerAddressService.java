package com.example.Service;

import com.example.DTO.request.AddressRequest;
import com.example.Model.DiaChiKhachHang;
import com.example.Model.KhachHang;
import com.example.Model.KhuVuc;
import com.example.Repository.DiaChiKhachHangRepository;
import com.example.Repository.KhachHangRepository;
import com.example.Repository.KhuVucCTVRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Sổ địa chỉ của khách hàng: một khách hàng có nhiều địa chỉ, đúng một địa chỉ mặc định. */
@Service
@Transactional
public class CustomerAddressService {

    private static final String HOAT_DONG = "HoatDong";

    private final DiaChiKhachHangRepository diaChiRepository;
    private final KhachHangRepository khachHangRepository;
    private final KhuVucService khuVucService;
    private final KhuVucCTVRepository khuVucCTVRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private DiaGioiService diaGioiService;

    public CustomerAddressService(DiaChiKhachHangRepository diaChiRepository,
                                  KhachHangRepository khachHangRepository,
                                  KhuVucService khuVucService,
                                  KhuVucCTVRepository khuVucCTVRepository) {
        this.diaChiRepository = diaChiRepository;
        this.khachHangRepository = khachHangRepository;
        this.khuVucService = khuVucService;
        this.khuVucCTVRepository = khuVucCTVRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSach(Integer khachHangId) {
        timKhachHang(khachHangId);
        return dangDung(khachHangId).stream()
                .sorted(Comparator.comparing((DiaChiKhachHang d) -> !Boolean.TRUE.equals(d.getLaMacDinh()))
                        .thenComparing(DiaChiKhachHang::getId))
                .map(this::toMapKemCtv)
                .toList();
    }

    /** Số cộng tác viên đang nhận việc tại quận, huyện của địa chỉ: cộng tác viên cùng quận được ưu tiên khi xếp lịch. */
    @Transactional(readOnly = true)
    public long soCongTacVienTaiKhuVuc(Integer khuVucId) {
        if (khuVucId == null) return 0;
        return khuVucCTVRepository.countByKhuVuc_IdAndTrangThaiAndCongTacVien_TrangThai(khuVucId, HOAT_DONG, HOAT_DONG);
    }

    private Map<String, Object> toMapKemCtv(DiaChiKhachHang d) {
        Map<String, Object> m = toMap(d);
        m.put("soCongTacVien", soCongTacVienTaiKhuVuc(d.getKhuVuc() != null ? d.getKhuVuc().getId() : null));
        // Tách lại số nhà / phường / tỉnh để form sửa điền sẵn
        if (diaGioiService != null) m.putAll(diaGioiService.tach(d.getDiaChiChiTiet()));
        return m;
    }

    public Map<String, Object> them(Integer khachHangId, AddressRequest req) {
        KhachHang kh = timKhachHang(khachHangId);
        boolean laDauTien = dangDung(khachHangId).isEmpty();

        DiaChiKhachHang dc = DiaChiKhachHang.builder()
                .maDiaChi(MaSinh.tao("DC-"))
                .khachHang(kh)
                .laMacDinh(false)
                .trangThai(HOAT_DONG)
                .build();
        ganGiaTri(dc, req);
        dc = diaChiRepository.save(dc);

        // Địa chỉ đầu tiên luôn là mặc định
        if (laDauTien || Boolean.TRUE.equals(req.getLaMacDinh())) {
            datMacDinh(khachHangId, dc);
        }
        return toMapKemCtv(dc);
    }

    public Map<String, Object> sua(Integer khachHangId, Integer diaChiId, AddressRequest req) {
        DiaChiKhachHang dc = timDiaChi(khachHangId, diaChiId);
        ganGiaTri(dc, req);
        diaChiRepository.save(dc);
        if (Boolean.TRUE.equals(req.getLaMacDinh())) {
            datMacDinh(khachHangId, dc);
        }
        return toMapKemCtv(dc);
    }

    public Map<String, Object> chonMacDinh(Integer khachHangId, Integer diaChiId) {
        DiaChiKhachHang dc = timDiaChi(khachHangId, diaChiId);
        datMacDinh(khachHangId, dc);
        return toMap(dc);
    }

    /** Xoá mềm (đơn cũ vẫn tham chiếu địa chỉ). Xoá địa chỉ mặc định thì chuyển mặc định sang địa chỉ còn lại. */
    public void xoa(Integer khachHangId, Integer diaChiId) {
        DiaChiKhachHang dc = timDiaChi(khachHangId, diaChiId);
        boolean laMacDinh = Boolean.TRUE.equals(dc.getLaMacDinh());
        dc.setTrangThai("DaXoa");
        dc.setLaMacDinh(false);
        diaChiRepository.save(dc);
        if (laMacDinh) {
            dangDung(khachHangId).stream()
                    .min(Comparator.comparing(DiaChiKhachHang::getId))
                    .ifPresent(con -> datMacDinh(khachHangId, con));
        }
    }

    /** Địa chỉ còn dùng của đúng khách hàng này; dùng cả khi đặt đơn để không lấy nhầm địa chỉ người khác. */
    @Transactional(readOnly = true)
    public DiaChiKhachHang timDiaChi(Integer khachHangId, Integer diaChiId) {
        DiaChiKhachHang dc = diaChiRepository.findById(diaChiId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy địa chỉ."));
        if (dc.getKhachHang() == null || !dc.getKhachHang().getId().equals(khachHangId)
                || !HOAT_DONG.equals(dc.getTrangThai())) {
            throw new IllegalArgumentException("Không tìm thấy địa chỉ.");
        }
        return dc;
    }

    private List<DiaChiKhachHang> dangDung(Integer khachHangId) {
        return diaChiRepository.findByKhachHang_IdAndTrangThai(khachHangId, HOAT_DONG);
    }

    private void datMacDinh(Integer khachHangId, DiaChiKhachHang macDinh) {
        for (DiaChiKhachHang d : dangDung(khachHangId)) {
            boolean la = d.getId().equals(macDinh.getId());
            if (!Boolean.valueOf(la).equals(d.getLaMacDinh())) {
                d.setLaMacDinh(la);
                diaChiRepository.save(d);
            }
        }
        macDinh.setLaMacDinh(true);
    }

    private void ganGiaTri(DiaChiKhachHang dc, AddressRequest req) {
        if (req.getMaQuan() != null && req.getMaPhuong() != null) {
            // Địa chỉ theo kiểu truyền thống V1 (3 cấp: Tỉnh -> Quận -> Phường)
            String soNha = req.getSoNhaTenDuong() != null ? req.getSoNhaTenDuong() : req.getDiaChiChiTiet();
            DiaGioiService.DiaChiDaXacDinh x = diaGioiService.xacDinhV1(soNha, req.getMaTinh(), req.getMaQuan(), req.getMaPhuong());
            dc.setDiaChiChiTiet(x.diaChiDayDu());
            dc.setKhuVuc(x.khuVuc());
        } else if (req.getMaTinh() != null || req.getMaPhuong() != null) {
            // Địa chỉ theo tỉnh, phường (API địa giới V2); khu vực = quận, huyện chứa phường đó
            String soNha = req.getSoNhaTenDuong() != null ? req.getSoNhaTenDuong() : req.getDiaChiChiTiet();
            DiaGioiService.DiaChiDaXacDinh x = diaGioiService.xacDinh(soNha, req.getMaTinh(), req.getMaPhuong(), req.getKhuVucId());
            dc.setDiaChiChiTiet(x.diaChiDayDu());
            dc.setKhuVuc(x.khuVuc()); // null = ngoài khu vực phục vụ: vẫn lưu được, chưa đặt dịch vụ được
        } else {
            // App cũ: địa chỉ dạng chữ + khu vực chọn tay
            if (req.getDiaChiChiTiet() == null || req.getDiaChiChiTiet().isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập địa chỉ.");
            }
            if (req.getKhuVucId() == null) {
                throw new IllegalArgumentException("Vui lòng chọn tỉnh, thành và phường, xã của địa chỉ.");
            }
            dc.setDiaChiChiTiet(req.getDiaChiChiTiet().trim());
            dc.setKhuVuc(khuVucService.layDangPhucVu(req.getKhuVucId()));
        }
        dc.setDienTichNha(req.getDienTichNha());
        dc.setLuuYDacBiet(blankToNull(req.getLuuYDacBiet()));
    }

    private KhachHang timKhachHang(Integer khachHangId) {
        if (khachHangId == null) throw new IllegalArgumentException("Thiếu mã khách hàng.");
        return khachHangRepository.findById(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng."));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** "Số nhà, đường" kèm quận, huyện và tỉnh/thành (bỏ phần đã có sẵn trong địa chỉ). */
    public static String diaChiDayDu(DiaChiKhachHang diaChi) {
        if (diaChi == null || diaChi.getDiaChiChiTiet() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(diaChi.getDiaChiChiTiet().trim());
        if (diaChi.getKhuVuc() != null) {
            for (String phan : new String[]{diaChi.getKhuVuc().getTenKhuVuc(), diaChi.getKhuVuc().getTinhThanh()}) {
                if (phan != null && !phan.isBlank() && !"Chưa xác định".equalsIgnoreCase(phan.trim())
                        && !sb.toString().toLowerCase().contains(phan.trim().toLowerCase())) {
                    sb.append(", ").append(phan.trim());
                }
            }
        }
        return sb.toString();
    }

    public static Map<String, Object> toMap(DiaChiKhachHang d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("maDiaChi", d.getMaDiaChi());
        m.put("diaChiChiTiet", d.getDiaChiChiTiet());
        KhuVuc kv = d.getKhuVuc();
        m.put("khuVucId", kv != null ? kv.getId() : null);
        m.put("khuVuc", kv != null ? kv.getTenKhuVuc() + ", " + kv.getTinhThanh() : null);
        m.put("dienTichNha", d.getDienTichNha());
        m.put("luuYDacBiet", d.getLuuYDacBiet());
        m.put("laMacDinh", Boolean.TRUE.equals(d.getLaMacDinh()));
        return m;
    }
}
