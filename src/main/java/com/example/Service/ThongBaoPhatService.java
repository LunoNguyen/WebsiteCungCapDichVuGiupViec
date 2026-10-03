package com.example.Service;

import com.example.Repository.ThongBaoNguoiDungRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phát thông báo tới nhóm người nhận ở luồng nền (bất đồng bộ).
 * Tách thành lớp riêng vì @Async chỉ có tác dụng khi được gọi từ một bean khác.
 */
@Service
public class ThongBaoPhatService {

    private static final Logger log = LoggerFactory.getLogger(ThongBaoPhatService.class);

    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final RealtimeService realtimeService;

    public ThongBaoPhatService(ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
                               RealtimeService realtimeService) {
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.realtimeService = realtimeService;
    }

    /**
     * @param nhomNhan KhachHang | CongTacVien | TatCa
     */
    @Async("taskExecutor")
    @Transactional
    public void phatChoNhom(Integer thongBaoId, String nhomNhan) {
        try {
            int soNguoi = 0;
            if ("KhachHang".equals(nhomNhan) || "TatCa".equals(nhomNhan)) {
                soNguoi += thongBaoNguoiDungRepository.phatChoKhachHang(thongBaoId);
            }
            if ("CongTacVien".equals(nhomNhan) || "TatCa".equals(nhomNhan)) {
                soNguoi += thongBaoNguoiDungRepository.phatChoCongTacVien(thongBaoId);
            }
            log.info("Đã phát thông báo #{} tới {} người nhận (nhóm {})", thongBaoId, soNguoi, nhomNhan);
        } catch (RuntimeException e) {
            log.error("Phát thông báo #{} thất bại: {}", thongBaoId, e.getMessage(), e);
            throw e; // để giao dịch được hoàn tác
        } finally {
            // Trang thông báo đang mở sẽ tự cập nhật số người nhận
            realtimeService.phatSuKien("/cskh/thong-bao");
        }
    }
}
