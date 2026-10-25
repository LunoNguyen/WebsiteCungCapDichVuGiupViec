package com.example.Service;

import com.example.Model.DonDatDichVu;
import com.example.Model.HoaDon;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.HoaDonRepository;
import com.example.SuKien.SuKienDon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;

/**
 * Mọi thay đổi đơn đặt / phân công (kể cả từ tác vụ nền như tự hủy đơn quá hạn) đều báo realtime
 * cho các trang web đang mở, không chỉ thao tác qua HTTP mà CacheInvalidationInterceptor bắt được.
 *
 * Riêng nhân viên CSKH còn nhận sự kiện có nội dung khi có đơn mới ("don-moi") và khi đơn chuyển khoản
 * vừa được thanh toán ("da-thanh-toan"): realtime.js hiện thẻ thông báo, phát tiếng và thông báo của hệ điều hành.
 */
@Service
public class RealtimeSuKienDonService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeSuKienDonService.class);
    private static final Set<String> CSKH = Set.of("ROLE_CSKH");

    private final RealtimeService realtimeService;
    private final DonDatDichVuRepository donRepo;
    private final HoaDonRepository hoaDonRepo;
    private final TransactionTemplate docDon;

    public RealtimeSuKienDonService(RealtimeService realtimeService, DonDatDichVuRepository donRepo,
                                    HoaDonRepository hoaDonRepo, PlatformTransactionManager tm) {
        this.realtimeService = realtimeService;
        this.donRepo = donRepo;
        this.hoaDonRepo = hoaDonRepo;
        this.docDon = new TransactionTemplate(tm);
        this.docDon.setReadOnly(true);
        this.docDon.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Thông báo cho CSKH: {tên sự kiện, dòng ghi chú}; null nếu không cần báo.
     *
     * @param donMoi      đơn vừa tạo (false: đơn chuyển khoản vừa được thanh toán)
     * @param hinhThuc    TienMat | ChuyenKhoan (null coi như tiền mặt)
     * @param daThanhToan hóa đơn đã thanh toán
     */
    public static String[] thongBaoCskh(boolean donMoi, String hinhThuc, boolean daThanhToan) {
        boolean chuyenKhoan = "ChuyenKhoan".equalsIgnoreCase(hinhThuc);
        if (donMoi) {
            return new String[]{"don-moi", chuyenKhoan
                    ? "Chuyển khoản – chờ khách thanh toán"
                    : "Tiền mặt – đang tìm cộng tác viên"};
        }
        if (chuyenKhoan && daThanhToan) {
            return new String[]{"da-thanh-toan", "Đã chuyển khoản – đang tìm cộng tác viên"};
        }
        return null;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiDonThayDoi(SuKienDon.DonMoi e) {
        bao();
        baoCskh(e.donId(), true);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiDaPhatHanh(SuKienDon.DaPhatHanh e) {
        bao();
        baoCskh(e.donId(), false);
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiDoiTrangThai(SuKienDon.DoiTrangThai e) { bao(); }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiDoiThongTin(SuKienDon.DoiThongTin e) { bao(); }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiPhanCongMoi(SuKienDon.PhanCongMoi e) { bao(); }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiPhanCongDoiTrangThai(SuKienDon.PhanCongDoiTrangThai e) { bao(); }

    private void bao() {
        realtimeService.phatSuKien("/don-dat-dich-vu");
    }

    /** Đọc lại đơn sau khi lưu xong (chi tiết, hóa đơn) rồi gửi thẻ thông báo cho các trang CSKH đang mở. */
    private void baoCskh(Integer donId, boolean donMoi) {
        if (donId == null) return;
        try {
            String json = docDon.execute(t -> {
                DonDatDichVu don = donRepo.findById(donId).orElse(null);
                if (don == null) return null;
                HoaDon hd = hoaDonRepo.findByDonDat_Id(donId).orElse(null);
                String hinhThuc = hd != null ? hd.getHinhThucThanhToan() : null;
                boolean daTra = hd != null && "DaThanhToan".equals(hd.getTrangThaiThanhToan());
                String[] tb = thongBaoCskh(donMoi, hinhThuc, daTra);
                return tb == null ? null : RealtimeService.jsonDon(tb[0], don, hinhThuc, daTra, tb[1]);
            });
            if (json == null) return;
            String loai = donMoi ? "don-moi" : "da-thanh-toan";
            realtimeService.guiNhanVien(CSKH, loai, json);
        } catch (Exception ex) {
            log.warn("Không gửi được thông báo đơn #{} cho CSKH: {}", donId, ex.getMessage());
        }
    }
}
