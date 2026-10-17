package com.example.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Cập nhật dữ liệu theo thời gian thực bằng Server-Sent Events (SSE).
 *
 * Mỗi trang quản trị mở một kết nối tới /realtime/stream. Khi có thao tác ghi dữ liệu thành công
 * (nhân viên thêm/sửa/xóa trên web, hoặc khách hàng/cộng tác viên thao tác trên ứng dụng di động),
 * CacheInvalidationInterceptor gọi {@link #phatSuKien(String)} và mọi trang đang mở nhận được tín hiệu
 * "du-lieu-thay-doi" để tự lấy dữ liệu mới (xem /js/realtime.js).
 *
 * Sự kiện chỉ mang đường dẫn vừa thay đổi, không mang dữ liệu nghiệp vụ; dữ liệu vẫn được tải qua
 * các trang đã phân quyền.
 */
@Service
public class RealtimeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeService.class);

    private final List<SseEmitter> ketNoi = new CopyOnWriteArrayList<>();
    /** Vai trò (ROLE_CSKH, ROLE_HCNS...) của từng kết nối trang quản trị: để gửi thông báo cho riêng một bộ phận. */
    private final Map<SseEmitter, String> vaiTroKetNoi = new ConcurrentHashMap<>();

    /** Kết nối của ứng dụng di động theo tài khoản (một tài khoản có thể mở trên nhiều máy). */
    private record KetNoiApp(Integer taiKhoanId, String vaiTro, SseEmitter emitter) { }
    private final List<KetNoiApp> ketNoiApp = new CopyOnWriteArrayList<>();

    /**
     * App (khách hàng / cộng tác viên) đăng ký nhận sự kiện của tài khoản mình:
     * "thong-bao-moi" (JSON {tieuDe, noiDung}) khi có thông báo mới → app hiện thông báo, cập nhật số chưa đọc
     * và tải lại dữ liệu đơn.
     *
     * @param vaiTro KhachHang | CongTacVien – để nhận cả thông báo gửi theo nhóm
     */
    public SseEmitter dangKyApp(Integer taiKhoanId, String vaiTro) {
        SseEmitter emitter = new SseEmitter(0L);
        KetNoiApp kn = new KetNoiApp(taiKhoanId, vaiTro, emitter);
        ketNoiApp.add(kn);
        emitter.onCompletion(() -> ketNoiApp.remove(kn));
        emitter.onTimeout(() -> ketNoiApp.remove(kn));
        emitter.onError(e -> ketNoiApp.remove(kn));
        guiApp(kn, "san-sang", "ok");
        return emitter;
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper JSON = new com.fasterxml.jackson.databind.ObjectMapper();

    /** Dữ liệu sự kiện "thong-bao-moi": {"id", "tieuDe", "noiDung"}. */
    public static String jsonThongBao(com.example.Model.ThongBao tb) {
        try {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", tb.getId());
            m.put("tieuDe", tb.getTieuDe());
            m.put("noiDung", tb.getNoiDung());
            return JSON.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Gửi sự kiện tới mọi máy đang mở app của một tài khoản. */
    public void guiChoTaiKhoan(Integer taiKhoanId, String tenSuKien, String duLieu) {
        if (taiKhoanId == null) return;
        for (KetNoiApp kn : ketNoiApp) {
            if (taiKhoanId.equals(kn.taiKhoanId())) guiApp(kn, tenSuKien, duLieu);
        }
    }

    /** Gửi sự kiện tới mọi app thuộc nhóm (KhachHang | CongTacVien | TatCa). */
    public void guiChoNhom(String nhom, String tenSuKien, String duLieu) {
        for (KetNoiApp kn : ketNoiApp) {
            if ("TatCa".equals(nhom) || nhom.equals(kn.vaiTro())) guiApp(kn, tenSuKien, duLieu);
        }
    }

    private void guiApp(KetNoiApp kn, String tenSuKien, String duLieu) {
        try {
            kn.emitter().send(SseEmitter.event().name(tenSuKien).data(duLieu == null ? "" : duLieu.replaceAll("[\r\n]", " ")));
        } catch (Exception e) {
            ketNoiApp.remove(kn);
            try {
                kn.emitter().completeWithError(e);
            } catch (Exception ignored) {
                // kết nối đã đóng
            }
        }
    }

    /** Đăng ký một trình duyệt mới (đã đăng nhập với vai trò vaiTro) vào danh sách nhận sự kiện. */
    public SseEmitter dangKy(String vaiTro) {
        // không tự hết hạn; trình duyệt đóng thì gỡ khỏi danh sách
        return dangKy(vaiTro, new SseEmitter(0L));
    }

    SseEmitter dangKy(String vaiTro, SseEmitter emitter) {
        ketNoi.add(emitter);
        if (vaiTro != null) vaiTroKetNoi.put(emitter, vaiTro);
        emitter.onCompletion(() -> go(emitter));
        emitter.onTimeout(() -> go(emitter));
        emitter.onError(e -> go(emitter));
        gui(emitter, "san-sang", "ok");
        return emitter;
    }

    private void go(SseEmitter emitter) {
        ketNoi.remove(emitter);
        vaiTroKetNoi.remove(emitter);
    }

    /**
     * Gửi sự kiện có nội dung (JSON) cho các trang quản trị của những vai trò trong tập,
     * VD "don-moi" cho ROLE_CSKH (realtime.js hiện thẻ thông báo).
     */
    public void guiNhanVien(java.util.Set<String> vaiTro, String tenSuKien, String duLieuJson) {
        String duLieu = duLieuJson == null ? "" : duLieuJson.replaceAll("[\r\n]", " ");
        for (SseEmitter emitter : ketNoi) {
            String vt = vaiTroKetNoi.get(emitter);
            if (vt != null && vaiTro.contains(vt)) gui(emitter, tenSuKien, duLieu);
        }
    }

    /**
     * Dữ liệu sự kiện đơn cho nhân viên: chỉ thông tin tóm tắt, không có số điện thoại / địa chỉ chi tiết của khách.
     *
     * @param loai don-moi | da-thanh-toan | tu-huy
     */
    public static String jsonDon(String loai, com.example.Model.DonDatDichVu don, String hinhThuc,
                                 boolean daThanhToan, String ghiChu) {
        try {
            String tenDichVu = don.getChiTietList() != null && !don.getChiTietList().isEmpty()
                    && don.getChiTietList().get(0).getDichVu() != null
                    ? don.getChiTietList().get(0).getDichVu().getTenDichVu() : "Dịch vụ";
            String ngayGio = (don.getNgayThucHien() != null
                    ? don.getNgayThucHien().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                    + (don.getGioBatDau() != null
                    ? " " + don.getGioBatDau().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) : "");
            String khuVuc = don.getDiaChi() != null && don.getDiaChi().getKhuVuc() != null
                    ? don.getDiaChi().getKhuVuc().getTenKhuVuc() : "";
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("loai", loai);
            m.put("donId", don.getId());
            m.put("maDonDat", don.getMaDonDat());
            m.put("tenDichVu", tenDichVu);
            m.put("ngayGio", ngayGio.trim());
            m.put("khuVuc", khuVuc);
            m.put("hinhThucThanhToan", hinhThuc);
            m.put("daThanhToan", daThanhToan);
            m.put("ghiChu", ghiChu);
            m.put("url", "/cskh/don-dat-dich-vu?moDon=" + don.getId());
            return JSON.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** Báo cho mọi trang đang mở biết dữ liệu vừa thay đổi. */
    public void phatSuKien(String duongDan) {
        if (ketNoi.isEmpty()) {
            return;
        }
        String noiDung = duongDan == null ? "" : duongDan.replaceAll("[\\r\\n]", "");
        for (SseEmitter emitter : ketNoi) {
            gui(emitter, "du-lieu-thay-doi", noiDung);
        }
        log.debug("Realtime: báo thay đổi '{}' tới {} kết nối", noiDung, ketNoi.size());
    }

    /** Gửi nhịp giữ kết nối để proxy/trình duyệt không tự ngắt, đồng thời dọn kết nối đã chết. */
    @Scheduled(fixedRate = 25000)
    public void giuKetNoi() {
        for (SseEmitter emitter : ketNoi) {
            gui(emitter, "nhip", "");
        }
        for (KetNoiApp kn : ketNoiApp) {
            guiApp(kn, "nhip", "");
        }
    }

    public int soKetNoi() {
        return ketNoi.size();
    }

    private void gui(SseEmitter emitter, String tenSuKien, String duLieu) {
        try {
            emitter.send(SseEmitter.event().name(tenSuKien).data(duLieu));
        } catch (Exception e) {
            // Trình duyệt đã đóng tab hoặc mất mạng
            go(emitter);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
                // kết nối đã đóng
            }
        }
    }
}
