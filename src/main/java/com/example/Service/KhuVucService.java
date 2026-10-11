package com.example.Service;

import com.example.Config.CacheNames;
import com.example.Model.DiaChiKhachHang;
import com.example.Model.KhuVuc;
import com.example.Model.KhuVucCTV;
import com.example.Repository.DiaChiKhachHangRepository;
import com.example.Repository.KhuVucCTVRepository;
import com.example.Repository.KhuVucRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.text.Collator;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Khu vực phục vụ = các quận, huyện (và TP. Thủ Đức) của TP. Hồ Chí Minh.
 * Mọi nơi nhập địa chỉ (sổ địa chỉ khách hàng, CSKH thêm khách, ứng viên CTV, phân công) dùng chung
 * danh sách này để địa chỉ của khách và khu vực nhận việc của cộng tác viên luôn khớp nhau.
 *
 * Khi khởi động: đồng bộ bảng KhuVuc với file data/quan-huyen-hcm.json — thêm quận còn thiếu
 * (mã KV-Q&lt;mã quận&gt;), chuẩn hoá tên, tạm ngưng khu vực ngoài danh sách (phường, xã dùng trước đây).
 * Không xoá dòng nào; địa chỉ và khu vực CTV đang gắn với khu vực đã ngưng được chuyển sang quận tương ứng.
 */
@Service
public class KhuVucService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KhuVucService.class);
    private static final String FILE_DU_LIEU = "data/quan-huyen-hcm.json";
    private static final String HOAT_DONG = "HoatDong";
    private static final String TAM_NGUNG = "TamNgung"; // CHECK chk_KV_trangThai / chk_KVCV_trangThai
    private static final Collator SAP_XEP_VI = Collator.getInstance(new Locale("vi", "VN"));
    private static final Pattern QUAN_SO = Pattern.compile("\\b(?:quan|q)\\s*\\.?\\s*(\\d{1,2})(?!\\d)");

    private final KhuVucRepository khuVucRepository;
    private final DiaChiKhachHangRepository diaChiRepository;
    private final KhuVucCTVRepository khuVucCTVRepository;
    private final CacheManager cacheManager;
    private final org.springframework.transaction.support.TransactionTemplate giaoDich;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tinhThanh = "Thành phố Hồ Chí Minh";
    /** Thứ tự hiển thị theo file dữ liệu: Quận 1 → Quận 12, quận có tên, TP. Thủ Đức, huyện. */
    private final Map<String, Integer> thuTu = new HashMap<>();

    public KhuVucService(KhuVucRepository khuVucRepository, DiaChiKhachHangRepository diaChiRepository,
                         KhuVucCTVRepository khuVucCTVRepository, CacheManager cacheManager,
                         org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.giaoDich = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.khuVucRepository = khuVucRepository;
        this.diaChiRepository = diaChiRepository;
        this.khuVucCTVRepository = khuVucCTVRepository;
        this.cacheManager = cacheManager;
    }

    public String getTinhThanh() {
        return tinhThanh;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // Gọi trong giao dịch (gọi thẳng this.dongBo() sẽ bỏ qua @Transactional)
            giaoDich.executeWithoutResult(t -> {
                try {
                    dongBo();
                } catch (Exception e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            log.warn("Không đồng bộ được danh sách quận, huyện TP.HCM: {}", e.getMessage());
        }
    }

    /** Đồng bộ bảng KhuVuc với danh sách quận, huyện TP.HCM rồi chuyển dữ liệu cũ sang quận. */
    @Transactional
    public void dongBo() throws Exception {
        JsonNode goc;
        try (InputStream in = new ClassPathResource(FILE_DU_LIEU).getInputStream()) {
            goc = objectMapper.readTree(in);
        }
        tinhThanh = goc.path("tinhThanh").asText(tinhThanh);

        Set<String> maChuan = new HashSet<>();
        int i = 0;
        for (JsonNode q : goc.path("quanHuyen")) {
            String ma = "KV-Q" + q.path("ma").asText();
            maChuan.add(ma);
            thuTu.put(ma, i++);
        }

        List<KhuVuc> hienCo = khuVucRepository.findAll();
        Map<String, KhuVuc> theoMa = new HashMap<>();
        Map<String, KhuVuc> theoTen = new HashMap<>();
        for (KhuVuc kv : hienCo) {
            theoMa.put(kv.getMaKhuVuc(), kv);
            // Dòng cũ cùng tên quận (dữ liệu mẫu trước đây) được dùng lại thay vì tạo dòng mới
            if (laTinhNay(kv.getTinhThanh()) && !maChuan.contains(kv.getMaKhuVuc())) {
                theoTen.putIfAbsent(tenSoKhop(kv.getTenKhuVuc()), kv);
            }
        }

        Set<Integer> trongDanhSach = new HashSet<>();
        Map<String, KhuVuc> quanTheoPhuongMoi = new HashMap<>();
        int themMoi = 0, capNhat = 0;
        for (JsonNode q : goc.path("quanHuyen")) {
            String ma = "KV-Q" + q.path("ma").asText();
            String ten = q.path("ten").asText();
            KhuVuc kv = theoMa.get(ma);
            if (kv == null) kv = theoTen.remove(tenSoKhop(ten));
            if (kv == null) {
                kv = khuVucRepository.save(KhuVuc.builder().maKhuVuc(ma).tenKhuVuc(ten).quanHuyen(ten)
                        .tinhThanh(tinhThanh).trangThai(HOAT_DONG).build());
                themMoi++;
            } else if (!ten.equals(kv.getTenKhuVuc()) || !ten.equals(kv.getQuanHuyen())
                    || !tinhThanh.equals(kv.getTinhThanh()) || !HOAT_DONG.equals(kv.getTrangThai())
                    || !ma.equals(kv.getMaKhuVuc())) {
                kv.setMaKhuVuc(ma);
                kv.setTenKhuVuc(ten);
                kv.setQuanHuyen(ten);
                kv.setTinhThanh(tinhThanh);
                kv.setTrangThai(HOAT_DONG);
                kv = khuVucRepository.save(kv);
                capNhat++;
            }
            trongDanhSach.add(kv.getId());
            for (JsonNode p : q.path("phuongMoi")) quanTheoPhuongMoi.put(chuanHoa(p.asText()), kv);
        }

        // Khu vực ngoài danh sách (phường, xã dùng trước đây, tỉnh khác): tạm ngưng, vẫn giữ cho dữ liệu cũ
        int ngung = 0;
        for (KhuVuc kv : hienCo) {
            if (!trongDanhSach.contains(kv.getId()) && HOAT_DONG.equals(kv.getTrangThai())) {
                kv.setTrangThai(TAM_NGUNG);
                khuVucRepository.save(kv);
                ngung++;
            }
        }

        List<KhuVuc> danhSachQuan = khuVucRepository.findAllById(trongDanhSach);
        int chuyen = chuyenDuLieuCu(danhSachQuan, quanTheoPhuongMoi);

        if (themMoi + capNhat + ngung + chuyen > 0) {
            log.info("Đồng bộ quận, huyện TP.HCM: thêm {}, cập nhật {}, tạm ngưng {}, chuyển {} địa chỉ / khu vực CTV sang quận",
                    themMoi, capNhat, ngung, chuyen);
            for (String ten : List.of(CacheNames.KHU_VUC, CacheNames.DICH_VU)) {
                Cache cache = cacheManager.getCache(ten);
                if (cache != null) cache.clear();
            }
        }
    }

    /**
     * Địa chỉ và khu vực CTV còn gắn với khu vực đã ngưng (phường, xã trước đây) được chuyển sang quận:
     * đoán từ chữ trong địa chỉ ("Quận 3", "Q.10", "Bình Thạnh"...), không được thì theo phường cũ.
     * Địa chỉ không đoán được thì bỏ khu vực để khách chọn lại; khu vực CTV không đoán được thì tạm ngưng.
     */
    private int chuyenDuLieuCu(List<KhuVuc> quans, Map<String, KhuVuc> quanTheoPhuongMoi) {
        int dem = 0;
        for (DiaChiKhachHang dc : diaChiRepository.findAll()) {
            KhuVuc cu = dc.getKhuVuc();
            if (cu == null || HOAT_DONG.equals(cu.getTrangThai())) continue;
            KhuVuc moi = doanQuan(dc.getDiaChiChiTiet(), quans)
                    .orElse(quanTheoPhuongMoi.get(chuanHoa(boTienTo(cu.getTenKhuVuc()))));
            if (moi == null && !HOAT_DONG.equals(dc.getTrangThai())) continue; // địa chỉ đã xoá: giữ nguyên lịch sử
            dc.setKhuVuc(moi);
            diaChiRepository.save(dc);
            dem++;
        }

        List<KhuVucCTV> tatCa = khuVucCTVRepository.findAll();
        Set<String> daCo = new HashSet<>();
        for (KhuVucCTV k : tatCa) {
            if (HOAT_DONG.equals(k.getKhuVuc().getTrangThai())) {
                daCo.add(k.getCongTacVien().getId() + ":" + k.getKhuVuc().getId());
            }
        }
        for (KhuVucCTV k : tatCa) {
            KhuVuc cu = k.getKhuVuc();
            if (HOAT_DONG.equals(cu.getTrangThai())) continue;
            KhuVuc moi = quanTheoPhuongMoi.get(chuanHoa(boTienTo(cu.getTenKhuVuc())));
            String khoa = moi != null ? k.getCongTacVien().getId() + ":" + moi.getId() : null;
            if (moi != null && daCo.add(khoa)) {
                k.setKhuVuc(moi);
            } else if (TAM_NGUNG.equals(k.getTrangThai())) {
                continue;
            } else {
                k.setTrangThai(TAM_NGUNG); // CTV chọn lại quận nhận việc
            }
            khuVucCTVRepository.save(k);
            dem++;
        }
        return dem;
    }

    /** Đoán quận từ chữ trong địa chỉ: "Quận 3", "Q.10", "Q1", "Bình Thạnh", "Thủ Đức", "Củ Chi"... */
    public Optional<KhuVuc> doanQuan(String diaChi, List<KhuVuc> quans) {
        if (diaChi == null || diaChi.isBlank()) return Optional.empty();
        String t = chuanHoa(diaChi);
        Matcher m = QUAN_SO.matcher(t);
        while (m.find()) {
            String ten = "quan " + Integer.parseInt(m.group(1));
            Optional<KhuVuc> kv = quans.stream().filter(q -> chuanHoa(q.getTenKhuVuc()).equals(ten)).findFirst();
            if (kv.isPresent()) return kv;
        }
        // Tên riêng: ưu tiên có tiền tố ("quận bình thạnh"), sau đó tên trơn; tên dài trước để tránh khớp nhầm
        List<KhuVuc> coTen = quans.stream().filter(q -> !chuanHoa(q.getTenKhuVuc()).matches("quan \\d+"))
                .sorted(Comparator.comparing((KhuVuc q) -> -boTienTo(q.getTenKhuVuc()).length())).toList();
        for (KhuVuc q : coTen) {
            if (t.contains(chuanHoa(q.getTenKhuVuc()))) return Optional.of(q);
        }
        for (KhuVuc q : coTen) {
            if (t.matches(".*\\b" + Pattern.quote(chuanHoa(boTienTo(q.getTenKhuVuc()))) + "\\b.*")) return Optional.of(q);
        }
        return Optional.empty();
    }

    /** Quận, huyện đang phục vụ: Quận 1 → Quận 12, quận có tên, TP. Thủ Đức, huyện. */
    @Transactional(readOnly = true)
    public List<KhuVuc> danhSach() {
        List<KhuVuc> ds = new ArrayList<>(khuVucRepository.findByTinhThanh(tinhThanh).stream()
                .filter(kv -> HOAT_DONG.equals(kv.getTrangThai()))
                .toList());
        ds.sort(Comparator.comparing((KhuVuc kv) -> thuTu.getOrDefault(kv.getMaKhuVuc(), Integer.MAX_VALUE))
                .thenComparing(KhuVuc::getTenKhuVuc, SAP_XEP_VI));
        return ds;
    }

    /** Tìm quận, huyện đang phục vụ theo tên ("Quận 3", "Bình Thạnh", "quan binh thanh"); không tạo mới. */
    @Transactional(readOnly = true)
    public Optional<KhuVuc> timTheoTen(String ten) {
        if (ten == null || ten.isBlank()) return Optional.empty();
        List<KhuVuc> ds = danhSach();
        String dung = tenSoKhop(ten);
        Optional<KhuVuc> khop = ds.stream().filter(kv -> tenSoKhop(kv.getTenKhuVuc()).equals(dung)).findFirst();
        if (khop.isPresent()) return khop;
        String khongDau = chuanHoa(ten);
        List<KhuVuc> gan = ds.stream().filter(kv -> chuanHoa(kv.getTenKhuVuc()).equals(khongDau)
                || chuanHoa(boTienTo(kv.getTenKhuVuc())).equals(khongDau)).toList();
        return gan.size() == 1 ? Optional.of(gan.get(0)) : Optional.empty();
    }

    /** Tên để so khớp: giữ dấu, không phân biệt hoa thường và khoảng trắng thừa. */
    private static String tenSoKhop(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /** Khu vực đang phục vụ theo id; báo lỗi nếu không có trong danh sách. */
    @Transactional(readOnly = true)
    public KhuVuc layDangPhucVu(Integer id) {
        KhuVuc kv = khuVucRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Khu vực không hợp lệ."));
        if (!HOAT_DONG.equals(kv.getTrangThai()) || !laTinhNay(kv.getTinhThanh())) {
            throw new IllegalArgumentException("Hiện chỉ phục vụ các quận, huyện thuộc " + tinhThanh + ".");
        }
        return kv;
    }

    private static String boTienTo(String ten) {
        return ten == null ? "" : ten.replaceFirst("^(Phường|Xã|Đặc khu|Quận|Huyện|Thành phố)\\s+", "");
    }

    private boolean laTinhNay(String tinh) {
        if (tinh == null) return false;
        String t = chuanHoa(tinh);
        return t.equals(chuanHoa(tinhThanh)) || t.contains("ho chi minh") || t.equals("tp.hcm") || t.equals("tphcm");
    }

    /** So khớp tên không phân biệt hoa thường, dấu, khoảng trắng thừa. */
    static String chuanHoa(String s) {
        if (s == null) return "";
        String bo = java.text.Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('đ', 'd');
        return bo.replaceAll("\\s+", " ");
    }
}
