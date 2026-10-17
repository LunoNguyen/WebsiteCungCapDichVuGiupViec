package com.example.Service;

import com.example.Model.*;
import com.example.Repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Tự hủy đơn quá hạn (chạy mỗi phút):
 * - Đơn CHƯA có cộng tác viên nhận: hết thời gian nhận đơn (HanNhanDonService, mặc định 15 phút kể từ lúc
 *   đơn được phát hành cho CTV) thì hủy. Trong thời gian đó CTV tự nhận đơn hoặc nhân viên phân công.
 * - Đơn CHƯA có cộng tác viên nhận (chờ duyệt, hoặc lời mời chưa ai xác nhận): quá giờ bắt đầu thì hủy.
 * - Đơn ĐÃ có cộng tác viên nhận nhưng chưa bắt đầu làm (vẫn "Đã xác nhận", chưa buổi nào được làm):
 *   quá giờ kết thúc (gói tháng: giờ kết thúc buổi cuối) thì hủy.
 * Đơn đang thực hiện / đã hoàn thành không bị đụng tới.
 *
 * Khi hủy: đơn → DaHuy (ghi lịch sử, người thực hiện "Hệ thống"), lịch làm việc chưa làm → Huy (giải phóng lịch CTV),
 * lời mời đang chờ → TuChoi kèm lý do, gửi thông báo cho khách hàng và cộng tác viên đã nhận.
 * Hóa đơn (nếu có) giữ nguyên để CSKH xử lý hoàn tiền thủ công khi khách đã thanh toán.
 */
@Service
public class TuHuyDonService {

    private static final Logger log = LoggerFactory.getLogger(TuHuyDonService.class);
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    public static final String NGUOI_THUC_HIEN = "Hệ thống (tự hủy đơn quá hạn)";

    private final DonDatDichVuRepository donRepo;
    private final PhanCongCTVRepository phanCongRepo;
    private final LichLamViecRepository lichRepo;
    private final LichSuTrangThaiDonRepository lichSuRepo;
    private final ThongBaoRepository thongBaoRepo;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepo;
    private final RealtimeService realtimeService;
    private final HanNhanDonService hanNhanDon;
    private final TransactionTemplate giaoDich;

    /** Bật / tắt tự hủy (application.properties: app.don.tu-huy=false để tắt). */
    @Value("${app.don.tu-huy:true}")
    private boolean bat;

    /** Hạn thanh toán chuyển khoản (phút, tính từ lúc tạo đơn): quá hạn chưa trả thì hủy. */
    @Value("${app.don.han-thanh-toan-phut:15}")
    private long hanThanhToanPhut;

    @org.springframework.beans.factory.annotation.Autowired
    private HoaDonRepository hoaDonRepo;

    public TuHuyDonService(DonDatDichVuRepository donRepo, PhanCongCTVRepository phanCongRepo,
                           LichLamViecRepository lichRepo, LichSuTrangThaiDonRepository lichSuRepo,
                           ThongBaoRepository thongBaoRepo, ThongBaoNguoiDungRepository thongBaoNguoiDungRepo,
                           RealtimeService realtimeService, HanNhanDonService hanNhanDon,
                           PlatformTransactionManager tm) {
        this.donRepo = donRepo;
        this.phanCongRepo = phanCongRepo;
        this.lichRepo = lichRepo;
        this.lichSuRepo = lichSuRepo;
        this.thongBaoRepo = thongBaoRepo;
        this.thongBaoNguoiDungRepo = thongBaoNguoiDungRepo;
        this.realtimeService = realtimeService;
        this.hanNhanDon = hanNhanDon;
        this.giaoDich = new TransactionTemplate(tm);
    }

    // Mỗi phút: đơn hết 15 phút chờ nhận được hủy đúng hạn (trễ tối đa 1 phút)
    @Scheduled(cron = "0 * * * * *")
    public void quetDinhKy() {
        if (bat) quet(LocalDateTime.now());
    }

    /** Hủy mọi đơn quá hạn tại thời điểm [luc]; trả về số đơn đã hủy. */
    public int quet(LocalDateTime luc) {
        // Đơn tới ngày làm (quá giờ) + mọi đơn đang chờ nhận (hết thời gian nhận đơn)
        List<Integer> ungVien = giaoDich.execute(t -> {
            java.util.Set<Integer> ids = new java.util.LinkedHashSet<>();
            donRepo.findChuaLamDenNgay(luc.toLocalDate()).forEach(d -> ids.add(d.getId()));
            donRepo.findByTrangThaiOrderByNgayTaoDesc("ChoDuyet").forEach(d -> ids.add(d.getId()));
            return new java.util.ArrayList<>(ids);
        });
        int daHuy = 0;
        for (Integer id : ungVien == null ? List.<Integer>of() : ungVien) {
            try {
                // Mỗi đơn một giao dịch và khóa đơn: không đụng độ với CTV đang bấm nhận đơn
                Boolean huy = giaoDich.execute(t -> xuLyMotDon(id, luc));
                if (Boolean.TRUE.equals(huy)) daHuy++;
            } catch (Exception e) {
                log.warn("Không tự hủy được đơn {}: {}", id, e.getMessage());
            }
        }
        if (daHuy > 0) {
            log.info("Đã tự hủy {} đơn quá hạn", daHuy);
            realtimeService.phatSuKien("/cskh/don-dat-dich-vu");
        }
        return daHuy;
    }

    private boolean xuLyMotDon(Integer id, LocalDateTime luc) {
        DonDatDichVu don = donRepo.findByIdForUpdate(id).orElse(null);
        if (don == null || !("ChoDuyet".equals(don.getTrangThai()) || "DaXacNhan".equals(don.getTrangThai()))) {
            return false;
        }
        List<PhanCongCTV> phanCongs = phanCongRepo.findAllByDonDat_Id(id);
        List<LichLamViec> lichs = lichRepo.findByPhanCong_DonDat_Id(id);
        boolean daCoNguoiNhan = phanCongs.stream()
                .anyMatch(pc -> "DaXacNhan".equals(pc.getTrangThai()) || "HoanThanh".equals(pc.getTrangThai()));
        boolean daLamBuoiNao = lichs.stream()
                .anyMatch(l -> "DangThucHien".equals(l.getTrangThai()) || "HoanThanh".equals(l.getTrangThai()));

        // Chưa có người nhận: quá hạn thanh toán chuyển khoản → hết 15 phút nhận đơn → quá giờ bắt đầu
        HoaDon hd = hoaDonRepo.findByDonDat_Id(id).orElse(null);
        String lyDo = daCoNguoiNhan || daLamBuoiNao || hd == null ? null
                : lyDoQuaHanThanhToan(hd.getHinhThucThanhToan(), hd.getTrangThaiThanhToan(), don.getNgayTao(), luc,
                        hanThanhToanPhut);
        if (lyDo == null) lyDo = lyDoHetHanNhan(hanNhanDon.hanNhan(don), daCoNguoiNhan, daLamBuoiNao, luc,
                hanNhanDon.soPhut());
        if (lyDo == null) lyDo = lyDoQuaHan(don, daCoNguoiNhan, daLamBuoiNao, luc);
        if (lyDo == null) return false;

        String trangThaiCu = don.getTrangThai();
        don.setTrangThai("DaHuy");
        donRepo.save(don);
        lichSuRepo.save(LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu(trangThaiCu)
                .trangThaiMoi("DaHuy")
                .nguoiThucHien(NGUOI_THUC_HIEN)
                .thoiGian(luc)
                .ghiChu(lyDo)
                .build());

        for (LichLamViec l : lichs) {
            if ("SapToi".equals(l.getTrangThai())) {
                l.setTrangThai("Huy");
                lichRepo.save(l);
            }
        }
        for (PhanCongCTV pc : phanCongs) {
            if ("ChoPhanCong".equals(pc.getTrangThai())) {
                pc.setTrangThai("TuChoi");
                pc.setLyDoTuChoi("Đơn tự hủy do quá hạn");
                phanCongRepo.save(pc);
            }
        }

        // Thông báo khách hàng và cộng tác viên: ThongBaoSuKienService (sự kiện đơn chuyển DaHuy)
        String ma = don.getMaDonDat();
        log.info("Tự hủy đơn {}: {}", ma, lyDo);
        return true;
    }

    /**
     * Lý do hủy vì đơn chuyển khoản quá hạn thanh toán (tính từ lúc tạo đơn); null nếu không phải
     * chuyển khoản, đã thanh toán hoặc còn hạn.
     */
    public static String lyDoQuaHanThanhToan(String hinhThuc, String trangThaiThanhToan, LocalDateTime ngayTao,
                                             LocalDateTime luc, long soPhut) {
        if (!"ChuyenKhoan".equalsIgnoreCase(hinhThuc) || !"ChuaThanhToan".equalsIgnoreCase(trangThaiThanhToan)
                || ngayTao == null) return null;
        LocalDateTime han = ngayTao.plusMinutes(soPhut);
        if (!luc.isAfter(han)) return null;
        return "Quá " + soPhut + " phút chưa thanh toán chuyển khoản (hạn " + han.format(GIO_NGAY) + ")";
    }

    /**
     * Lý do hủy vì hết thời gian nhận đơn; null nếu đã có người nhận, đã làm hoặc còn hạn.
     *
     * @param han hạn nhận đơn (HanNhanDonService.hanNhan; null = chưa phát hành)
     */
    public static String lyDoHetHanNhan(LocalDateTime han, boolean daCoNguoiNhan, boolean daLamBuoiNao,
                                        LocalDateTime luc, long soPhut) {
        if (han == null || daCoNguoiNhan || daLamBuoiNao || !luc.isAfter(han)) return null;
        return "Hết " + soPhut + " phút chờ (hạn " + han.format(GIO_NGAY) + ") nhưng chưa có cộng tác viên nhận đơn";
    }

    /**
     * Lý do đơn bị coi là quá hạn tại thời điểm [luc]; null nếu chưa quá hạn.
     *
     * @param daCoNguoiNhan có cộng tác viên đã xác nhận nhận đơn
     * @param daLamBuoiNao  đã có buổi làm bắt đầu / hoàn thành
     */
    public static String lyDoQuaHan(DonDatDichVu don, boolean daCoNguoiNhan, boolean daLamBuoiNao, LocalDateTime luc) {
        if (don.getNgayThucHien() == null || don.getGioBatDau() == null || daLamBuoiNao) return null;
        if (daCoNguoiNhan) {
            // Đơn đã có CTV nhận được xử lý qua chức năng "Xử lý quá hạn" của CTV, không tự động hủy ngầm
            return null;
        }
        LocalDateTime batDau = don.getNgayThucHien().atTime(don.getGioBatDau());
        return luc.isAfter(batDau)
                ? "Quá giờ bắt đầu (" + batDau.format(GIO_NGAY) + ") nhưng chưa có cộng tác viên nhận đơn"
                : null;
    }
}
