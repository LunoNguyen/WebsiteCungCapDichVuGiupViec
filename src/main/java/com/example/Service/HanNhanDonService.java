package com.example.Service;

import com.example.Model.DonDatDichVu;
import com.example.Model.HoaDon;
import com.example.Model.LichSuTrangThaiDon;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.HoaDonRepository;
import com.example.Repository.LichSuTrangThaiDonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Phát hành đơn và thời hạn nhận đơn.
 * - Đơn tiền mặt được phát hành cho cộng tác viên ngay khi đặt; đơn chuyển khoản chỉ được phát hành
 *   sau khi khách thanh toán (suy ra từ HoaDon, không thêm trạng thái đơn).
 * - Đã phát hành thì CTV tự nhận / nhân viên phân công trong {@code app.don.thoi-gian-nhan-phut} phút
 *   (mặc định 15), tính từ lúc phát hành hoặc lúc đơn bị trả lại "Chờ duyệt" (CTV hủy / bị bỏ phân công).
 *   Hết hạn mà chưa có ai nhận thì TuHuyDonService hủy đơn cho khách.
 */
@Service
public class HanNhanDonService {

    private final LichSuTrangThaiDonRepository lichSuRepo;
    private final DonDatDichVuRepository donRepo;
    private final HoaDonRepository hoaDonRepo;

    @Value("${app.don.thoi-gian-nhan-phut:15}")
    private long soPhut;

    @Value("${app.don.han-thanh-toan-phut:15}")
    private long hanThanhToanPhut;

    public HanNhanDonService(LichSuTrangThaiDonRepository lichSuRepo, DonDatDichVuRepository donRepo,
                             HoaDonRepository hoaDonRepo) {
        this.lichSuRepo = lichSuRepo;
        this.donRepo = donRepo;
        this.hoaDonRepo = hoaDonRepo;
    }

    /** Phương thức thanh toán lưu vào HoaDon: null / trống = tiền mặt (ứng dụng chưa có bước thanh toán). */
    public static String chuanHoaHinhThuc(String hinhThuc) {
        if (hinhThuc == null || hinhThuc.isBlank() || "TienMat".equalsIgnoreCase(hinhThuc.trim())) return "TienMat";
        if ("ChuyenKhoan".equalsIgnoreCase(hinhThuc.trim())) return "ChuyenKhoan";
        throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ.");
    }

    /**
     * Thời điểm đơn được phát hành cho CTV; null = chưa phát hành (chuyển khoản chưa thanh toán).
     *
     * @param lanChoDuyetGanNhat lần gần nhất đơn chuyển sang "Chờ duyệt" (lịch sử trạng thái), có thể null
     */
    public static LocalDateTime tinhLucPhatHanh(LocalDateTime lanChoDuyetGanNhat, LocalDateTime ngayTao,
                                                String hinhThuc, String trangThaiThanhToan, LocalDateTime ngayThanhToan) {
        LocalDateTime choDuyet = lanChoDuyetGanNhat != null ? lanChoDuyetGanNhat : ngayTao;
        if ("ChuyenKhoan".equalsIgnoreCase(hinhThuc)) {
            if (!"DaThanhToan".equalsIgnoreCase(trangThaiThanhToan)) return null;
            if (ngayThanhToan == null) return choDuyet;
            return choDuyet != null && choDuyet.isAfter(ngayThanhToan) ? choDuyet : ngayThanhToan;
        }
        return choDuyet;
    }

    /**
     * Hạn nhận đơn để hiển thị đếm ngược; null nếu đơn không còn chờ nhận
     * (không phải "Chờ duyệt", chưa phát hành hoặc không xác định được lúc phát hành).
     */
    public LocalDateTime hanNhanNeuDangCho(Integer donId) {
        if (donId == null) return null;
        return donRepo.findById(donId)
                .filter(d -> "ChoDuyet".equals(d.getTrangThai()))
                .map(this::hanNhan)
                .orElse(null);
    }

    public long soPhut() {
        return soPhut;
    }

    public long soPhutThanhToan() {
        return hanThanhToanPhut;
    }

    /** Hạn chuyển khoản của đơn đang chờ thanh toán (Chờ duyệt + chuyển khoản + chưa trả); còn lại null. */
    public LocalDateTime hanThanhToanNeuDangCho(Integer donId) {
        if (donId == null) return null;
        DonDatDichVu don = donRepo.findById(donId).orElse(null);
        if (don == null || !"ChoDuyet".equals(don.getTrangThai()) || don.getNgayTao() == null) return null;
        HoaDon hd = hoaDonRepo.findByDonDat_Id(donId).orElse(null);
        if (hd == null || !"ChuyenKhoan".equalsIgnoreCase(hd.getHinhThucThanhToan())
                || !"ChuaThanhToan".equalsIgnoreCase(hd.getTrangThaiThanhToan())) return null;
        return don.getNgayTao().plusMinutes(hanThanhToanPhut);
    }

    /** Thời điểm đơn được phát hành cho CTV (xem {@link #tinhLucPhatHanh}); null = chưa phát hành. */
    public LocalDateTime batDauPhatHanh(DonDatDichVu don) {
        LocalDateTime choDuyet = lichSuRepo.findFirstByDonDat_IdAndTrangThaiMoiOrderByThoiGianDescIdDesc(don.getId(), "ChoDuyet")
                .map(LichSuTrangThaiDon::getThoiGian)
                .orElse(null);
        HoaDon hd = hoaDonRepo.findByDonDat_Id(don.getId()).orElse(null);
        return tinhLucPhatHanh(choDuyet, don.getNgayTao(),
                hd != null ? hd.getHinhThucThanhToan() : null,
                hd != null ? hd.getTrangThaiThanhToan() : null,
                hd != null ? hd.getNgayThanhToan() : null);
    }

    public boolean daPhatHanh(DonDatDichVu don) {
        return batDauPhatHanh(don) != null;
    }

    /** Hạn cuối để có người nhận đơn; null nếu chưa phát hành / không xác định được lúc phát hành. */
    public LocalDateTime hanNhan(DonDatDichVu don) {
        LocalDateTime batDau = batDauPhatHanh(don);
        return batDau == null ? null : batDau.plusMinutes(soPhut);
    }

    public boolean daHetHan(DonDatDichVu don, LocalDateTime luc) {
        LocalDateTime han = hanNhan(don);
        return han != null && luc.isAfter(han);
    }

    /** Số giây còn lại để nhận đơn (0 nếu đã hết hạn, null nếu không xác định). */
    public Long soGiayConLai(DonDatDichVu don) {
        LocalDateTime han = hanNhan(don);
        if (han == null) return null;
        return Math.max(0, Duration.between(LocalDateTime.now(), han).getSeconds());
    }

    /** Chặn nhận / phân công đơn chưa phát hành (chờ thanh toán) hoặc đã quá thời hạn nhận (đang chờ hệ thống hủy). */
    public void kiemTraConHan(DonDatDichVu don) {
        LocalDateTime han = hanNhan(don);
        if (han == null) {
            throw new IllegalArgumentException("Đơn đang chờ khách thanh toán, chưa phát hành cho cộng tác viên.");
        }
        if (LocalDateTime.now().isAfter(han)) {
            throw new IllegalArgumentException("Đơn đã hết " + soPhut
                    + " phút chờ nhận và đang được hệ thống hủy, không nhận hoặc phân công được nữa.");
        }
    }
}
