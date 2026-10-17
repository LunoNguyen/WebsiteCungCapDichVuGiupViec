package com.example.Service;

import com.example.Model.LichSuSuDungKhuyenMai;
import com.example.Repository.LichSuSuDungKhuyenMaiRepository;
import com.example.Repository.MaKhuyenMaiRepository;
import com.example.SuKien.SuKienDon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Đơn đã dùng mã khuyến mãi bị hủy (khách hủy, tự hủy do quá hạn, CSKH hủy): trả lại lượt dùng của mã.
 * Khách được dùng lại mã đó vì quy tắc "mỗi khách một lần" chỉ tính đơn chưa bị hủy.
 */
@Service
public class HoanLuotKhuyenMaiService {

    private static final Logger log = LoggerFactory.getLogger(HoanLuotKhuyenMaiService.class);

    private final LichSuSuDungKhuyenMaiRepository lichSuRepo;
    private final MaKhuyenMaiRepository maKhuyenMaiRepo;

    public HoanLuotKhuyenMaiService(LichSuSuDungKhuyenMaiRepository lichSuRepo, MaKhuyenMaiRepository maKhuyenMaiRepo) {
        this.lichSuRepo = lichSuRepo;
        this.maKhuyenMaiRepo = maKhuyenMaiRepo;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void khiDonDoiTrangThai(SuKienDon.DoiTrangThai e) {
        if (!"DaHuy".equals(e.moi())) return;
        for (LichSuSuDungKhuyenMai ls : lichSuRepo.findByDonDat_Id(e.donId())) {
            if (ls.getKhuyenMai() != null && maKhuyenMaiRepo.hoanLuotDung(ls.getKhuyenMai().getId()) > 0) {
                log.info("Đơn #{} bị hủy: trả lại 1 lượt dùng mã khuyến mãi #{}", e.donId(), ls.getKhuyenMai().getId());
            }
        }
    }
}
