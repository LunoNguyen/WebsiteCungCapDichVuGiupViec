package com.example.Service;

import com.example.SuKien.SuKienDon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Mọi thay đổi đơn đặt / phân công (kể cả từ tác vụ nền như tự hủy đơn quá hạn) đều báo realtime
 * cho các trang web đang mở, không chỉ thao tác qua HTTP mà CacheInvalidationInterceptor bắt được.
 */
@Service
public class RealtimeSuKienDonService {

    private final RealtimeService realtimeService;

    public RealtimeSuKienDonService(RealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void khiDonThayDoi(SuKienDon.DonMoi e) { bao(); }

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
}
