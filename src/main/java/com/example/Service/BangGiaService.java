package com.example.Service;

import com.example.Config.CacheNames;
import com.example.Model.BangGiaDichVu;
import com.example.Model.DichVu;
import com.example.Repository.BangGiaDichVuRepository;
import com.example.Repository.DichVuRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Bảng giá theo thời gian. Cập nhật giá không ghi đè gói cũ mà tạo một bảng giá mới có hiệu lực
 * từ NgayApDung đến NgayKetThuc (DATETIME, có giờ; kết thúc để trống = không giới hạn).
 *
 * Giá áp dụng cho một dịch vụ vào ngày D = bảng giá còn hiệu lực ngày D (không bị ngừng thủ công,
 * NgayApDung <= D <= NgayKetThuc) có NgayApDung mới nhất. Bảng giá tạm (có ngày kết thúc) hết hạn thì
 * giá tự quay về bảng giá chưa kết thúc mới nhất.
 *
 * Trạng thái (cột TrangThai, không đổi cấu trúc bảng):
 *   CSDL chỉ cho 2 giá trị (CHECK chk_BG_trangThai): DangApDung = còn hiệu lực (kể cả chưa tới giờ áp dụng),
 *   HetHan = đã kết thúc / bị ngừng. "Sắp áp dụng" chỉ để hiển thị, suy ra từ NgayApDung > hiện tại (xem hienThi()).
 * Mỗi phút, khi khởi động và sau mỗi lần sửa: cập nhật trạng thái theo ngày và chép giá đang áp dụng
 * vào DichVu.GiaHienTai (các trang khách hàng, ứng dụng đọc cột này).
 */
@Service
public class BangGiaService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BangGiaService.class);
    /** Chỉ dùng để hiển thị / lọc trên trang HCNS, không lưu vào CSDL. */
    public static final String CHUA_AP_DUNG = "ChuaApDung";
    public static final String DANG_AP_DUNG = "DangApDung";
    public static final String HET_HAN = "HetHan";

    private final BangGiaDichVuRepository bangGiaRepository;
    private final DichVuRepository dichVuRepository;
    private final CacheManager cacheManager;
    /** dichVuId -> giá gốc (giá sẽ quay về) khi đang chạy một đợt giá tạm; tính lại mỗi lần lamMoi(). */
    private volatile Map<Integer, BigDecimal> giaGocTheoDichVu = Map.of();

    public BangGiaService(BangGiaDichVuRepository bangGiaRepository, DichVuRepository dichVuRepository,
                          CacheManager cacheManager) {
        this.bangGiaRepository = bangGiaRepository;
        this.dichVuRepository = dichVuRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            lamMoi();
        } catch (Exception e) {
            log.warn("Không cập nhật được bảng giá khi khởi động: {}", e.getMessage());
        }
    }

    /** Mỗi phút: bảng giá tới giờ áp dụng / hết hạn được chuyển trạng thái, giá dịch vụ đổi theo đúng phút. */
    @Scheduled(cron = "0 * * * * *")
    public void chayMoiPhut() {
        lamMoi();
    }

    /** Cập nhật trạng thái theo thời điểm hiện tại và đồng bộ DichVu.GiaHienTai. */
    @Transactional
    public void lamMoi() {
        LocalDateTime bayGio = LocalDateTime.now();
        List<BangGiaDichVu> tatCa = bangGiaRepository.findAll();
        int doiTrangThai = 0;
        for (BangGiaDichVu bg : tatCa) {
            String moi = trangThaiTheoLuc(bg, bayGio);
            if (!moi.equals(bg.getTrangThai())) {
                bg.setTrangThai(moi);
                bangGiaRepository.save(bg);
                doiTrangThai++;
            }
        }

        Map<Integer, List<BangGiaDichVu>> theoDichVu = new HashMap<>();
        for (BangGiaDichVu bg : tatCa) {
            if (bg.getDichVu() != null) theoDichVu.computeIfAbsent(bg.getDichVu().getId(), k -> new ArrayList<>()).add(bg);
        }
        int doiGia = 0;
        Map<Integer, BigDecimal> giaGocMoi = new HashMap<>();
        for (DichVu dv : dichVuRepository.findAll()) {
            List<BangGiaDichVu> cuaDv = theoDichVu.getOrDefault(dv.getId(), List.of());
            Optional<BangGiaDichVu> ap = chonGia(cuaDv, bayGio);
            giaGoc(cuaDv, ap.orElse(null), bayGio).ifPresent(g -> giaGocMoi.put(dv.getId(), g));
            if (ap.isPresent() && ap.get().getDonGia() != null
                    && (dv.getGiaHienTai() == null || ap.get().getDonGia().compareTo(dv.getGiaHienTai()) != 0)) {
                dv.setGiaHienTai(ap.get().getDonGia());
                dichVuRepository.save(dv);
                doiGia++;
            }
        }
        boolean doiGiaGoc = !giaGocMoi.equals(giaGocTheoDichVu);
        giaGocTheoDichVu = Map.copyOf(giaGocMoi);
        if (doiTrangThai + doiGia > 0 || doiGiaGoc) {
            log.info("Bảng giá: đổi trạng thái {} gói, cập nhật giá {} dịch vụ", doiTrangThai, doiGia);
            xoaCache();
        }
    }

    /** Bảng giá áp dụng cho dịch vụ tại một thời điểm (rỗng nếu không có bảng giá nào còn hiệu lực lúc đó). */
    @Transactional(readOnly = true)
    public Optional<BangGiaDichVu> giaApDung(Integer dichVuId, LocalDateTime luc) {
        if (dichVuId == null) return Optional.empty();
        return chonGia(bangGiaRepository.findByDichVu_Id(dichVuId), luc != null ? luc : LocalDateTime.now());
    }

    /**
     * Giá gốc để hiển thị gạch ngang: khi giá đang tính đến từ một bảng giá có thời hạn (đợt tăng / giảm tạm),
     * giá gốc là giá của bảng giá không thời hạn mới nhất (giá sẽ quay về sau đợt). Null nếu không có đợt tạm
     * hoặc giá không đổi.
     */
    public BigDecimal giaGoc(Integer dichVuId) {
        return dichVuId == null ? null : giaGocTheoDichVu.get(dichVuId);
    }

    static Optional<BigDecimal> giaGoc(List<BangGiaDichVu> cuaDv, BangGiaDichVu dangApDung, LocalDateTime luc) {
        if (dangApDung == null || dangApDung.getNgayKetThuc() == null || dangApDung.getDonGia() == null) return Optional.empty();
        return chonGia(cuaDv.stream().filter(bg -> bg.getNgayKetThuc() == null).toList(), luc)
                .map(BangGiaDichVu::getDonGia)
                .filter(g -> g.compareTo(dangApDung.getDonGia()) != 0);
    }

    /** Theo ngày làm (chưa biết giờ): lấy giá lúc bắt đầu ngày đó, hôm nay thì lấy giá hiện tại. */
    @Transactional(readOnly = true)
    public Optional<BangGiaDichVu> giaApDung(Integer dichVuId, LocalDate ngay) {
        LocalDateTime luc = ngay == null || !ngay.isAfter(LocalDate.now()) ? LocalDateTime.now() : ngay.atStartOfDay();
        return giaApDung(dichVuId, luc);
    }

    /**
     * Cập nhật giá: tạo bảng giá mới cho dịch vụ của gói [bangGiaGocId], giữ nguyên các bảng giá cũ.
     *
     * @param ketThuc null = áp dụng không thời hạn
     */
    @Transactional
    public BangGiaDichVu capNhatGia(Integer bangGiaGocId, BigDecimal donGia, LocalDateTime apDung,
                                    LocalDateTime ketThuc, String nguoiCapNhat) {
        BangGiaDichVu goc = bangGiaRepository.findById(bangGiaGocId)
                .orElseThrow(() -> new IllegalArgumentException("Gói giá #" + bangGiaGocId + " không còn tồn tại."));
        if (donGia == null || donGia.signum() < 0) {
            throw new IllegalArgumentException("Giá mới của " + goc.getMaBangGia() + " không hợp lệ.");
        }
        LocalDateTime tu = apDung != null ? apDung : LocalDateTime.now();
        kiemTraKhoang(tu, ketThuc, null);
        DichVu dv = goc.getDichVu();
        BigDecimal giaTruoc = dv != null ? giaApDung(dv.getId(), tu).map(BangGiaDichVu::getDonGia).orElse(goc.getDonGia()) : goc.getDonGia();

        BangGiaDichVu moi = new BangGiaDichVu();
        moi.setMaBangGia(MaSinh.tao("BG-"));
        moi.setDichVu(dv);
        moi.setKhuVuc(goc.getKhuVuc());
        moi.setLoaiHinhDat(goc.getLoaiHinhDat());
        moi.setDonViTinh(goc.getDonViTinh());
        moi.setDonGia(donGia);
        moi.setGiaCu(giaTruoc);
        moi.setNgayApDung(tu);
        moi.setNgayKetThuc(ketThuc);
        moi.setNguoiCapNhat(nguoiCapNhat != null && nguoiCapNhat.length() > 100 ? nguoiCapNhat.substring(0, 100) : nguoiCapNhat);
        moi.setTrangThai(trangThaiTheoLuc(moi, LocalDateTime.now()));
        moi = bangGiaRepository.save(moi);
        lamMoi();
        return moi;
    }

    /**
     * Kiểm tra khoảng thời gian áp dụng: bắt đầu không ở quá khứ (trừ khi giữ nguyên thời điểm cũ [apDungCu]),
     * kết thúc sau bắt đầu. Cho lệch 1 phút để chọn "bây giờ" không bị báo quá khứ.
     */
    public static void kiemTraKhoang(LocalDateTime tu, LocalDateTime den, LocalDateTime apDungCu) {
        LocalDateTime mocQuaKhu = LocalDateTime.now().minusMinutes(1);
        if (tu != null && tu.isBefore(mocQuaKhu) && !tu.equals(apDungCu)) {
            throw new IllegalArgumentException("Thời điểm áp dụng không được ở quá khứ.");
        }
        if (den != null && tu != null && !den.isAfter(tu)) {
            throw new IllegalArgumentException("Thời điểm kết thúc phải sau thời điểm áp dụng.");
        }
    }

    /**
     * Ghép ngày (yyyy-MM-dd) và giờ (HH:mm) từ form. Không có giờ: đầu ngày cho thời điểm bắt đầu,
     * 23:59:59 cho thời điểm kết thúc. Nhận cả chuỗi ngày giờ đầy đủ (yyyy-MM-ddTHH:mm[:ss]).
     *
     * @return null nếu không nhập ngày
     */
    public static LocalDateTime ghepNgayGio(String ngay, String gio, boolean laKetThuc) {
        if (ngay == null || ngay.isBlank()) return null;
        String n = ngay.trim();
        if (n.contains("T")) return LocalDateTime.parse(n);
        LocalDate d = LocalDate.parse(n);
        if (gio == null || gio.isBlank()) return laKetThuc ? d.atTime(23, 59, 59) : d.atStartOfDay();
        LocalTime t = LocalTime.parse(gio.trim());
        return d.atTime(laKetThuc && t.getSecond() == 0 && t.getMinute() == 59 && t.getHour() == 23 ? t.withSecond(59) : t);
    }

    /** Trạng thái lưu CSDL theo thời điểm: hết thời hạn -> HetHan; gói đã ngừng thủ công giữ HetHan. */
    static String trangThaiTheoLuc(BangGiaDichVu bg, LocalDateTime luc) {
        boolean daQua = bg.getNgayKetThuc() != null && bg.getNgayKetThuc().isBefore(luc);
        if (daQua) return HET_HAN;
        if (HET_HAN.equals(bg.getTrangThai())) return HET_HAN; // ngừng áp dụng thủ công
        return DANG_AP_DUNG; // còn hiệu lực, kể cả chưa tới giờ áp dụng
    }

    /** Trạng thái hiển thị: thêm "Sắp áp dụng" (ChuaApDung) cho gói còn hiệu lực nhưng chưa tới giờ áp dụng. */
    public static String hienThi(BangGiaDichVu bg, LocalDateTime luc) {
        if (HET_HAN.equals(bg.getTrangThai())) return HET_HAN;
        return bg.getNgayApDung() != null && bg.getNgayApDung().isAfter(luc) ? CHUA_AP_DUNG : DANG_AP_DUNG;
    }

    /** Trong các bảng giá còn hiệu lực tại [luc], lấy bảng có thời điểm áp dụng mới nhất (bằng nhau: tạo sau). */
    static Optional<BangGiaDichVu> chonGia(List<BangGiaDichVu> ds, LocalDateTime luc) {
        return ds.stream()
                .filter(bg -> !HET_HAN.equals(bg.getTrangThai())) // đã kết thúc hoặc bị ngừng thủ công
                .filter(bg -> bg.getNgayApDung() == null || !bg.getNgayApDung().isAfter(luc))
                .filter(bg -> bg.getNgayKetThuc() == null || !bg.getNgayKetThuc().isBefore(luc))
                .max(Comparator.comparing((BangGiaDichVu bg) -> bg.getNgayApDung() != null ? bg.getNgayApDung() : LocalDateTime.MIN)
                        .thenComparing(BangGiaDichVu::getId));
    }

    private void xoaCache() {
        for (String ten : List.of(CacheNames.DICH_VU, CacheNames.DANH_MUC_DICH_VU)) {
            Cache c = cacheManager.getCache(ten);
            if (c != null) c.clear();
        }
    }
}
