package com.example.Service;

import com.example.Model.*;
import com.example.Repository.*;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CSKH đổi trạng thái / xóa đơn trên trang Quản lý đơn đặt, và tự hoàn thành đơn đang làm đã quá giờ kết thúc.
 *
 * Đổi trạng thái chỉ theo luồng hợp lệ; khi đơn đổi trạng thái, DonDatListener phát sự kiện nên
 * hoàn lượt khuyến mãi (đơn hủy), thông báo khách / CTV và cập nhật realtime chạy như khi khách hủy hay CTV làm xong.
 * Xóa hẳn chỉ dành cho đơn chưa phát sinh gì (chưa thanh toán, chưa phân công, chưa đánh giá / khiếu nại).
 */
@Service
public class QuanLyDonService {

    private static final Logger log = LoggerFactory.getLogger(QuanLyDonService.class);

    static final Map<String, String> TEN = Map.of(
            "ChoDuyet", "Chờ duyệt",
            "DaXacNhan", "Đã xác nhận",
            "DangThucHien", "Đang thực hiện",
            "HoanThanh", "Hoàn thành",
            "DaHuy", "Đã hủy");

    private static final Map<String, Set<String>> CHUYEN = Map.of(
            "ChoDuyet", Set.of("DangThucHien", "DaHuy"),
            "DaXacNhan", Set.of("DangThucHien", "DaHuy"),
            "DangThucHien", Set.of("HoanThanh", "DaHuy"));

    /** Đơn đang làm quá giờ kết thúc buổi cuối bao lâu thì tự hoàn thành. */
    static final long GIO_TU_HOAN_THANH = 24;

    private final DonDatDichVuRepository donRepo;
    private final PhanCongCTVRepository phanCongRepo;
    private final LichLamViecRepository lichRepo;
    private final LichSuTrangThaiDonRepository lichSuRepo;
    private final HoaDonRepository hoaDonRepo;
    private final DanhGiaRepository danhGiaRepo;
    private final KhieuNaiRepository khieuNaiRepo;
    private final LichSuSuDungKhuyenMaiRepository lichSuKmRepo;
    private final MaKhuyenMaiRepository maKhuyenMaiRepo;
    private final RealtimeService realtimeService;
    private final EntityManager em;
    private final TransactionTemplate giaoDich;

    @Value("${app.don.tu-huy:true}")
    private boolean bat;

    public QuanLyDonService(DonDatDichVuRepository donRepo, PhanCongCTVRepository phanCongRepo,
                            LichLamViecRepository lichRepo, LichSuTrangThaiDonRepository lichSuRepo,
                            HoaDonRepository hoaDonRepo, DanhGiaRepository danhGiaRepo,
                            KhieuNaiRepository khieuNaiRepo, LichSuSuDungKhuyenMaiRepository lichSuKmRepo,
                            MaKhuyenMaiRepository maKhuyenMaiRepo, RealtimeService realtimeService,
                            EntityManager em, PlatformTransactionManager tm) {
        this.donRepo = donRepo;
        this.phanCongRepo = phanCongRepo;
        this.lichRepo = lichRepo;
        this.lichSuRepo = lichSuRepo;
        this.hoaDonRepo = hoaDonRepo;
        this.danhGiaRepo = danhGiaRepo;
        this.khieuNaiRepo = khieuNaiRepo;
        this.lichSuKmRepo = lichSuKmRepo;
        this.maKhuyenMaiRepo = maKhuyenMaiRepo;
        this.realtimeService = realtimeService;
        this.em = em;
        this.giaoDich = new TransactionTemplate(tm);
    }

    // ───────────── Quy tắc (thuần, có unit test) ─────────────

    /** Câu lỗi khi không được chuyển [cu] → [moi]; null nếu hợp lệ. */
    public static String loiChuyenTrangThai(String cu, String moi) {
        if (moi == null || !TEN.containsKey(moi)) return "Trạng thái không hợp lệ.";
        if (moi.equals(cu)) return "Đơn đã ở trạng thái này.";
        if ("HoanThanh".equals(cu) || "DaHuy".equals(cu)) {
            return "Đơn đã kết thúc (" + TEN.get(cu) + "), không đổi trạng thái được nữa.";
        }
        Set<String> duoc = CHUYEN.get(cu);
        if (duoc == null || !duoc.contains(moi)) {
            return "Không chuyển được từ " + TEN.getOrDefault(cu, String.valueOf(cu)) + " sang " + TEN.get(moi) + ".";
        }
        return null;
    }

    /** Lý do không xóa hẳn được đơn; null nếu xóa được. */
    public static String lyDoKhongXoa(String trangThai, boolean daThanhToan, boolean coPhanCong, boolean coLienQuan) {
        if (!"ChoDuyet".equals(trangThai) && !"DaHuy".equals(trangThai)) {
            return "Chỉ xóa được đơn Chờ duyệt hoặc Đã hủy. Đơn khác hãy chuyển sang Đã hủy.";
        }
        if (daThanhToan) return "Đơn đã thanh toán, không xóa được. Hãy chuyển sang Đã hủy.";
        if (coPhanCong) return "Đơn đã có phân công cộng tác viên, không xóa được. Hãy chuyển sang Đã hủy.";
        if (coLienQuan) return "Đơn đã có đánh giá, khiếu nại hoặc biên bản, không xóa được.";
        return null;
    }

    /** Đơn đang làm đã quá giờ kết thúc buổi cuối ít nhất 24 giờ. */
    public static boolean quaHanHoanThanh(DonDatDichVu don, LocalDateTime luc) {
        if (don.getNgayThucHien() == null || don.getGioBatDau() == null) return false;
        List<LocalDate> buoi = PhanCongService.cacBuoi(don);
        LocalDateTime ketThuc = buoi.get(buoi.size() - 1).atTime(PhanCongService.gioKetThuc(don));
        return luc.isAfter(ketThuc.plusHours(GIO_TU_HOAN_THANH));
    }

    // ───────────── Thao tác của CSKH ─────────────

    @Transactional
    public void doiTrangThai(Integer donId, String moi, String ghiChu, String nguoi) {
        DonDatDichVu don = donRepo.findByIdForUpdate(donId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        String loi = loiChuyenTrangThai(don.getTrangThai(), moi);
        if (loi != null) throw new IllegalArgumentException(loi);

        List<PhanCongCTV> phanCongs = phanCongRepo.findAllByDonDat_Id(donId);
        if ("DangThucHien".equals(moi) && phanCongs.stream().noneMatch(pc -> "DaXacNhan".equals(pc.getTrangThai()))) {
            throw new IllegalArgumentException("Đơn chưa có cộng tác viên nhận, chưa chuyển sang Đang thực hiện được.");
        }
        String lyDo = ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
        if ("DaHuy".equals(moi) && lyDo == null) {
            throw new IllegalArgumentException("Vui lòng nhập lý do hủy đơn.");
        }
        apDung(don, phanCongs, moi, nguoi, lyDo != null ? lyDo : "CSKH chuyển sang " + TEN.get(moi), LocalDateTime.now());
        realtimeService.phatSuKien("/cskh/don-dat-dich-vu");
    }

    /** Ghi chú nội bộ của đơn (ô Ghi chú ở cửa sổ chỉnh sửa). */
    @Transactional
    public void capNhatGhiChu(Integer donId, String ghiChu) {
        DonDatDichVu don = donRepo.findById(donId).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        String moi = ghiChu == null || ghiChu.isBlank() ? null : ghiChu.trim();
        if (moi != null && moi.length() > 500) throw new IllegalArgumentException("Ghi chú tối đa 500 ký tự.");
        don.setGhiChu(moi);
        donRepo.save(don);
    }

    /** Xóa hẳn đơn chưa phát sinh; trả mã đơn đã xóa. */
    @Transactional
    public String xoaDon(Integer donId) {
        DonDatDichVu don = donRepo.findByIdForUpdate(donId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        HoaDon hd = hoaDonRepo.findByDonDat_Id(donId).orElse(null);
        boolean coBienLai = hd != null && dem("select count(b) from BienLai b where b.hoaDon.id = :id", hd.getId()) > 0;
        boolean daThanhToan = hd != null && "DaThanhToan".equals(hd.getTrangThaiThanhToan()) || coBienLai;
        boolean coPhanCong = !phanCongRepo.findAllByDonDat_Id(donId).isEmpty();
        boolean coLienQuan = danhGiaRepo.findByDonDat_Id(donId).isPresent()
                || khieuNaiRepo.findByDonDat_Id(donId).isPresent()
                || dem("select count(b) from BieuMauTaiSan b where b.donDat.id = :id", donId) > 0;
        String loi = lyDoKhongXoa(don.getTrangThai(), daThanhToan, coPhanCong, coLienQuan);
        if (loi != null) throw new IllegalArgumentException(loi);

        // Đơn chưa hủy mà xóa: trả lại lượt mã khuyến mãi đã dùng (đơn đã hủy thì đã được trả lúc hủy)
        for (LichSuSuDungKhuyenMai ls : lichSuKmRepo.findByDonDat_Id(donId)) {
            if (!"DaHuy".equals(don.getTrangThai()) && ls.getKhuyenMai() != null) {
                maKhuyenMaiRepo.hoanLuotDung(ls.getKhuyenMai().getId());
            }
            lichSuKmRepo.delete(ls);
        }
        if (hd != null) {
            em.createQuery("delete from ChiTietHoaDon c where c.hoaDon.id = :id").setParameter("id", hd.getId()).executeUpdate();
            hoaDonRepo.delete(hd);
        }
        em.createQuery("delete from LichSuTrangThaiDon l where l.donDat.id = :id").setParameter("id", donId).executeUpdate();
        String ma = don.getMaDonDat();
        donRepo.delete(don); // ChiTietDonDat xóa theo (cascade)
        log.info("CSKH xóa đơn {}", ma);
        realtimeService.phatSuKien("/cskh/don-dat-dich-vu");
        return ma;
    }

    // ───────────── Tự hoàn thành đơn quá hạn ─────────────

    // 5 phút một lần: đơn Đang thực hiện đã quá giờ kết thúc 24 giờ → Hoàn thành
    @Scheduled(cron = "0 */5 * * * *")
    public void quetDinhKy() {
        if (bat) tuHoanThanh(LocalDateTime.now());
    }

    public int tuHoanThanh(LocalDateTime luc) {
        List<Integer> ids = giaoDich.execute(t -> donRepo.findByTrangThaiOrderByNgayTaoDesc("DangThucHien").stream()
                .map(DonDatDichVu::getId).toList());
        int dem = 0;
        for (Integer id : ids == null ? List.<Integer>of() : ids) {
            try {
                Boolean xong = giaoDich.execute(t -> {
                    DonDatDichVu don = donRepo.findByIdForUpdate(id).orElse(null);
                    if (don == null || !"DangThucHien".equals(don.getTrangThai()) || !quaHanHoanThanh(don, luc)) return false;
                    apDung(don, phanCongRepo.findAllByDonDat_Id(id), "HoanThanh", "Hệ thống",
                            "Tự hoàn thành: quá giờ kết thúc " + GIO_TU_HOAN_THANH + " giờ", luc);
                    return true;
                });
                if (Boolean.TRUE.equals(xong)) dem++;
            } catch (Exception e) {
                log.warn("Không tự hoàn thành được đơn {}: {}", id, e.getMessage());
            }
        }
        if (dem > 0) {
            log.info("Đã tự hoàn thành {} đơn quá hạn", dem);
            realtimeService.phatSuKien("/cskh/don-dat-dich-vu");
        }
        return dem;
    }

    // ───────────── Dùng chung ─────────────

    /** Đổi trạng thái đơn cùng phân công / lịch làm việc, ghi lịch sử. */
    private void apDung(DonDatDichVu don, List<PhanCongCTV> phanCongs, String moi, String nguoi, String ghiChu,
                        LocalDateTime luc) {
        String cu = don.getTrangThai();
        List<LichLamViec> lichs = lichRepo.findByPhanCong_DonDat_Id(don.getId());
        switch (moi) {
            case "DangThucHien" -> lichs.stream().filter(l -> "SapToi".equals(l.getTrangThai()))
                    .forEach(l -> l.setTrangThai("DangThucHien"));
            case "HoanThanh" -> {
                lichs.stream().filter(l -> !"Huy".equals(l.getTrangThai())).forEach(l -> l.setTrangThai("HoanThanh"));
                phanCongs.stream().filter(pc -> "DaXacNhan".equals(pc.getTrangThai()))
                        .forEach(pc -> pc.setTrangThai("HoanThanh"));
            }
            case "DaHuy" -> {
                lichs.stream().filter(l -> "SapToi".equals(l.getTrangThai()) || "DangThucHien".equals(l.getTrangThai()))
                        .forEach(l -> l.setTrangThai("Huy"));
                phanCongs.stream().filter(pc -> "ChoPhanCong".equals(pc.getTrangThai())).forEach(pc -> {
                    pc.setTrangThai("TuChoi");
                    pc.setLyDoTuChoi("Đơn đã bị hủy");
                });
            }
            default -> { }
        }
        lichRepo.saveAll(lichs);
        phanCongRepo.saveAll(phanCongs);
        don.setTrangThai(moi);
        donRepo.save(don);
        lichSuRepo.save(LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu(cu)
                .trangThaiMoi(moi)
                .nguoiThucHien(nguoi)
                .thoiGian(luc)
                .ghiChu(ghiChu)
                .build());
    }

    private long dem(String jpql, Integer id) {
        return em.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
    }
}
