package com.example.Service;

import com.example.Model.KhuVuc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Địa giới hành chính cho ĐỊA CHỈ (2 cấp từ 01/07/2025: tỉnh/thành → phường/xã), lấy từ
 * https://provinces.open-api.vn/api/v2, và cách suy ra KHU VỰC của một địa chỉ.
 *
 * - Địa chỉ: nơi ở / nơi làm dịch vụ = số nhà, tên đường + phường/xã + tỉnh/thành (bất kỳ tỉnh nào).
 * - Khu vực: cấp quận, huyện (lớn hơn phường, nhỏ hơn tỉnh) – bảng KhuVuc. API v2 không còn cấp quận,
 *   nên khu vực của một phường mới lấy theo quận cũ chứa phường đó (/api/v2/w/{code}/to-legacies/).
 *   Bảng phường TP.HCM → quận cũ đã được lấy sẵn vào data/phuong-xa-hcm.json (trường quanCu) để
 *   không phụ thuộc mạng; phường nằm trên nhiều quận cũ thì người dùng chọn một trong các quận đó.
 *
 * Tỉnh khác và phường xã thuộc tỉnh khác lấy trực tiếp từ API (giữ trong bộ nhớ sau lần đầu).
 */
@Service
public class DiaGioiService {

    private static final Logger log = LoggerFactory.getLogger(DiaGioiService.class);
    private static final String API = "https://provinces.open-api.vn/api/v2";
    private static final String API_V1 = "https://provinces.open-api.vn/api/v1";
    private static final String FILE_HCM = "data/phuong-xa-hcm.json";
    public static final int MA_HCM = 79;

    private final KhuVucService khuVucService;
    private final ObjectMapper om = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private volatile List<Map<String, Object>> tinhCache;
    private final Map<Integer, List<Map<String, Object>>> phuongCache = new ConcurrentHashMap<>();
    private volatile List<Map<String, Object>> tinhCacheV1;
    private final Map<Integer, List<Map<String, Object>>> quanCacheV1 = new ConcurrentHashMap<>();
    private final Map<Integer, List<Map<String, Object>>> phuongCacheV1 = new ConcurrentHashMap<>();
    /** Mã phường TP.HCM → mã quận cũ (thuộc TP.HCM cũ), theo thứ tự nhiều phường cũ nhất trước. */
    private final Map<Integer, List<Integer>> quanCuTheoPhuong = new HashMap<>();
    private String tenHcm = "Thành phố Hồ Chí Minh";

    public DiaGioiService(KhuVucService khuVucService) {
        this.khuVucService = khuVucService;
        try (InputStream in = new ClassPathResource(FILE_HCM).getInputStream()) {
            JsonNode goc = om.readTree(in);
            tenHcm = goc.path("tinhThanh").asText(tenHcm);
            List<Map<String, Object>> ds = new ArrayList<>();
            for (JsonNode p : goc.path("phuongXa")) {
                int ma = p.path("ma").asInt();
                ds.add(phuong(ma, p.path("ten").asText()));
                List<Integer> quan = new ArrayList<>();
                for (JsonNode q : p.path("quanCu")) {
                    if (q.path("tinh").asInt() == MA_HCM) quan.add(q.path("ma").asInt());
                }
                quanCuTheoPhuong.put(ma, quan);
            }
            phuongCache.put(MA_HCM, ds);
        } catch (Exception e) {
            log.warn("Không đọc được {}: {}", FILE_HCM, e.getMessage());
        }
    }

    private static Map<String, Object> phuong(int ma, String ten) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ma", ma);
        m.put("ten", ten);
        return m;
    }

    // =========================================================================
    // TỈNH, PHƯỜNG (địa chỉ)
    // =========================================================================

    /** 34 tỉnh, thành; mạng lỗi thì chỉ còn TP.HCM. */
    public List<Map<String, Object>> tinhThanh() {
        if (tinhCache != null) return tinhCache;
        try {
            List<Map<String, Object>> ds = new ArrayList<>();
            for (JsonNode p : goi(API + "/p/")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ma", p.path("code").asInt());
                m.put("ten", p.path("name").asText());
                ds.add(m);
            }
            // TP.HCM (nơi bTaskee phục vụ) lên đầu
            ds.sort(Comparator.comparing((Map<String, Object> m) -> !Objects.equals(m.get("ma"), MA_HCM)));
            tinhCache = List.copyOf(ds);
            return tinhCache;
        } catch (Exception e) {
            log.warn("Không tải được danh sách tỉnh từ provinces.open-api.vn: {}", e.getMessage());
            Map<String, Object> hcm = new LinkedHashMap<>();
            hcm.put("ma", MA_HCM);
            hcm.put("ten", tenHcm);
            return List.of(hcm);
        }
    }

    /** Phường, xã của một tỉnh (kèm khu vực ứng với mỗi phường nếu thuộc khu vực phục vụ). */
    public List<Map<String, Object>> phuongXa(int maTinh) {
        List<Map<String, Object>> ds = phuongCache.get(maTinh);
        if (ds == null) {
            try {
                List<Map<String, Object>> moi = new ArrayList<>();
                for (JsonNode w : goi(API + "/p/" + maTinh + "?depth=2").path("wards")) {
                    moi.add(phuong(w.path("code").asInt(), w.path("name").asText()));
                }
                ds = List.copyOf(moi);
                phuongCache.put(maTinh, ds);
            } catch (Exception e) {
                throw new IllegalStateException("Không tải được danh sách phường, xã. Thử lại sau ít phút.");
            }
        }
        List<Map<String, Object>> kq = new ArrayList<>();
        for (Map<String, Object> p : ds) {
            Map<String, Object> m = new LinkedHashMap<>(p);
            m.put("khuVuc", KhuVucCtvService.toMap(khuVucCuaPhuong((Integer) p.get("ma"))));
            kq.add(m);
        }
        kq.sort(Comparator.comparing((Map<String, Object> m) -> boTienTo((String) m.get("ten")),
                java.text.Collator.getInstance(new Locale("vi", "VN"))));
        return kq;
    }

    public Optional<String> tenTinh(Integer maTinh) {
        if (maTinh == null) return Optional.empty();
        if (maTinh == MA_HCM) return Optional.of(tenHcm);
        return tinhThanh().stream().filter(t -> Objects.equals(t.get("ma"), maTinh))
                .map(t -> (String) t.get("ten")).findFirst();
    }

    public Optional<String> tenPhuong(Integer maTinh, Integer maPhuong) {
        if (maTinh == null || maPhuong == null) return Optional.empty();
        List<Map<String, Object>> ds = phuongCache.get(maTinh);
        if (ds == null) {
            phuongXa(maTinh);
            ds = phuongCache.getOrDefault(maTinh, List.of());
        }
        return ds.stream().filter(p -> Objects.equals(p.get("ma"), maPhuong)).map(p -> (String) p.get("ten")).findFirst();
    }

    // =========================================================================
    // KHU VỰC CỦA ĐỊA CHỈ
    // =========================================================================

    /** Khu vực (quận, huyện đang phục vụ) chứa phường này; rỗng nếu ngoài khu vực phục vụ. */
    public List<KhuVuc> khuVucCuaPhuong(Integer maPhuong) {
        List<Integer> quan = maPhuong == null ? null : quanCuTheoPhuong.get(maPhuong);
        if (quan == null || quan.isEmpty()) return List.of();
        Map<String, KhuVuc> theoMa = new HashMap<>();
        for (KhuVuc kv : khuVucService.danhSach()) theoMa.put(kv.getMaKhuVuc(), kv);
        List<KhuVuc> kq = new ArrayList<>();
        for (Integer q : quan) {
            KhuVuc kv = theoMa.get("KV-Q" + q);
            if (kv != null) kq.add(kv);
        }
        return kq;
    }

    /** Kết quả xác định địa chỉ: chuỗi đầy đủ và khu vực (null = ngoài khu vực phục vụ). */
    public record DiaChiDaXacDinh(String diaChiDayDu, KhuVuc khuVuc, String tenPhuong, String tenTinh) { }

    /**
     * Ghép địa chỉ "số nhà, tên đường, phường/xã, tỉnh/thành" và chọn khu vực.
     * Phường thuộc 1 quận cũ: tự chọn; nhiều quận cũ: khuVucId phải là một trong số đó (không gửi thì lấy quận đầu).
     */
    public DiaChiDaXacDinh xacDinh(String soNhaTenDuong, Integer maTinh, Integer maPhuong, Integer khuVucId) {
        if (soNhaTenDuong == null || soNhaTenDuong.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số nhà, tên đường.");
        }
        String tinh = tenTinh(maTinh).orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn tỉnh, thành phố."));
        String phuong = tenPhuong(maTinh, maPhuong).orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn phường, xã."));
        String soNha = soNhaTenDuong.trim().replaceAll("\\s+", " ");
        if (soNha.length() > 200) throw new IllegalArgumentException("Số nhà, tên đường tối đa 200 ký tự.");

        List<KhuVuc> ungVien = khuVucCuaPhuong(maPhuong);
        KhuVuc kv = null;
        if (!ungVien.isEmpty()) {
            kv = khuVucId == null ? ungVien.get(0) : ungVien.stream().filter(k -> k.getId().equals(khuVucId)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(phuong + " thuộc khu vực "
                            + String.join(" hoặc ", ungVien.stream().map(KhuVuc::getTenKhuVuc).toList()) + "."));
        }
        return new DiaChiDaXacDinh(soNha + ", " + phuong + ", " + tinh, kv, phuong, tinh);
    }

    /**
     * Tách địa chỉ đã lưu "số nhà, đường, phường, tỉnh" để điền lại form sửa (so tên với danh sách API).
     * Không khớp thì trả về soNhaTenDuong = cả chuỗi để người dùng chọn lại phường, tỉnh.
     */
    public Map<String, Object> tach(String diaChi) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("soNhaTenDuong", diaChi == null ? "" : diaChi);
        if (diaChi == null) return m;
        List<String> phan = new ArrayList<>(Arrays.stream(diaChi.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList());
        if (phan.size() < 3) return m;
        String tenTinhCuoi = phan.get(phan.size() - 1);
        Integer maTinh = null;
        for (Map<String, Object> t : tinhThanh()) {
            if (KhuVucService.chuanHoa((String) t.get("ten")).equals(KhuVucService.chuanHoa(tenTinhCuoi))) {
                maTinh = (Integer) t.get("ma");
                break;
            }
        }
        if (maTinh == null) return m;
        String tenPhuongKe = KhuVucService.chuanHoa(phan.get(phan.size() - 2));
        List<Map<String, Object>> ds = phuongCache.containsKey(maTinh) ? phuongCache.get(maTinh) : List.of();
        if (ds.isEmpty()) {
            try { phuongXa(maTinh); ds = phuongCache.getOrDefault(maTinh, List.of()); } catch (Exception ignored) { }
        }
        for (Map<String, Object> p : ds) {
            if (KhuVucService.chuanHoa((String) p.get("ten")).equals(tenPhuongKe)) {
                m.put("maTinh", maTinh);
                m.put("maPhuong", p.get("ma"));
                m.put("soNhaTenDuong", String.join(", ", phan.subList(0, phan.size() - 2)));
                break;
            }
        }
        return m;
    }

    // =========================================================================
    // API v1: ĐỊA CHỈ TRUYỀN THỐNG 3 CẤP (TỈNH -> QUẬN/HUYỆN -> PHƯỜNG/XÃ)
    // =========================================================================

    public List<Map<String, Object>> tinhThanhV1() {
        if (tinhCacheV1 != null) return tinhCacheV1;
        try {
            List<Map<String, Object>> ds = new ArrayList<>();
            for (JsonNode p : goi(API_V1 + "/p/")) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ma", p.path("code").asInt());
                m.put("ten", p.path("name").asText());
                ds.add(m);
            }
            ds.sort(Comparator.comparing((Map<String, Object> m) -> !Objects.equals(m.get("ma"), MA_HCM)));
            tinhCacheV1 = List.copyOf(ds);
            return tinhCacheV1;
        } catch (Exception e) {
            log.warn("Không tải được danh sách tỉnh v1: {}", e.getMessage());
            return tinhThanh();
        }
    }

    public List<Map<String, Object>> quanHuyenV1(int maTinh) {
        List<Map<String, Object>> ds = quanCacheV1.get(maTinh);
        if (ds == null) {
            try {
                List<Map<String, Object>> moi = new ArrayList<>();
                for (JsonNode d : goi(API_V1 + "/p/" + maTinh + "?depth=2").path("districts")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ma", d.path("code").asInt());
                    m.put("ten", d.path("name").asText());
                    m.put("maTinh", maTinh);
                    // Kiểm tra xem quận này có thuộc khu vực bTaskee phục vụ không
                    if (khuVucService != null) {
                        Optional<KhuVuc> kv = khuVucService.timTheoTen(d.path("name").asText());
                        if (kv.isPresent()) {
                            m.put("khuVucId", kv.get().getId());
                            m.put("tenKhuVuc", kv.get().getTenKhuVuc());
                        }
                    }
                    moi.add(m);
                }
                ds = List.copyOf(moi);
                quanCacheV1.put(maTinh, ds);
            } catch (Exception e) {
                throw new IllegalStateException("Không tải được danh sách quận, huyện. Thử lại sau ít phút.");
            }
        }
        return ds;
    }

    public List<Map<String, Object>> phuongXaV1(int maQuan) {
        List<Map<String, Object>> ds = phuongCacheV1.get(maQuan);
        if (ds == null) {
            try {
                List<Map<String, Object>> moi = new ArrayList<>();
                for (JsonNode w : goi(API_V1 + "/d/" + maQuan + "?depth=2").path("wards")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ma", w.path("code").asInt());
                    m.put("ten", w.path("name").asText());
                    m.put("maQuan", maQuan);
                    moi.add(m);
                }
                ds = List.copyOf(moi);
                phuongCacheV1.put(maQuan, ds);
            } catch (Exception e) {
                throw new IllegalStateException("Không tải được danh sách phường, xã. Thử lại sau ít phút.");
            }
        }
        return ds;
    }

    public Optional<String> tenQuanV1(Integer maTinh, Integer maQuan) {
        if (maTinh == null || maQuan == null) return Optional.empty();
        return quanHuyenV1(maTinh).stream().filter(q -> Objects.equals(q.get("ma"), maQuan))
                .map(q -> (String) q.get("ten")).findFirst();
    }

    public Optional<String> tenPhuongV1(Integer maQuan, Integer maPhuong) {
        if (maQuan == null || maPhuong == null) return Optional.empty();
        return phuongXaV1(maQuan).stream().filter(p -> Objects.equals(p.get("ma"), maPhuong))
                .map(p -> (String) p.get("ten")).findFirst();
    }

    /** Ghép địa chỉ kiểu cũ 3 cấp: Số nhà, Phường, Quận, Tỉnh và tự động xác định KhuVuc */
    public DiaChiDaXacDinh xacDinhV1(String soNhaTenDuong, Integer maTinh, Integer maQuan, Integer maPhuong) {
        if (soNhaTenDuong == null || soNhaTenDuong.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số nhà, tên đường.");
        }
        // Tên tỉnh theo danh mục cũ (v1): mã tỉnh cũ đã sáp nhập không còn trong danh mục mới
        String tinh = tinhThanhV1().stream()
                .filter(t -> Objects.equals(t.get("ma"), maTinh)).map(t -> (String) t.get("ten")).findFirst()
                .or(() -> tenTinh(maTinh))
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn tỉnh, thành phố."));
        String quan = tenQuanV1(maTinh, maQuan).orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn quận, huyện."));
        String phuong = tenPhuongV1(maQuan, maPhuong).orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn phường, xã."));
        String soNha = soNhaTenDuong.trim().replaceAll("\\s+", " ");

        KhuVuc kv = null;
        if (khuVucService != null) {
            kv = khuVucService.timTheoTen(quan).orElse(null);
        }

        String diaChiDayDu = soNha + ", " + phuong + ", " + quan + ", " + tinh;
        return new DiaChiDaXacDinh(diaChiDayDu, kv, phuong, tinh);
    }

    // =========================================================================
    // Ô TÌM ĐỊA CHỈ: gõ tên tỉnh / quận, dò cả sau sáp nhập (v2) lẫn trước sáp nhập (v1)
    // =========================================================================

    private volatile List<Map<String, Object>> quanTatCaV1;

    /** Mọi quận, huyện cũ (một lần gọi /api/v1/?depth=2), kèm tên tỉnh cũ; mạng lỗi thì rỗng. */
    private List<Map<String, Object>> quanTatCaV1() {
        if (quanTatCaV1 != null) return quanTatCaV1;
        try {
            List<Map<String, Object>> ds = new ArrayList<>();
            for (JsonNode p : goi(API_V1 + "/?depth=2")) {
                for (JsonNode d : p.path("districts")) {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("ma", d.path("code").asInt());
                    m.put("ten", d.path("name").asText());
                    m.put("maTinh", p.path("code").asInt());
                    m.put("tenTinh", p.path("name").asText());
                    ds.add(m);
                }
            }
            quanTatCaV1 = List.copyOf(ds);
            return quanTatCaV1;
        } catch (Exception e) {
            log.warn("Không tải được danh sách quận, huyện v1: {}", e.getMessage());
            return List.of();
        }
    }

    /** Gợi ý cho ô tìm địa chỉ: [{loai: tinhMoi|tinhCu|quanCu, maTinh, maQuan?, ten, moTa}]. */
    public List<Map<String, Object>> timTinhQuan(String q) {
        boolean rong = q == null || q.isBlank();
        return loc(q, tinhThanh(), rong ? List.of() : tinhThanhV1(), rong ? List.of() : quanTatCaV1(), 20);
    }

    private static final Set<String> VIET_TAT_HCM = Set.of("hcm", "tphcm", "tp hcm", "tp.hcm", "sai gon", "saigon", "sg");

    /**
     * Lọc gợi ý (không phân biệt dấu). Xếp: khớp đầu tên (bỏ "Tỉnh", "Quận"…) trước, khớp giữa tên,
     * rồi quận khớp theo tên tỉnh cũ; cùng mức thì tỉnh mới → tỉnh cũ → quận cũ. Chuỗi rỗng: tỉnh sau sáp nhập.
     */
    static List<Map<String, Object>> loc(String q, List<Map<String, Object>> tinhMoi, List<Map<String, Object>> tinhCu,
                                         List<Map<String, Object>> quanCu, int gioiHan) {
        String k = boDau(q);
        if (VIET_TAT_HCM.contains(k)) k = "ho chi minh";
        record Ung(int diem, int loai, Map<String, Object> m) { }
        List<Ung> ds = new ArrayList<>();
        for (Map<String, Object> t : tinhMoi) {
            int d = diem(k, (String) t.get("ten"), null);
            if (d >= 0) ds.add(new Ung(d, 0, goiY("tinhMoi", (Integer) t.get("ma"), null, (String) t.get("ten"), "Sau sáp nhập · chọn phường, xã mới")));
        }
        if (!k.isEmpty()) {
            for (Map<String, Object> t : tinhCu) {
                int d = diem(k, (String) t.get("ten"), null);
                if (d >= 0) ds.add(new Ung(d, 1, goiY("tinhCu", (Integer) t.get("ma"), null, (String) t.get("ten"), "Trước sáp nhập · chọn quận, huyện")));
            }
            for (Map<String, Object> qu : quanCu) {
                int d = diem(k, (String) qu.get("ten"), (String) qu.get("tenTinh"));
                if (d >= 0) ds.add(new Ung(d, 2, goiY("quanCu", (Integer) qu.get("maTinh"), (Integer) qu.get("ma"),
                        (String) qu.get("ten"), qu.get("tenTinh") + " · trước sáp nhập")));
            }
        }
        ds.sort(Comparator.comparingInt(Ung::diem).thenComparingInt(Ung::loai));
        return ds.stream().limit(gioiHan).map(Ung::m).toList();
    }

    /** 0 = khớp đầu tên, 1 = khớp giữa tên, 2 = khớp tên tỉnh (của quận), -1 = không khớp. */
    private static int diem(String k, String ten, String tenTinh) {
        if (k.isEmpty()) return 0;
        String t = boDau(ten);
        if (t.replaceFirst("^(tinh|thanh pho|quan|huyen|thi xa)\\s+", "").startsWith(k)) return 0;
        if (t.contains(k)) return 1;
        return tenTinh != null && boDau(tenTinh).contains(k) ? 2 : -1;
    }

    private static Map<String, Object> goiY(String loai, Integer maTinh, Integer maQuan, String ten, String moTa) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("loai", loai);
        m.put("maTinh", maTinh);
        if (maQuan != null) m.put("maQuan", maQuan);
        m.put("ten", ten);
        m.put("moTa", moTa);
        return m;
    }

    private static String boDau(String s) {
        if (s == null) return "";
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D').toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private JsonNode goi(String url) throws Exception {
        HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) throw new IllegalStateException("HTTP " + res.statusCode());
        return om.readTree(res.body());
    }

    private static String boTienTo(String ten) {
        return ten == null ? "" : ten.replaceFirst("^(Phường|Xã|Đặc khu|Thị trấn)\\s+", "");
    }
}
