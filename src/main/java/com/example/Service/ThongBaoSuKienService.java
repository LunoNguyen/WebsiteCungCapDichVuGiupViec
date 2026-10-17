package com.example.Service;

import com.example.Model.*;
import com.example.Repository.*;
import com.example.SuKien.SuKienDanhGia;
import com.example.SuKien.SuKienDon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Thông báo tự động theo sự kiện đơn đặt (phát từ DonDatListener, xử lý sau khi giao dịch đã ghi xong,
 * ở luồng nền nên không làm chậm thao tác của người dùng).
 *
 * Khách hàng: đặt đơn, có cộng tác viên nhận đơn, đơn đổi trạng thái (bắt đầu làm, hoàn thành, hủy,
 *             quay lại tìm CTV), đơn đổi ngày / giờ / địa chỉ, ưu đãi mới ({@link #uuDaiMoi}).
 *             bTaskee trả lời đánh giá của khách ({@link #khiCoPhanHoiDanhGia}).
 * Cộng tác viên: được phân công / mời nhận đơn, có đơn mới trong khu vực + dịch vụ đã đăng ký,
 *             đơn đã nhận bị khách hủy (hoặc bị hủy), đơn đã nhận đổi ngày / giờ / địa chỉ.
 *
 * Thông báo riêng lưu nhóm "CaNhan" (chỉ người nhận thấy); ưu đãi gửi nhóm "KhachHang".
 */
@Service
public class ThongBaoSuKienService {

    private static final Logger log = LoggerFactory.getLogger(ThongBaoSuKienService.class);
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String NGUOI_GUI = "bTaskee";

    private final DonDatDichVuRepository donRepo;
    private final PhanCongCTVRepository phanCongRepo;
    private final KhuVucCTVRepository khuVucCtvRepo;
    private final DichVuCTVRepository dichVuCtvRepo;
    private final LichSuTrangThaiDonRepository lichSuRepo;
    private final ThongBaoService thongBaoService;

    /** Đơn chuyển khoản chưa thanh toán thì chưa báo cộng tác viên (chưa phát hành). */
    @org.springframework.beans.factory.annotation.Autowired
    private HanNhanDonService hanNhanDonService;
    @org.springframework.beans.factory.annotation.Autowired
    private DanhGiaRepository danhGiaRepo;
    @org.springframework.beans.factory.annotation.Autowired
    private PhanHoiDanhGiaRepository phanHoiDanhGiaRepo;

    public ThongBaoSuKienService(DonDatDichVuRepository donRepo, PhanCongCTVRepository phanCongRepo,
                                 KhuVucCTVRepository khuVucCtvRepo, DichVuCTVRepository dichVuCtvRepo,
                                 LichSuTrangThaiDonRepository lichSuRepo, ThongBaoService thongBaoService) {
        this.donRepo = donRepo;
        this.phanCongRepo = phanCongRepo;
        this.khuVucCtvRepo = khuVucCtvRepo;
        this.dichVuCtvRepo = dichVuCtvRepo;
        this.lichSuRepo = lichSuRepo;
        this.thongBaoService = thongBaoService;
    }

    // =========================================================================
    // ĐƠN MỚI: khách + CTV trong khu vực có đăng ký dịch vụ
    // =========================================================================

    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiCoDonMoi(SuKienDon.DonMoi e) {
        DonDatDichVu don = donRepo.findByIdWithDetails(e.donId()).orElse(null);
        if (don == null) return;
        if (!hanNhanDonService.daPhatHanh(don)) {
            guiKhach(don, "Đơn " + don.getMaDonDat() + " đang chờ thanh toán",
                    "Vui lòng chuyển khoản trong 15 phút để bTaskee bắt đầu tìm cộng tác viên.");
            return;
        }
        guiKhach(don, "Đã nhận đơn " + don.getMaDonDat(),
                "bTaskee đã nhận đơn " + moTa(don) + ". Chúng tôi đang tìm cộng tác viên phù hợp và sẽ báo ngay khi có người nhận.");
        baoCongTacVienKhuVuc(don);
    }

    /** Đơn chuyển khoản vừa thanh toán: báo khách, rồi báo CTV trong khu vực như đơn mới. */
    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiDaPhatHanh(SuKienDon.DaPhatHanh e) {
        DonDatDichVu don = donRepo.findByIdWithDetails(e.donId()).orElse(null);
        if (don == null) return;
        guiKhach(don, "Đã nhận thanh toán đơn " + don.getMaDonDat(),
                "bTaskee đã nhận thanh toán đơn " + moTa(don) + ". Chúng tôi đang tìm cộng tác viên phù hợp và sẽ báo ngay khi có người nhận.");
        baoCongTacVienKhuVuc(don);
    }

    /** Báo cộng tác viên trong khu vực của đơn, có đăng ký dịch vụ của đơn. */
    private void baoCongTacVienKhuVuc(DonDatDichVu don) {
        KhuVuc kv = don.getDiaChi() != null ? don.getDiaChi().getKhuVuc() : null;
        if (kv == null || !"ChoDuyet".equals(don.getTrangThai())) return;
        Set<Integer> dichVuCuaDon = new HashSet<>();
        if (don.getChiTietList() != null) {
            for (ChiTietDonDat ct : don.getChiTietList()) {
                if (ct.getDichVu() != null) dichVuCuaDon.add(ct.getDichVu().getId());
            }
        }
        int dem = 0;
        for (KhuVucCTV k : khuVucCtvRepo.findByKhuVuc_IdAndTrangThai(kv.getId(), "HoatDong")) {
            CongTacVien ctv = k.getCongTacVien();
            if (ctv == null || !"HoatDong".equals(ctv.getTrangThai()) || ctv.getTaiKhoan() == null
                    || PhanCongService.laChinhKhachDatDon(ctv, don)) {
                continue;
            }
            boolean lamDichVu = dichVuCtvRepo.findByCongTacVien_Id(ctv.getId()).stream()
                    .anyMatch(dv -> "HoatDong".equalsIgnoreCase(dv.getTrangThai()) && dv.getDichVu() != null
                            && dichVuCuaDon.contains(dv.getDichVu().getId()));
            if (!lamDichVu) continue;
            gui(ctv.getTaiKhoan(), "Có đơn mới tại " + kv.getTenKhuVuc() + ": " + don.getMaDonDat(),
                    "Có đơn mới trong khu vực và dịch vụ bạn đăng ký: " + moTa(don)
                            + ". Liên hệ CSKH hoặc chờ phân công để nhận đơn.");
            dem++;
        }
        log.info("Đơn mới {}: đã báo {} cộng tác viên ở {}", don.getMaDonDat(), dem, kv.getTenKhuVuc());
    }

    // =========================================================================
    // ĐỔI TRẠNG THÁI ĐƠN
    // =========================================================================

    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiDonDoiTrangThai(SuKienDon.DoiTrangThai e) {
        DonDatDichVu don = donRepo.findByIdWithDetails(e.donId()).orElse(null);
        if (don == null) return;
        String ma = don.getMaDonDat();
        switch (e.moi()) {
            // DaXacNhan: báo ở sự kiện phân công (biết tên cộng tác viên nhận đơn)
            case "ChoDuyet" -> {
                if ("DaXacNhan".equals(e.cu())) {
                    guiKhach(don, "Đơn " + ma + " đang tìm cộng tác viên khác",
                            "Cộng tác viên trước đã không thể nhận đơn " + moTa(don)
                                    + ". bTaskee đang tìm người khác và sẽ báo ngay khi có người nhận.");
                }
            }
            case "DangThucHien" -> guiKhach(don, "Đơn " + ma + " đang được thực hiện",
                    "Cộng tác viên đã bắt đầu làm đơn " + moTa(don) + ".");
            case "HoanThanh" -> guiKhach(don, "Đơn " + ma + " đã hoàn thành",
                    "Đơn " + moTa(don) + " đã hoàn thành. Mời bạn đánh giá để bTaskee phục vụ tốt hơn.");
            case "DaHuy" -> khiHuy(don, e.cu());
            default -> { }
        }
    }

    private void khiHuy(DonDatDichVu don, String trangThaiCu) {
        Optional<LichSuTrangThaiDon> ls = lichSuRepo
                .findFirstByDonDat_IdAndTrangThaiMoiOrderByThoiGianDescIdDesc(don.getId(), "DaHuy");
        String nguoi = ls.map(LichSuTrangThaiDon::getNguoiThucHien).orElse("");
        String lyDo = ls.map(LichSuTrangThaiDon::getGhiChu).filter(s -> !s.isBlank()).orElse(null);
        boolean khachHuy = nguoi.startsWith("KhachHang");
        String ma = don.getMaDonDat();

        guiKhach(don, "Đơn " + ma + " đã hủy",
                khachHuy ? "Bạn đã hủy đơn " + moTa(don) + "."
                         : "Đơn " + moTa(don) + " đã bị hủy" + (lyDo != null ? ". Lý do: " + lyDo : "")
                           + ". Nếu đã thanh toán, bTaskee sẽ liên hệ hoàn tiền.");

        // Cộng tác viên đã nhận / đang được mời nhận đơn
        for (PhanCongCTV pc : phanCongRepo.findAllByDonDat_Id(don.getId())) {
            boolean daNhan = "DaXacNhan".equals(pc.getTrangThai());
            boolean dangMoi = "ChoPhanCong".equals(pc.getTrangThai())
                    || ("TuChoi".equals(pc.getTrangThai()) && pc.getLyDoTuChoi() != null
                        && List.of("Đơn tự hủy do quá hạn", "Khách hàng đã hủy đơn").contains(pc.getLyDoTuChoi()));
            if ((!daNhan && !dangMoi) || pc.getCongTacVien() == null || pc.getCongTacVien().getTaiKhoan() == null) continue;
            gui(pc.getCongTacVien().getTaiKhoan(), "Đơn " + ma + " đã bị hủy",
                    (khachHuy ? "Khách hàng đã hủy đơn " : "Đơn ") + moTa(don)
                            + (khachHuy ? "" : " đã bị hủy" + (lyDo != null ? " (" + lyDo + ")" : ""))
                            + ". Lịch làm việc của đơn này đã được gỡ khỏi lịch của bạn.");
        }
    }

    // =========================================================================
    // ĐỔI NGÀY / GIỜ / ĐỊA CHỈ
    // =========================================================================

    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiDonDoiThongTin(SuKienDon.DoiThongTin e) {
        DonDatDichVu don = donRepo.findByIdWithDetails(e.donId()).orElse(null);
        if (don == null) return;
        String noiDung = "Đơn " + don.getMaDonDat() + " đã thay đổi " + e.moTa() + ". Thông tin hiện tại: " + moTa(don) + ".";
        guiKhach(don, "Đơn " + don.getMaDonDat() + " có thay đổi", noiDung);
        for (PhanCongCTV pc : phanCongRepo.findAllByDonDat_Id(don.getId())) {
            if ("DaXacNhan".equals(pc.getTrangThai()) && pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
                gui(pc.getCongTacVien().getTaiKhoan(), "Đơn " + don.getMaDonDat() + " đổi lịch", noiDung);
            }
        }
    }

    // =========================================================================
    // PHÂN CÔNG
    // =========================================================================

    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiPhanCongMoi(SuKienDon.PhanCongMoi e) {
        PhanCongCTV pc = phanCongRepo.findById(e.phanCongId()).orElse(null);
        if (pc == null || pc.getDonDat() == null) return;
        DonDatDichVu don = donRepo.findByIdWithDetails(pc.getDonDat().getId()).orElse(null);
        if (don == null) return;
        CongTacVien ctv = pc.getCongTacVien();
        if (ctv != null && ctv.getTaiKhoan() != null) {
            boolean choXacNhan = "ChoPhanCong".equals(pc.getTrangThai());
            gui(ctv.getTaiKhoan(), "Bạn được phân công đơn " + don.getMaDonDat(),
                    "Bạn được phân công làm đơn " + moTa(don)
                            + (choXacNhan ? ". Vui lòng vào mục Đơn hàng để xác nhận nhận đơn." : ". Lịch làm việc đã được thêm vào lịch của bạn."));
        }
        if ("DaXacNhan".equals(pc.getTrangThai())) baoKhachCoNguoiNhan(don, ctv);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiPhanCongDoiTrangThai(SuKienDon.PhanCongDoiTrangThai e) {
        if (!"DaXacNhan".equals(e.moi())) return;
        PhanCongCTV pc = phanCongRepo.findById(e.phanCongId()).orElse(null);
        if (pc == null || pc.getDonDat() == null) return;
        donRepo.findByIdWithDetails(pc.getDonDat().getId()).ifPresent(don -> baoKhachCoNguoiNhan(don, pc.getCongTacVien()));
    }

    private void baoKhachCoNguoiNhan(DonDatDichVu don, CongTacVien ctv) {
        String ten = ctv != null ? ctv.getHoTen() : "Cộng tác viên";
        guiKhach(don, "Đơn " + don.getMaDonDat() + " – Đã có cộng tác viên tiếp nhận",
                "Cộng tác viên " + ten + (ctv != null && ctv.getSoDienThoai() != null ? " (" + ctv.getSoDienThoai() + ")" : "")
                        + " đã nhận đơn " + moTa(don) + ".");
    }

    // =========================================================================
    // ƯU ĐÃI MỚI (gửi mọi khách hàng)
    // =========================================================================

    /** Gửi thông báo ưu đãi / khuyến mãi tới toàn bộ khách hàng. */
    @Transactional
    public void uuDaiMoi(String tieuDe, String noiDung) {
        thongBaoService.gui(tieuDe, noiDung, "KhachHang", null, "Phòng Marketing");
    }

    // =========================================================================
    // PHẢN HỒI ĐÁNH GIÁ: báo khách khi bTaskee trả lời
    // =========================================================================

    @Async("taskExecutor")
    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiCoPhanHoiDanhGia(SuKienDanhGia.PhanHoiMoi e) {
        DanhGia dg = danhGiaRepo.findById(e.danhGiaId()).orElse(null);
        if (dg == null || dg.getDonDat() == null) return;
        PhanHoiDanhGia ph = DanhGiaService.phanHoiMoiNhatCuaNhanVien(
                phanHoiDanhGiaRepo.findByDanhGia_IdOrderByThoiGianAscIdAsc(dg.getId()));
        if (ph == null) return;
        DonDatDichVu don = dg.getDonDat();
        guiKhach(don, "bTaskee đã phản hồi đánh giá đơn " + don.getMaDonDat(), noiDungPhanHoiDanhGia(ph.getNoiDung()));
    }

    /** Trích phản hồi (tối đa 200 ký tự) kèm lời nhắc xem trong đơn. */
    static String noiDungPhanHoiDanhGia(String phanHoi) {
        String t = phanHoi == null ? "" : phanHoi.trim();
        if (t.length() > 200) t = t.substring(0, 200) + "…";
        return t + " Xem chi tiết trong đơn hàng của bạn.";
    }

    // =========================================================================
    // NỘI BỘ
    // =========================================================================

    private void guiKhach(DonDatDichVu don, String tieuDe, String noiDung) {
        if (don.getKhachHang() != null && don.getKhachHang().getTaiKhoan() != null) {
            gui(don.getKhachHang().getTaiKhoan(), tieuDe, noiDung);
        }
    }

    private void gui(TaiKhoan tk, String tieuDe, String noiDung) {
        try {
            thongBaoService.gui(tieuDe.length() > 200 ? tieuDe.substring(0, 200) : tieuDe, noiDung, "CaNhan", tk.getId(), NGUOI_GUI);
        } catch (RuntimeException ex) {
            log.warn("Không gửi được thông báo '{}' tới tài khoản {}: {}", tieuDe, tk.getId(), ex.getMessage());
        }
    }

    /** "DD-xxx (Dọn dẹp nhà 3 giờ, 09:00 ngày 12/10/2026)". */
    private static String moTa(DonDatDichVu don) {
        StringBuilder sb = new StringBuilder(don.getMaDonDat() != null ? don.getMaDonDat() : "");
        List<String> phan = new ArrayList<>();
        if (don.getChiTietList() != null && !don.getChiTietList().isEmpty() && don.getChiTietList().get(0).getDichVu() != null) {
            phan.add(don.getChiTietList().get(0).getDichVu().getTenDichVu());
        }
        if (don.getGioBatDau() != null && don.getNgayThucHien() != null) {
            phan.add(don.getGioBatDau().format(GIO) + " ngày " + don.getNgayThucHien().format(NGAY));
        }
        if (!phan.isEmpty()) sb.append(" (").append(String.join(", ", phan)).append(")");
        return sb.toString();
    }
}
