package com.example.Service;

import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Xét duyệt đơn ứng tuyển cộng tác viên ở phòng HCNS:
 *   1. Đơn mới (HCNS chưa mở) được làm nổi bật; mở chi tiết đơn thì ghi nhận "đã xem".
 *   2. Gửi lịch phỏng vấn (thời gian, địa điểm) cho ứng viên: thông báo trong tài khoản + SMS.
 *   3. Chỉ phê duyệt được sau khi đã gửi lịch phỏng vấn (CapTaiKhoanCtvService).
 *
 * Không thêm cột: "đã xem" lưu ở NhatKyTaiKhoan của tài khoản ứng viên, lịch phỏng vấn là
 * thông báo cá nhân (ThongBao/ThongBaoNguoiDung) có tiêu đề bắt đầu bằng {@link #TIEN_TO_PHONG_VAN}.
 */
@Service
public class UngVienCtvService {

    public static final String HANH_DONG_DA_XEM = "HCNS xem hồ sơ ứng tuyển";
    public static final String TIEN_TO_PHONG_VAN = "Lịch phỏng vấn CTV: ";
    private static final DateTimeFormatter GIO_NGAY = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] THU = {"Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"};

    private final CongTacVienRepository congTacVienRepository;
    private final NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final DichVuCTVRepository dichVuCTVRepository;
    private final KhuVucCTVRepository khuVucCTVRepository;
    private final ChungChiCTVRepository chungChiCTVRepository;
    private final HoSoCTVRepository hoSoCTVRepository;
    private final ThongBaoService thongBaoService;
    private final SmsSender smsSender;

    public UngVienCtvService(CongTacVienRepository congTacVienRepository,
                             NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository,
                             ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
                             DichVuCTVRepository dichVuCTVRepository,
                             KhuVucCTVRepository khuVucCTVRepository,
                             ChungChiCTVRepository chungChiCTVRepository,
                             HoSoCTVRepository hoSoCTVRepository,
                             ThongBaoService thongBaoService,
                             SmsSender smsSender) {
        this.congTacVienRepository = congTacVienRepository;
        this.nhatKyTaiKhoanRepository = nhatKyTaiKhoanRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.dichVuCTVRepository = dichVuCTVRepository;
        this.khuVucCTVRepository = khuVucCTVRepository;
        this.chungChiCTVRepository = chungChiCTVRepository;
        this.hoSoCTVRepository = hoSoCTVRepository;
        this.thongBaoService = thongBaoService;
        this.smsSender = smsSender;
    }

    /** Lịch phỏng vấn đã gửi gần nhất của một ứng viên. */
    public record LichPhongVan(String thoiGian, String noiDung, LocalDateTime guiLuc) {
        public boolean coTheDoiLich() {
            try {
                LocalDateTime gioHen = LocalDateTime.parse(thoiGian, GIO_NGAY);
                return LocalDateTime.now().isBefore(gioHen.minusHours(6));
            } catch (Exception e) {
                return false;
            }
        }
    }

    // =========================================================================
    // ĐÃ XEM / CHƯA XEM
    // =========================================================================

    /** Id các hồ sơ CTV mà HCNS đã mở xem (hồ sơ không có tài khoản do HCNS tự tạo: coi như đã xem). */
    @Transactional(readOnly = true)
    public Set<Integer> hoSoDaXem(List<CongTacVien> ds) {
        Set<Integer> taiKhoanDaXem = new HashSet<>(nhatKyTaiKhoanRepository.taiKhoanDaCoHanhDong(HANH_DONG_DA_XEM));
        Set<Integer> ketQua = new HashSet<>();
        for (CongTacVien ctv : ds) {
            if (ctv.getTaiKhoan() == null || taiKhoanDaXem.contains(ctv.getTaiKhoan().getId())) {
                ketQua.add(ctv.getId());
            }
        }
        return ketQua;
    }

    @Transactional
    public void danhDauDaXem(CongTacVien ctv, String ip, String thietBi) {
        TaiKhoan tk = ctv.getTaiKhoan();
        if (tk == null || nhatKyTaiKhoanRepository.existsByTaiKhoan_IdAndHanhDong(tk.getId(), HANH_DONG_DA_XEM)) {
            return;
        }
        ghiNhatKy(tk, HANH_DONG_DA_XEM, ip, thietBi);
    }

    // =========================================================================
    // LỊCH PHỎNG VẤN
    // =========================================================================

    /** Lịch phỏng vấn gần nhất theo id hồ sơ CTV. */
    @Transactional(readOnly = true)
    public Map<Integer, LichPhongVan> lichPhongVan(List<CongTacVien> ds) {
        Map<Integer, Integer> ctvTheoTaiKhoan = new HashMap<>();
        for (CongTacVien ctv : ds) {
            if (ctv.getTaiKhoan() != null) ctvTheoTaiKhoan.put(ctv.getTaiKhoan().getId(), ctv.getId());
        }
        Map<Integer, LichPhongVan> ketQua = new HashMap<>();
        if (ctvTheoTaiKhoan.isEmpty()) return ketQua;
        for (ThongBaoNguoiDung t : thongBaoNguoiDungRepository.timTheoTienToTieuDe(TIEN_TO_PHONG_VAN)) {
            Integer ctvId = ctvTheoTaiKhoan.get(t.getTaiKhoan().getId());
            if (ctvId != null && !ketQua.containsKey(ctvId)) { // danh sách đã sắp mới nhất trước
                ThongBao b = t.getThongBao();
                ketQua.put(ctvId, new LichPhongVan(b.getTieuDe().substring(TIEN_TO_PHONG_VAN.length()),
                        b.getNoiDung(), b.getThoiGianGui()));
            }
        }
        return ketQua;
    }

    @Transactional(readOnly = true)
    public boolean daGuiLichPhongVan(CongTacVien ctv) {
        return lichPhongVan(List.of(ctv)).containsKey(ctv.getId());
    }

    /**
     * Gửi (hoặc đổi) lịch phỏng vấn: thông báo trong tài khoản ứng viên + SMS.
     *
     * @return nội dung SMS đã gửi
     */
    @Transactional
    public String guiLichPhongVan(Integer ctvId, LocalDateTime thoiGian, String diaDiem, String ghiChu,
                                  String nguoiGui, String ip, String thietBi) {
        CongTacVien ctv = congTacVienRepository.findById(ctvId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ ứng viên."));
        if (!"ChoDuyet".equals(ctv.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ gửi lịch phỏng vấn cho hồ sơ đang chờ duyệt.");
        }
        TaiKhoan tk = ctv.getTaiKhoan();
        if (tk == null) {
            throw new IllegalArgumentException("Hồ sơ này chưa có tài khoản ứng viên nên không gửi được lịch phỏng vấn.");
        }
        if (thoiGian == null || !thoiGian.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Thời gian phỏng vấn phải sau thời điểm hiện tại.");
        }
        String noi = diaDiem != null ? diaDiem.trim() : "";
        if (noi.length() < 10) {
            throw new IllegalArgumentException("Nhập địa chỉ phỏng vấn đầy đủ (số nhà, đường, phường).");
        }
        String gc = ghiChu != null && !ghiChu.isBlank() ? ghiChu.trim() : null;
        LichPhongVan lichCu = lichPhongVan(List.of(ctv)).get(ctv.getId());
        boolean doiLich = lichCu != null;
        if (doiLich && !lichCu.coTheDoiLich()) {
            throw new IllegalArgumentException("Không thể đổi lịch phỏng vấn: Đã quá thời hạn cho phép (chỉ được đổi trước giờ hẹn cũ ít nhất 6 tiếng). Lịch hiện tại: " + lichCu.thoiGian());
        }

        String thoiGianText = thoiGian.format(GIO_NGAY);
        String thuNgay = THU[thoiGian.getDayOfWeek().getValue() - 1] + ", " + thoiGian.format(NGAY);
        String noiDung = "Chào " + ctv.getHoTen() + ",\n"
                + (doiLich ? "bTaskee xin thông báo lịch phỏng vấn của bạn đã được thay đổi.\n"
                           : "Hồ sơ cộng tác viên " + ctv.getMaCongTacVien() + " của bạn đã qua vòng xét hồ sơ. Mời bạn đến phỏng vấn:\n")
                + "- Thời gian: " + thoiGian.format(DateTimeFormatter.ofPattern("HH:mm")) + ", " + thuNgay + "\n"
                + "- Địa điểm: " + noi + "\n"
                + (gc != null ? "- Ghi chú: " + gc + "\n" : "")
                + "Vui lòng mang theo CCCD bản gốc và có mặt trước 10 phút. Nếu cần đổi lịch, hãy liên hệ phòng HCNS.";
        thongBaoService.gui(TIEN_TO_PHONG_VAN + thoiGianText, noiDung, "CaNhan", tk.getId(),
                nguoiGui != null && !nguoiGui.isBlank() ? nguoiGui : "HCNS");

        String sms = "bTaskee: " + (doiLich ? "Lich phong van CTV cua ban doi sang " : "Moi ban phong van CTV luc ")
                + thoiGianText + " tai " + boDau(noi) + ". Mang theo CCCD goc.";
        String maTin = smsSender.send(ctv.getSoDienThoai(), sms);
        ghiNhatKy(tk, (doiLich ? "HCNS đổi lịch phỏng vấn " : "HCNS mời phỏng vấn ") + thoiGianText + " (" + maTin + ")", ip, thietBi);
        return sms;
    }

    // =========================================================================
    // CHI TIẾT ĐƠN ỨNG TUYỂN
    // =========================================================================

    @Transactional(readOnly = true)
    public Map<String, Object> chiTiet(Integer ctvId) {
        CongTacVien ctv = congTacVienRepository.findById(ctvId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ ứng viên."));
        TaiKhoan tk = ctv.getTaiKhoan();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ctv.getId());
        m.put("ma", ctv.getMaCongTacVien());
        m.put("hoTen", ctv.getHoTen());
        m.put("ngaySinh", ctv.getNgaySinh() != null ? ctv.getNgaySinh().format(NGAY) : null);
        m.put("tuoi", ctv.getNgaySinh() != null ? Period.between(ctv.getNgaySinh(), LocalDate.now()).getYears() : null);
        m.put("gioiTinh", switch (ctv.getGioiTinh() == null ? "" : ctv.getGioiTinh()) {
            case "Nam" -> "Nam";
            case "Nu" -> "Nữ";
            default -> "Khác";
        });
        m.put("soDienThoai", ctv.getSoDienThoai());
        m.put("email", tk != null && tk.getEmail() != null && !tk.getEmail().endsWith(".local") ? tk.getEmail() : null);
        m.put("tenDangNhap", tk != null ? tk.getTenDangNhap() : null);
        m.put("diaChiHienTai", ctv.getNoiCuTru());
        m.put("ngayDangKy", ctv.getNgayDangKy() != null ? ctv.getNgayDangKy().format(NGAY) : null);
        m.put("trangThai", ctv.getTrangThai());
        m.put("capDo", ctv.getCapDo());

        m.put("khuVuc", khuVucCTVRepository.findByCongTacVien_Id(ctvId).stream()
                .filter(k -> k.getKhuVuc() != null)
                .map(k -> k.getKhuVuc().getTenKhuVuc() + ", " + k.getKhuVuc().getTinhThanh())
                .distinct().toList());
        m.put("dichVu", dichVuCTVRepository.findByCongTacVien_Id(ctvId).stream()
                .filter(d -> d.getDichVu() != null)
                .map(d -> d.getDichVu().getTenDichVu())
                .distinct().sorted().toList());
        m.put("chungChi", chungChiCTVRepository.findByCongTacVien_Id(ctvId).stream().map(c -> {
            Map<String, Object> cc = new LinkedHashMap<>();
            cc.put("ten", c.getTenChungChi() != null ? c.getTenChungChi() : c.getLoaiChungChi());
            cc.put("noiCap", c.getNoiCap());
            cc.put("ngayCap", c.getNgayCap() != null ? c.getNgayCap().format(NGAY) : null);
            return cc;
        }).toList());
        m.put("soTaiLieu", hoSoCTVRepository.findByCongTacVien_Id(ctvId).size()
                + chungChiCTVRepository.findByCongTacVien_Id(ctvId).stream().filter(c -> c.getDuongDanFile() != null).count());

        LichPhongVan lich = lichPhongVan(List.of(ctv)).get(ctvId);
        if (lich != null) {
            m.put("phongVan", Map.of(
                    "thoiGian", lich.thoiGian(),
                    "noiDung", lich.noiDung(),
                    "guiLuc", lich.guiLuc().format(GIO_NGAY),
                    "coTheDoiLich", lich.coTheDoiLich()
            ));
        }
        m.put("lichSu", tk == null ? List.of() : nhatKyTaiKhoanRepository.findByTaiKhoan_IdOrderByThoiGianDesc(tk.getId())
                .stream().limit(10)
                .map(n -> Map.of("thoiGian", n.getThoiGian() != null ? n.getThoiGian().format(GIO_NGAY) : "",
                        "hanhDong", n.getHanhDong()))
                .toList());
        return m;
    }

    private void ghiNhatKy(TaiKhoan tk, String hanhDong, String ip, String thietBi) {
        NhatKyTaiKhoan log = new NhatKyTaiKhoan();
        log.setTaiKhoan(tk);
        log.setHanhDong(hanhDong.length() > 100 ? hanhDong.substring(0, 100) : hanhDong);
        log.setDiaChiIP(ip);
        log.setThietBi(thietBi);
        log.setKetQua("ThanhCong");
        nhatKyTaiKhoanRepository.save(log);
    }

    /** SMS gửi tiếng Việt không dấu (đỡ tốn ký tự, máy cũ hiển thị đúng). */
    static String boDau(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D');
    }
}
