package com.example.Service;

import com.example.DTO.request.AddressRequest;
import com.example.Model.DiaChiKhachHang;
import com.example.Model.KhachHang;
import com.example.Model.KhuVuc;
import com.example.Repository.DiaChiKhachHangRepository;
import com.example.Repository.KhachHangRepository;
import com.example.Repository.KhuVucRepository;
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
    private final KhuVucRepository khuVucRepository;

    public CustomerAddressService(DiaChiKhachHangRepository diaChiRepository,
                                  KhachHangRepository khachHangRepository,
                                  KhuVucRepository khuVucRepository) {
        this.diaChiRepository = diaChiRepository;
        this.khachHangRepository = khachHangRepository;
        this.khuVucRepository = khuVucRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSach(Integer khachHangId) {
        timKhachHang(khachHangId);
        return dangDung(khachHangId).stream()
                .sorted(Comparator.comparing((DiaChiKhachHang d) -> !Boolean.TRUE.equals(d.getLaMacDinh()))
                        .thenComparing(DiaChiKhachHang::getId))
                .map(CustomerAddressService::toMap)
                .toList();
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
        return toMap(dc);
    }

    public Map<String, Object> sua(Integer khachHangId, Integer diaChiId, AddressRequest req) {
        DiaChiKhachHang dc = timDiaChi(khachHangId, diaChiId);
        ganGiaTri(dc, req);
        diaChiRepository.save(dc);
        if (Boolean.TRUE.equals(req.getLaMacDinh())) {
            datMacDinh(khachHangId, dc);
        }
        return toMap(dc);
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
        KhuVuc kv = null;
        if (req.getKhuVucId() != null) {
            kv = khuVucRepository.findById(req.getKhuVucId())
                    .orElseThrow(() -> new IllegalArgumentException("Khu vực không hợp lệ."));
        }
        dc.setDiaChiChiTiet(req.getDiaChiChiTiet().trim());
        dc.setKhuVuc(kv);
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
