package com.example.Service;

import com.example.Model.CongTacVien;
import com.example.Model.KhuVuc;
import com.example.Model.KhuVucCTV;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.KhuVucCTVRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Khu vực nhận việc của cộng tác viên: các quận, huyện (cùng danh sách với địa chỉ khách hàng).
 * CTV cùng quận với địa chỉ đơn được ưu tiên khi CSKH xếp lịch.
 * Bỏ chọn một quận thì dòng KhuVucCTV chuyển TamNgung (không xoá, giữ lịch sử); chọn lại thì bật lại.
 */
@Service
@Transactional
public class KhuVucCtvService {

    private static final String HOAT_DONG = "HoatDong";
    private static final String TAM_NGUNG = "TamNgung";
    public static final int TOI_DA = 5;

    private final KhuVucCTVRepository khuVucCTVRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final KhuVucService khuVucService;

    public KhuVucCtvService(KhuVucCTVRepository khuVucCTVRepository, CongTacVienRepository congTacVienRepository,
                            KhuVucService khuVucService) {
        this.khuVucCTVRepository = khuVucCTVRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.khuVucService = khuVucService;
    }

    /** Quận, huyện CTV đang nhận việc (chỉ khu vực còn phục vụ), theo thứ tự danh sách khu vực. */
    @Transactional(readOnly = true)
    public List<KhuVuc> dangNhan(Integer ctvId) {
        Set<Integer> ids = new HashSet<>();
        for (KhuVucCTV k : khuVucCTVRepository.findByCongTacVien_Id(ctvId)) {
            if (HOAT_DONG.equals(k.getTrangThai()) && k.getKhuVuc() != null && HOAT_DONG.equals(k.getKhuVuc().getTrangThai())) {
                ids.add(k.getKhuVuc().getId());
            }
        }
        return khuVucService.danhSach().stream().filter(kv -> ids.contains(kv.getId())).toList();
    }

    /** "Quận 1, Quận 3" hoặc chuỗi rỗng. */
    @Transactional(readOnly = true)
    public String tomTat(Integer ctvId) {
        return String.join(", ", dangNhan(ctvId).stream().map(KhuVuc::getTenKhuVuc).toList());
    }

    /** Tóm tắt khu vực của mọi CTV (bảng danh sách HCNS, lịch phân công CSKH). */
    @Transactional(readOnly = true)
    public Map<Integer, List<KhuVuc>> theoCtv() {
        Map<Integer, Integer> thuTu = new HashMap<>();
        List<KhuVuc> ds = khuVucService.danhSach();
        for (int i = 0; i < ds.size(); i++) thuTu.put(ds.get(i).getId(), i);
        Map<Integer, List<KhuVuc>> kq = new HashMap<>();
        for (KhuVucCTV k : khuVucCTVRepository.findAll()) {
            if (HOAT_DONG.equals(k.getTrangThai()) && k.getKhuVuc() != null && thuTu.containsKey(k.getKhuVuc().getId())) {
                kq.computeIfAbsent(k.getCongTacVien().getId(), x -> new ArrayList<>()).add(k.getKhuVuc());
            }
        }
        kq.values().forEach(l -> l.sort(Comparator.comparing(kv -> thuTu.get(kv.getId()))));
        return kq;
    }

    /** Đặt lại khu vực nhận việc: 1 đến {@link #TOI_DA} quận, huyện đang phục vụ. */
    public List<KhuVuc> capNhat(Integer ctvId, Collection<Integer> khuVucIds) {
        CongTacVien ctv = congTacVienRepository.findById(ctvId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên."));
        Set<Integer> chon = new LinkedHashSet<>(khuVucIds == null ? List.of() : khuVucIds);
        chon.remove(null);
        if (chon.isEmpty()) {
            throw new IllegalArgumentException("Chọn ít nhất 1 quận, huyện nhận việc.");
        }
        if (chon.size() > TOI_DA) {
            throw new IllegalArgumentException("Chọn tối đa " + TOI_DA + " quận, huyện để kịp di chuyển giữa các đơn.");
        }
        Map<Integer, KhuVuc> hopLe = new LinkedHashMap<>();
        for (Integer id : chon) hopLe.put(id, khuVucService.layDangPhucVu(id));

        Map<Integer, KhuVucCTV> daCo = new HashMap<>();
        for (KhuVucCTV k : khuVucCTVRepository.findByCongTacVien_Id(ctvId)) {
            daCo.put(k.getKhuVuc().getId(), k);
        }
        for (KhuVucCTV k : daCo.values()) {
            String moi = hopLe.containsKey(k.getKhuVuc().getId()) ? HOAT_DONG : TAM_NGUNG;
            if (!moi.equals(k.getTrangThai())) {
                k.setTrangThai(moi);
                khuVucCTVRepository.save(k);
            }
        }
        for (KhuVuc kv : hopLe.values()) {
            if (!daCo.containsKey(kv.getId())) {
                khuVucCTVRepository.save(KhuVucCTV.builder()
                        .maKhuVucCTV(MaSinh.tao("KVC-"))
                        .congTacVien(ctv)
                        .khuVuc(kv)
                        .trangThai(HOAT_DONG)
                        .build());
            }
        }
        return dangNhan(ctvId);
    }

    public static List<Map<String, Object>> toMap(List<KhuVuc> ds) {
        List<Map<String, Object>> kq = new ArrayList<>();
        for (KhuVuc kv : ds) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", kv.getId());
            m.put("tenKhuVuc", kv.getTenKhuVuc());
            kq.add(m);
        }
        return kq;
    }
}
