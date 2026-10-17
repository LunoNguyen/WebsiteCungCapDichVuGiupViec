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
 * Tự hủy đơn quá hạn (chạy 5 phút một lần):
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
    private final TransactionTemplate giaoDich;

    /** Bật / tắt tự hủy (application.properties: app.don.tu-huy=false để tắt). */
    @Value("${app.don.tu-huy:true}")
    private boolean bat;

    public TuHuyDonService(DonDatDichVuRepository donRepo, PhanCongCTVRepository phanCongRepo,
                           LichLamViecRepository lichRepo, LichSuTrangThaiDonRepository lichSuRepo,
                           ThongBaoRepository thongBaoRepo, ThongBaoNguoiDungRepository thongBaoNguoiDungRepo,
                           RealtimeService realtimeService, PlatformTransactionManager tm) {
        this.donRepo = donRepo;
        this.phanCongRepo = phanCongRepo;
        this.lichRepo = lichRepo;
        this.lichSuRepo = lichSuRepo;
        this.thongBaoRepo = thongBaoRepo;
        this.thongBaoNguoiDungRepo = thongBaoNguoiDungRepo;
        this.realtimeService = realtimeService;
        this.giaoDich = new TransactionTemplate(tm);
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void quetDinhKy() {
        if (bat) quet(LocalDateTime.now());
    }

    /** Hủy mọi đơn quá hạn tại thời điểm [luc]; trả về số đơn đã hủy. */
    public int quet(LocalDateTime luc) {
        List<Integer> ungVien = giaoDich.execute(t ->
                donRepo.findChuaLamDenNgay(luc.toLocalDate()).stream().map(DonDatDichVu::getId).toList());
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

        String lyDo = lyDoQuaHan(don, daCoNguoiNhan, daLamBuoiNao, luc);
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
