package com.example.Service;

import com.example.Model.CongTacVien;
import com.example.Model.KhachHang;
import com.example.Model.TaiKhoan;
import com.example.Model.ThongBao;
import com.example.Model.ThongBaoNguoiDung;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.KhachHangRepository;
import com.example.Repository.TaiKhoanRepository;
import com.example.Repository.ThongBaoNguoiDungRepository;
import com.example.Repository.ThongBaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nghiệp vụ thông báo của bộ phận Chăm sóc khách hàng: gửi, xem, xóa.
 *
 * Thông báo lưu ở bảng ThongBao; mỗi người nhận có một dòng ở ThongBaoNguoiDung
 * (ứng dụng di động đọc bảng này để hiện thông báo và đánh dấu đã đọc).
 */
@Service
public class ThongBaoService {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ThongBaoRepository thongBaoRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final ThongBaoPhatService thongBaoPhatService;

    public ThongBaoService(ThongBaoRepository thongBaoRepository,
                           ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
                           TaiKhoanRepository taiKhoanRepository,
                           KhachHangRepository khachHangRepository,
                           CongTacVienRepository congTacVienRepository,
                           ThongBaoPhatService thongBaoPhatService) {
        this.thongBaoRepository = thongBaoRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.thongBaoPhatService = thongBaoPhatService;
    }

    // =========================================================================
    // GỬI
    // =========================================================================

    /**
     * Tạo và gửi thông báo.
     *
     * @param nhomNhan       KhachHang | CongTacVien | TatCa | CaNhan
     * @param taiKhoanNhanId bắt buộc khi nhomNhan = CaNhan
     */
    @Transactional
    public ThongBao gui(String tieuDe, String noiDung, String nhomNhan, Integer taiKhoanNhanId, String nguoiGui) {
        if (tieuDe == null || tieuDe.isBlank()) {
            throw new IllegalArgumentException("Nhập tiêu đề thông báo.");
        }
        if (tieuDe.trim().length() > 200) {
            throw new IllegalArgumentException("Tiêu đề tối đa 200 ký tự.");
        }
        if (noiDung == null || noiDung.isBlank()) {
            throw new IllegalArgumentException("Nhập nội dung thông báo.");
        }
        if (!List.of("KhachHang", "CongTacVien", "TatCa", "CaNhan").contains(nhomNhan)) {
            throw new IllegalArgumentException("Chọn đối tượng nhận thông báo.");
        }

        TaiKhoan nguoiNhan = null;
        if ("CaNhan".equals(nhomNhan)) {
            if (taiKhoanNhanId == null) {
                throw new IllegalArgumentException("Chọn người nhận thông báo.");
            }
            nguoiNhan = taiKhoanRepository.findById(taiKhoanNhanId)
                    .orElseThrow(() -> new IllegalArgumentException("Người nhận không tồn tại hoặc chưa có tài khoản."));
        }

        String ten = nguoiGui == null || nguoiGui.isBlank() ? "CSKH" : nguoiGui.trim();
        ThongBao thongBao = thongBaoRepository.save(ThongBao.builder()
                .maThongBao(MaSinh.tao("TB-"))
                .tieuDe(tieuDe.trim())
                .noiDung(noiDung.trim())
                .nguoiGui(ten.length() > 50 ? ten.substring(0, 50) : ten)
                .nhomNhan(nhomNhan)
                .thoiGianGui(LocalDateTime.now())
                .trangThai("DaGui")
                .build());

        if (nguoiNhan != null) {
            thongBaoNguoiDungRepository.save(ThongBaoNguoiDung.builder()
                    .thongBao(thongBao)
                    .taiKhoan(nguoiNhan)
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build());
        } else {
            // Gửi cho cả nhóm: phát ở luồng nền SAU KHI giao dịch này đã ghi xong (commit),
            // để luồng nền chắc chắn nhìn thấy thông báo vừa tạo.
            final Integer thongBaoId = thongBao.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    thongBaoPhatService.phatChoNhom(thongBaoId, nhomNhan);
                }
            });
        }
        return thongBao;
    }

    // =========================================================================
    // XÓA
    // =========================================================================

    @Transactional
    public void xoa(Integer id) {
        ThongBao thongBao = thongBaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thông báo không còn tồn tại (có thể đã bị người khác xóa)."));
        thongBaoNguoiDungRepository.deleteByThongBao_Id(id);
        thongBaoRepository.delete(thongBao);
    }

    // =========================================================================
    // XEM
    // =========================================================================

    /** Danh sách thông báo (mới nhất trước) kèm số người nhận, số đã đọc và tên người nhận cá nhân. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> danhSach() {
        Map<Integer, long[]> thongKe = new HashMap<>();
        for (Object[] dong : thongBaoNguoiDungRepository.thongKeNguoiNhan()) {
            long daDoc = dong[2] != null ? ((Number) dong[2]).longValue() : 0;
            thongKe.put((Integer) dong[0], new long[]{((Number) dong[1]).longValue(), daDoc});
        }

        List<ThongBao> tatCa = new ArrayList<>(thongBaoRepository.findAll());
        tatCa.sort(Comparator.comparing(ThongBao::getId).reversed());

        List<Map<String, Object>> ketQua = new ArrayList<>();
        for (ThongBao tb : tatCa) {
            ketQua.add(thanhDong(tb, thongKe.getOrDefault(tb.getId(), new long[]{0, 0})));
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> chiTiet(Integer id) {
        ThongBao tb = thongBaoRepository.findById(id).orElse(null);
        if (tb == null) {
            return null;
        }
        List<ThongBaoNguoiDung> nguoiNhan = thongBaoNguoiDungRepository.findByThongBao_Id(id);
        long daDoc = nguoiNhan.stream().filter(n -> Boolean.TRUE.equals(n.getDaDoc())).count();
        Map<String, Object> dong = thanhDong(tb, new long[]{nguoiNhan.size(), daDoc});
        dong.put("noiDung", tb.getNoiDung());
        return dong;
    }

    private Map<String, Object> thanhDong(ThongBao tb, long[] soLieu) {
        Map<String, Object> dong = new LinkedHashMap<>();
        dong.put("id", tb.getId());
        dong.put("maThongBao", tb.getMaThongBao());
        dong.put("tieuDe", tb.getTieuDe());
        dong.put("nguoiGui", tb.getNguoiGui());
        dong.put("nhomNhan", tb.getNhomNhan());
        dong.put("doiTuong", tenDoiTuong(tb));
        dong.put("thoiGianGui", tb.getThoiGianGui() != null ? tb.getThoiGianGui().format(NGAY_GIO) : "");
        dong.put("trangThai", tb.getTrangThai());
        dong.put("tenTrangThai", switch (tb.getTrangThai() == null ? "" : tb.getTrangThai()) {
            case "DaGui" -> "Đã gửi";
            case "ChuaGui" -> "Chờ duyệt";
            case "Nhap" -> "Bản nháp";
            default -> tb.getTrangThai();
        });
        dong.put("soNguoiNhan", soLieu[0]);
        dong.put("soDaDoc", soLieu[1]);
        return dong;
    }

    private String tenDoiTuong(ThongBao tb) {
        String nhom = tb.getNhomNhan() == null ? "" : tb.getNhomNhan();
        switch (nhom) {
            case "KhachHang":
                return "Tất cả khách hàng";
            case "CongTacVien":
                return "Tất cả cộng tác viên";
            case "NhanVien":
                return "Nhân viên";
            case "TatCa":
                return "Khách hàng và cộng tác viên";
            case "CaNhan":
                List<ThongBaoNguoiDung> nguoiNhan = thongBaoNguoiDungRepository.findByThongBao_Id(tb.getId());
                if (nguoiNhan.isEmpty() || nguoiNhan.get(0).getTaiKhoan() == null) {
                    return "Cá nhân";
                }
                return tenChuTaiKhoan(nguoiNhan.get(0).getTaiKhoan());
            default:
                return nhom;
        }
    }

    private String tenChuTaiKhoan(TaiKhoan taiKhoan) {
        KhachHang kh = khachHangRepository.findByTaiKhoan_Id(taiKhoan.getId()).orElse(null);
        if (kh != null) {
            return kh.getHoTen() + " (khách hàng)";
        }
        CongTacVien ctv = congTacVienRepository.findByTaiKhoan_Id(taiKhoan.getId()).orElse(null);
        if (ctv != null) {
            return ctv.getHoTen() + " (cộng tác viên)";
        }
        return taiKhoan.getTenDangNhap();
    }

    // =========================================================================
    // TÌM NGƯỜI NHẬN
    // =========================================================================

    /**
     * Tìm khách hàng hoặc cộng tác viên có tài khoản theo tên / số điện thoại (tối đa 20 kết quả).
     *
     * @param loai KhachHang | CongTacVien
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> timNguoiNhan(String loai, String tuKhoa) {
        String q = tuKhoa == null ? "" : tuKhoa.trim().toLowerCase(Locale.ROOT);
        List<Map<String, Object>> ketQua = new ArrayList<>();
        if ("CongTacVien".equals(loai)) {
            for (CongTacVien ctv : congTacVienRepository.findAll()) {
                if (ketQua.size() >= 20) {
                    break;
                }
                if (ctv.getTaiKhoan() != null && khop(q, ctv.getHoTen(), ctv.getSoDienThoai())) {
                    ketQua.add(nguoi(ctv.getTaiKhoan().getId(), ctv.getHoTen(), ctv.getSoDienThoai()));
                }
            }
        } else {
            for (KhachHang kh : khachHangRepository.findAll()) {
                if (ketQua.size() >= 20) {
                    break;
                }
                if (kh.getTaiKhoan() != null && khop(q, kh.getHoTen(), kh.getSoDienThoai())) {
                    ketQua.add(nguoi(kh.getTaiKhoan().getId(), kh.getHoTen(), kh.getSoDienThoai()));
                }
            }
        }
        return ketQua;
    }

    private static boolean khop(String q, String ten, String sdt) {
        return q.isEmpty()
                || (ten != null && ten.toLowerCase(Locale.ROOT).contains(q))
                || (sdt != null && sdt.contains(q));
    }

    private static Map<String, Object> nguoi(Integer taiKhoanId, String ten, String sdt) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taiKhoanId", taiKhoanId);
        m.put("ten", ten);
        m.put("soDienThoai", sdt);
        return m;
    }
}
