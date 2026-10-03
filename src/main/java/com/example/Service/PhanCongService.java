package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.CongTacVien;
import com.example.Model.DichVuCTV;
import com.example.Model.DonDatDichVu;
import com.example.Model.KhuVuc;
import com.example.Model.KhuVucCTV;
import com.example.Model.LichLamViec;
import com.example.Model.LichSuTrangThaiDon;
import com.example.Model.PhanCongCTV;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.DichVuCTVRepository;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.KhuVucCTVRepository;
import com.example.Repository.LichLamViecRepository;
import com.example.Repository.LichSuTrangThaiDonRepository;
import com.example.Repository.PhanCongCTVRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Xếp lịch và phân công cộng tác viên (CTV) cho đơn đặt dịch vụ.
 *
 * THUẬT TOÁN GỢI Ý (cho một đơn):
 *  Bước 1 – Loại các CTV vi phạm ràng buộc cứng:
 *     a. CTV không ở trạng thái hoạt động, hoặc đã nhận / đã từ chối chính đơn này.
 *     b. Trùng giờ hoặc không đủ khoảng nghỉ: giữa hai việc liên tiếp của một CTV phải cách nhau
 *        ít nhất {@code app.phan-cong.khoang-cach-phut} phút (mặc định 60) để nghỉ và di chuyển.
 *     c. Quá tải trong ngày: vượt số đơn tối đa hoặc tổng số giờ làm tối đa.
 *     d. Không kịp di chuyển: việc liền trước / liền sau trong ngày ở tỉnh, thành phố khác.
 *  Bước 2 – Chấm điểm phù hợp (0–100) cho các CTV còn lại:
 *     đúng dịch vụ đã đăng ký (30) + đúng khu vực hoạt động (25, cùng tỉnh/thành 12)
 *     + còn ít việc trong ngày (20) + còn ít việc trong tuần (10) + điểm đánh giá (10) + cấp độ (5),
 *     trừ điểm nếu việc liền kề ở khu vực khác mà khoảng nghỉ ngắn.
 *  Bước 3 – Sắp xếp giảm dần theo điểm; bằng điểm thì ưu tiên người ít việc trong tuần hơn
 *     để chia việc đều, tránh dồn đơn cho một người.
 *
 * Hệ thống chưa lưu tọa độ nên thời gian di chuyển được ước lượng theo khu vực (phường/xã, tỉnh/thành).
 * Mọi thao tác phân công (CSKH phân công, CTV tự nhận) đều kiểm tra lại các ràng buộc cứng ở bước 1.
 */
@Service
public class PhanCongService {

    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PHUT_MAC_DINH = 120; // đơn không có giờ kết thúc và dịch vụ không có thời lượng

    private final DonDatDichVuRepository donDatDichVuRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final PhanCongCTVRepository phanCongCTVRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final DichVuCTVRepository dichVuCTVRepository;
    private final KhuVucCTVRepository khuVucCTVRepository;
    private final LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;

    @Value("${app.phan-cong.khoang-cach-phut:60}")
    private int khoangCachPhut;

    @Value("${app.phan-cong.toi-da-don-moi-ngay:4}")
    private int toiDaDonMoiNgay;

    @Value("${app.phan-cong.toi-da-gio-moi-ngay:8}")
    private int toiDaGioMoiNgay;

    @Value("${app.phan-cong.toi-da-don-moi-tuan:20}")
    private int toiDaDonMoiTuan;

    public PhanCongService(DonDatDichVuRepository donDatDichVuRepository,
                           CongTacVienRepository congTacVienRepository,
                           PhanCongCTVRepository phanCongCTVRepository,
                           LichLamViecRepository lichLamViecRepository,
                           DichVuCTVRepository dichVuCTVRepository,
                           KhuVucCTVRepository khuVucCTVRepository,
                           LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository) {
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.dichVuCTVRepository = dichVuCTVRepository;
        this.khuVucCTVRepository = khuVucCTVRepository;
        this.lichSuTrangThaiDonRepository = lichSuTrangThaiDonRepository;
    }

    // =========================================================================
    // MÔ HÌNH DÙNG TRONG THUẬT TOÁN
    // =========================================================================

    /** Một việc CTV đã có (lịch làm việc, hoặc phân công đang chờ xác nhận). */
    private record Viec(Integer ctvId, Integer donDatId, Integer phanCongId, LocalDate ngay,
                        LocalTime batDau, LocalTime ketThuc, KhuVuc khuVuc) {
        long phut() {
            return java.time.Duration.between(batDau, ketThuc).toMinutes();
        }
    }

    /** Kết quả kiểm tra ràng buộc cứng của một CTV với một khung giờ. */
    private static final class KiemTra {
        String lyDoLoai;                 // null = hợp lệ
        List<String> canhBao = new ArrayList<>();
        int soDonTrongNgay;
        long phutTrongNgay;
        int diemTru;
        boolean lienKeCungKhuVuc;
    }

    // =========================================================================
    // DỮ LIỆU LỊCH
    // =========================================================================

    private static LocalTime ketThucCuaDon(DonDatDichVu don) {
        if (don.getGioKetThuc() != null) {
            return don.getGioKetThuc();
        }
        int phut = 0;
        if (don.getChiTietList() != null) {
            for (ChiTietDonDat ct : don.getChiTietList()) {
                if (ct.getDichVu() != null && ct.getDichVu().getThoiGianThucHien() != null) {
                    phut += ct.getDichVu().getThoiGianThucHien() * (ct.getSoLuong() != null ? ct.getSoLuong() : 1);
                }
            }
        }
        return congPhut(don.getGioBatDau(), phut > 0 ? phut : PHUT_MAC_DINH);
    }

    /** Cộng phút nhưng không vượt qua nửa đêm (tránh giờ kết thúc quay vòng về đầu ngày). */
    private static LocalTime congPhut(LocalTime gio, long phut) {
        long tong = gio.toSecondOfDay() / 60 + phut;
        return tong >= 24 * 60 ? LocalTime.of(23, 59) : LocalTime.of((int) (tong / 60), (int) (tong % 60));
    }

    private static KhuVuc khuVucCuaDon(DonDatDichVu don) {
        return don != null && don.getDiaChi() != null ? don.getDiaChi().getKhuVuc() : null;
    }

    /** Mọi việc của tất cả CTV trong khoảng ngày, gom theo CTV. */
    /**
     * @param gomDangCho true: tính cả phân công đang chờ CTV xác nhận (khi CSKH xếp lịch, để không giao chồng);
     *                   false: chỉ tính lịch đã chốt (khi CTV tự nhận đơn – lời mời chưa nhận không chiếm lịch)
     */
    private Map<Integer, List<Viec>> taiViec(LocalDate tuNgay, LocalDate denNgay, boolean gomDangCho) {
        Map<Integer, List<Viec>> theoCtv = new HashMap<>();
        Set<Integer> phanCongCoLich = new HashSet<>();

        for (LichLamViec l : lichLamViecRepository.findLichTrongKhoang(tuNgay, denNgay)) {
            PhanCongCTV pc = l.getPhanCong();
            DonDatDichVu don = pc != null ? pc.getDonDat() : null;
            if (pc != null) {
                phanCongCoLich.add(pc.getId());
            }
            LocalTime ketThuc = l.getGioKetThuc() != null ? l.getGioKetThuc() : congPhut(l.getGioBatDau(), PHUT_MAC_DINH);
            theoCtv.computeIfAbsent(l.getCongTacVien().getId(), k -> new ArrayList<>())
                    .add(new Viec(l.getCongTacVien().getId(), don != null ? don.getId() : null,
                            pc != null ? pc.getId() : null, l.getNgayLam(), l.getGioBatDau(), ketThuc, khuVucCuaDon(don)));
        }
        // Phân công đã gửi cho CTV nhưng chưa xác nhận cũng giữ chỗ trong lịch
        if (!gomDangCho) {
            return theoCtv;
        }
        for (PhanCongCTV pc : phanCongCTVRepository.findDangChoTrongKhoang(tuNgay, denNgay)) {
            if (phanCongCoLich.contains(pc.getId())) {
                continue;
            }
            DonDatDichVu don = pc.getDonDat();
            theoCtv.computeIfAbsent(pc.getCongTacVien().getId(), k -> new ArrayList<>())
                    .add(new Viec(pc.getCongTacVien().getId(), don.getId(), pc.getId(), don.getNgayThucHien(),
                            don.getGioBatDau(), ketThucCuaDon(don), khuVucCuaDon(don)));
        }
        return theoCtv;
    }

    // =========================================================================
    // BƯỚC 1 – RÀNG BUỘC CỨNG
    // =========================================================================

    /**
     * Kiểm tra một CTV có nhận được việc [batDau, ketThuc) trong ngày hay không.
     *
     * @param viecCuaCtv      các việc CTV đã có (trong ngày đang xét hoặc rộng hơn)
     * @param boQuaPhanCongId phân công không tính (khi CTV xác nhận chính phân công đó)
     */
    private KiemTra kiemTra(List<Viec> viecCuaCtv, LocalDate ngay, LocalTime batDau, LocalTime ketThuc,
                            KhuVuc khuVucDon, Integer boQuaPhanCongId) {
        KiemTra kq = new KiemTra();
        long phutMoi = java.time.Duration.between(batDau, ketThuc).toMinutes();
        Viec truoc = null;
        Viec sau = null;

        for (Viec v : viecCuaCtv) {
            if (!ngay.equals(v.ngay()) || (boQuaPhanCongId != null && boQuaPhanCongId.equals(v.phanCongId()))) {
                continue;
            }
            kq.soDonTrongNgay++;
            kq.phutTrongNgay += v.phut();

            // b. Trùng giờ / không đủ khoảng nghỉ
            boolean trungGio = batDau.isBefore(v.ketThuc()) && v.batDau().isBefore(ketThuc);
            if (trungGio) {
                kq.lyDoLoai = "Trùng giờ với việc " + v.batDau().format(GIO) + "–" + v.ketThuc().format(GIO);
                return kq;
            }
            long cachTruoc = java.time.Duration.between(v.ketThuc(), batDau).toMinutes(); // v kết thúc trước việc mới
            long cachSau = java.time.Duration.between(ketThuc, v.batDau()).toMinutes();   // v bắt đầu sau việc mới
            long cach = cachTruoc >= 0 ? cachTruoc : cachSau;
            if (cach < khoangCachPhut) {
                kq.lyDoLoai = "Chỉ cách việc " + v.batDau().format(GIO) + "–" + v.ketThuc().format(GIO)
                        + " " + cach + " phút (cần ít nhất " + khoangCachPhut + " phút)";
                return kq;
            }
            if (cachTruoc >= 0 && (truoc == null || v.ketThuc().isAfter(truoc.ketThuc()))) {
                truoc = v;
            }
            if (cachSau >= 0 && (sau == null || v.batDau().isBefore(sau.batDau()))) {
                sau = v;
            }
        }

        // c. Quá tải trong ngày
        if (kq.soDonTrongNgay + 1 > toiDaDonMoiNgay) {
            kq.lyDoLoai = "Đã có " + kq.soDonTrongNgay + " việc trong ngày (tối đa " + toiDaDonMoiNgay + ")";
            return kq;
        }
        if (kq.phutTrongNgay + phutMoi > toiDaGioMoiNgay * 60L) {
            kq.lyDoLoai = "Tổng giờ làm trong ngày sẽ vượt " + toiDaGioMoiNgay + " giờ";
            return kq;
        }

        // d. Di chuyển giữa các việc liền kề
        for (Viec lienKe : new Viec[]{truoc, sau}) {
            if (lienKe == null || lienKe.khuVuc() == null || khuVucDon == null) {
                continue;
            }
            long cach = lienKe == truoc
                    ? java.time.Duration.between(lienKe.ketThuc(), batDau).toMinutes()
                    : java.time.Duration.between(ketThuc, lienKe.batDau()).toMinutes();
            if (!cungTinh(lienKe.khuVuc(), khuVucDon)) {
                kq.lyDoLoai = "Việc liền kề lúc " + lienKe.batDau().format(GIO) + " ở " + lienKe.khuVuc().getTinhThanh()
                        + ", không kịp di chuyển sang tỉnh, thành phố khác";
                return kq;
            }
            if (lienKe.khuVuc().getId().equals(khuVucDon.getId())) {
                kq.lienKeCungKhuVuc = true;
            } else if (cach < khoangCachPhut + 30) {
                kq.diemTru += 8;
                kq.canhBao.add("Việc liền kề ở " + lienKe.khuVuc().getTenKhuVuc() + ", chỉ cách " + cach + " phút để di chuyển");
            }
        }
        return kq;
    }

    private static boolean cungTinh(KhuVuc a, KhuVuc b) {
        return a.getTinhThanh() != null && b.getTinhThanh() != null
                && a.getTinhThanh().trim().equalsIgnoreCase(b.getTinhThanh().trim());
    }

    /**
     * Dùng khi lưu phân công hoặc khi CTV tự nhận đơn: ném lỗi nếu khung giờ vi phạm ràng buộc cứng.
     */
    @Transactional(readOnly = true)
    public void kiemTraLichHoacBaoLoi(Integer ctvId, LocalDate ngay, LocalTime batDau, LocalTime ketThuc,
                                      KhuVuc khuVucDon, Integer boQuaPhanCongId) {
        kiemTraLichHoacBaoLoi(ctvId, ngay, batDau, ketThuc, khuVucDon, boQuaPhanCongId, true);
    }

    @Transactional(readOnly = true)
    public void kiemTraLichHoacBaoLoi(Integer ctvId, LocalDate ngay, LocalTime batDau, LocalTime ketThuc,
                                      KhuVuc khuVucDon, Integer boQuaPhanCongId, boolean gomDangCho) {
        if (ketThuc == null) {
            ketThuc = congPhut(batDau, PHUT_MAC_DINH);
        }
        if (!ketThuc.isAfter(batDau)) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        List<Viec> viec = taiViec(ngay, ngay, gomDangCho).getOrDefault(ctvId, List.of());
        KiemTra kq = kiemTra(viec, ngay, batDau, ketThuc, khuVucDon, boQuaPhanCongId);
        if (kq.lyDoLoai != null) {
            throw new IllegalArgumentException("Không thể xếp lịch: " + kq.lyDoLoai + ".");
        }
    }

    // =========================================================================
    // BƯỚC 2 + 3 – GỢI Ý
    // =========================================================================

    /** Danh sách gợi ý CTV cho một đơn: người phù hợp (đã xếp hạng) và người không xếp được kèm lý do. */
    @Transactional(readOnly = true)
    public Map<String, Object> goiY(Integer donDatId) {
        DonDatDichVu don = donDatDichVuRepository.findByIdWithDetails(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt dịch vụ."));

        LocalDate ngay = don.getNgayThucHien();
        LocalTime batDau = don.getGioBatDau();
        LocalTime ketThuc = ketThucCuaDon(don);
        KhuVuc khuVucDon = khuVucCuaDon(don);
        LocalDate dauTuan = ngay.with(DayOfWeek.MONDAY);
        LocalDate cuoiTuan = dauTuan.plusDays(6);

        Set<Integer> dichVuCuaDon = new HashSet<>();
        int soNguoiCan = 1;
        if (don.getChiTietList() != null) {
            for (ChiTietDonDat ct : don.getChiTietList()) {
                if (ct.getDichVu() != null) {
                    dichVuCuaDon.add(ct.getDichVu().getId());
                    if (ct.getDichVu().getSoNguoiThucHien() != null) {
                        soNguoiCan = Math.max(soNguoiCan, ct.getDichVu().getSoNguoiThucHien());
                    }
                }
            }
        }

        // Phân công hiện có của đơn này
        Map<Integer, PhanCongCTV> phanCongCuaDon = new HashMap<>();
        List<Map<String, Object>> daPhanCong = new ArrayList<>();
        for (PhanCongCTV pc : phanCongCTVRepository.findAllByDonDat_Id(donDatId)) {
            phanCongCuaDon.put(pc.getCongTacVien().getId(), pc);
            if (!"TuChoi".equalsIgnoreCase(pc.getTrangThai())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("phanCongId", pc.getId());
                m.put("ctvId", pc.getCongTacVien().getId());
                m.put("hoTen", pc.getCongTacVien().getHoTen());
                m.put("soDienThoai", pc.getCongTacVien().getSoDienThoai());
                m.put("trangThai", pc.getTrangThai());
                daPhanCong.add(m);
            }
        }

        // Dịch vụ và khu vực mỗi CTV đăng ký
        Map<Integer, Set<Integer>> dichVuTheoCtv = new HashMap<>();
        for (DichVuCTV dv : dichVuCTVRepository.findAll()) {
            if ("HoatDong".equalsIgnoreCase(dv.getTrangThai()) && dv.getDichVu() != null) {
                dichVuTheoCtv.computeIfAbsent(dv.getCongTacVien().getId(), k -> new HashSet<>()).add(dv.getDichVu().getId());
            }
        }
        Map<Integer, List<KhuVuc>> khuVucTheoCtv = new HashMap<>();
        for (KhuVucCTV kv : khuVucCTVRepository.findAll()) {
            if ("HoatDong".equalsIgnoreCase(kv.getTrangThai()) && kv.getKhuVuc() != null) {
                khuVucTheoCtv.computeIfAbsent(kv.getCongTacVien().getId(), k -> new ArrayList<>()).add(kv.getKhuVuc());
            }
        }

        Map<Integer, List<Viec>> viecTrongTuan = taiViec(dauTuan, cuoiTuan, true);

        List<Map<String, Object>> phuHop = new ArrayList<>();
        List<Map<String, Object>> khongPhuHop = new ArrayList<>();

        for (CongTacVien ctv : congTacVienRepository.findAll()) {
            if (!"HoatDong".equalsIgnoreCase(ctv.getTrangThai())) {
                continue; // chưa duyệt / bị đình chỉ: không đưa vào danh sách
            }
            List<Viec> viec = viecTrongTuan.getOrDefault(ctv.getId(), List.of());
            int soDonTrongTuan = viec.size();

            Map<String, Object> dong = new LinkedHashMap<>();
            dong.put("ctvId", ctv.getId());
            dong.put("hoTen", ctv.getHoTen());
            dong.put("soDienThoai", ctv.getSoDienThoai());
            dong.put("capDo", ctv.getCapDo());
            dong.put("diemDanhGia", ctv.getDiemDanhGia());
            dong.put("soDonTrongTuan", soDonTrongTuan);

            List<String> lichTrongNgay = new ArrayList<>();
            viec.stream().filter(v -> ngay.equals(v.ngay())).sorted(Comparator.comparing(Viec::batDau))
                    .forEach(v -> lichTrongNgay.add(v.batDau().format(GIO) + "–" + v.ketThuc().format(GIO)
                            + (v.khuVuc() != null ? " " + v.khuVuc().getTenKhuVuc() : "")));
            dong.put("lichTrongNgay", lichTrongNgay);

            // ---- Bước 1: ràng buộc cứng ----
            PhanCongCTV daCo = phanCongCuaDon.get(ctv.getId());
            String lyDoLoai = null;
            KiemTra kq = null;
            if (daCo != null) {
                lyDoLoai = "TuChoi".equalsIgnoreCase(daCo.getTrangThai())
                        ? "Đã từ chối đơn này" : "Đã được phân công cho đơn này";
            } else {
                kq = kiemTra(viec, ngay, batDau, ketThuc, khuVucDon, null);
                lyDoLoai = kq.lyDoLoai;
                dong.put("soDonTrongNgay", kq.soDonTrongNgay);
                dong.put("gioTrongNgay", Math.round(kq.phutTrongNgay / 6.0) / 10.0);
            }
            if (lyDoLoai == null && soDonTrongTuan + 1 > toiDaDonMoiTuan) {
                lyDoLoai = "Đã có " + soDonTrongTuan + " việc trong tuần (tối đa " + toiDaDonMoiTuan + ")";
            }
            if (lyDoLoai != null) {
                dong.put("lyDoLoai", lyDoLoai);
                khongPhuHop.add(dong);
                continue;
            }

            // ---- Bước 2: chấm điểm ----
            List<String> lyDo = new ArrayList<>();
            List<String> canhBao = new ArrayList<>(kq.canhBao);
            double diem = 0;

            Set<Integer> dichVuCtv = dichVuTheoCtv.getOrDefault(ctv.getId(), Set.of());
            if (dichVuCuaDon.isEmpty() || dichVuCtv.containsAll(dichVuCuaDon)) {
                diem += 30;
                lyDo.add("Đã đăng ký dịch vụ này");
            } else if (dichVuCuaDon.stream().anyMatch(dichVuCtv::contains)) {
                diem += 15;
                canhBao.add("Chỉ đăng ký một phần dịch vụ của đơn");
            } else {
                canhBao.add("Chưa đăng ký dịch vụ này");
            }

            List<KhuVuc> khuVucCtv = khuVucTheoCtv.getOrDefault(ctv.getId(), List.of());
            if (khuVucDon == null) {
                diem += 12;
            } else if (khuVucCtv.stream().anyMatch(kv -> kv.getId().equals(khuVucDon.getId()))) {
                diem += 25;
                lyDo.add("Hoạt động tại " + khuVucDon.getTenKhuVuc());
            } else if (khuVucCtv.stream().anyMatch(kv -> cungTinh(kv, khuVucDon))) {
                diem += 12;
                lyDo.add("Cùng " + khuVucDon.getTinhThanh());
            } else {
                canhBao.add("Ngoài khu vực hoạt động đã đăng ký");
            }

            diem += 20.0 * (1 - (double) kq.soDonTrongNgay / toiDaDonMoiNgay);
            lyDo.add(kq.soDonTrongNgay == 0 ? "Chưa có việc trong ngày" : "Đang có " + kq.soDonTrongNgay + " việc trong ngày");
            diem += 10.0 * (1 - Math.min(1.0, (double) soDonTrongTuan / toiDaDonMoiTuan));

            double danhGia = ctv.getDiemDanhGia() != null ? ctv.getDiemDanhGia().doubleValue() : 0;
            diem += 10.0 * Math.min(1.0, danhGia / 5.0);
            if (danhGia >= 4.5) {
                lyDo.add("Đánh giá " + ctv.getDiemDanhGia().stripTrailingZeros().toPlainString() + "/5");
            }
            diem += "UuTu".equalsIgnoreCase(ctv.getCapDo()) ? 5 : ("Thuong".equalsIgnoreCase(ctv.getCapDo()) ? 3 : 0);

            if (kq.lienKeCungKhuVuc) {
                lyDo.add("Việc liền kề cùng khu vực, không phải di chuyển xa");
            }
            diem -= kq.diemTru;

            dong.put("diem", (int) Math.round(Math.max(0, Math.min(100, diem))));
            dong.put("lyDo", lyDo);
            dong.put("canhBao", canhBao);
            phuHop.add(dong);
        }

        // ---- Bước 3: xếp hạng ----
        phuHop.sort(Comparator.<Map<String, Object>, Integer>comparing(m -> (Integer) m.get("diem")).reversed()
                .thenComparing(m -> (Integer) m.get("soDonTrongTuan"))
                .thenComparing(m -> (Integer) m.get("ctvId")));

        Map<String, Object> ketQua = new LinkedHashMap<>();
        ketQua.put("donDatId", don.getId());
        ketQua.put("maDonDat", don.getMaDonDat());
        ketQua.put("trangThai", don.getTrangThai());
        ketQua.put("ngay", ngay.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        ketQua.put("gio", batDau.format(GIO) + "–" + ketThuc.format(GIO));
        ketQua.put("khuVuc", khuVucDon != null ? khuVucDon.getTenKhuVuc() + ", " + khuVucDon.getTinhThanh() : "");
        ketQua.put("soNguoiCan", soNguoiCan);
        ketQua.put("khoangCachPhut", khoangCachPhut);
        ketQua.put("toiDaDonMoiNgay", toiDaDonMoiNgay);
        ketQua.put("toiDaGioMoiNgay", toiDaGioMoiNgay);
        ketQua.put("daPhanCong", daPhanCong);
        ketQua.put("phuHop", phuHop);
        ketQua.put("khongPhuHop", khongPhuHop);
        return ketQua;
    }

    // =========================================================================
    // PHÂN CÔNG THỦ CÔNG
    // =========================================================================

    /** CSKH phân công một CTV cho đơn (giờ làm lấy theo đơn). Kiểm tra lại mọi ràng buộc cứng trước khi lưu. */
    @Transactional
    public PhanCongCTV phanCong(Integer donDatId, Integer ctvId, String nguoiThucHien) {
        // Khóa CTV rồi tới đơn: hai nhân viên phân công cùng lúc được xử lý lần lượt
        CongTacVien ctv = congTacVienRepository.findByIdForUpdate(ctvId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên."));
        DonDatDichVu don = donDatDichVuRepository.findByIdForUpdate(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt dịch vụ."));

        if (!"HoatDong".equalsIgnoreCase(ctv.getTrangThai())) {
            throw new IllegalArgumentException("Cộng tác viên này chưa được duyệt hoặc đang bị đình chỉ.");
        }
        if (!"ChoDuyet".equalsIgnoreCase(don.getTrangThai()) && !"DaXacNhan".equalsIgnoreCase(don.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ phân công được cho đơn đang chờ duyệt hoặc đã xác nhận.");
        }
        PhanCongCTV daCo = phanCongCTVRepository.findByDonDat_IdAndCongTacVien_Id(donDatId, ctvId).orElse(null);
        if (daCo != null) {
            throw new IllegalArgumentException("TuChoi".equalsIgnoreCase(daCo.getTrangThai())
                    ? "Cộng tác viên này đã từ chối đơn này." : "Cộng tác viên này đã được phân công cho đơn này.");
        }

        LocalTime ketThuc = ketThucCuaDon(don);
        kiemTraLichHoacBaoLoi(ctvId, don.getNgayThucHien(), don.getGioBatDau(), ketThuc, khuVucCuaDon(don), null);

        PhanCongCTV pc = phanCongCTVRepository.save(PhanCongCTV.builder()
                .maPhanCong(MaSinh.tao("PC-"))
                .donDat(don)
                .congTacVien(ctv)
                .trangThai("DaXacNhan")
                .thoiGianPhanCong(LocalDateTime.now())
                .thoiGianXacNhan(LocalDateTime.now())
                .build());

        lichLamViecRepository.save(LichLamViec.builder()
                .maLichLamViec(MaSinh.tao("LLV-"))
                .phanCong(pc)
                .congTacVien(ctv)
                .ngayLam(don.getNgayThucHien())
                .gioBatDau(don.getGioBatDau())
                .gioKetThuc(ketThuc)
                .trangThai("SapToi")
                .build());

        String trangThaiCu = don.getTrangThai();
        if (!"DaXacNhan".equalsIgnoreCase(trangThaiCu)) {
            don.setTrangThai("DaXacNhan");
            donDatDichVuRepository.save(don);
        }
        ghiLichSu(don, trangThaiCu, don.getTrangThai(), nguoiThucHien, "Phân công cộng tác viên " + ctv.getHoTen());
        return pc;
    }

    /** Bỏ một phân công chưa thực hiện; đơn không còn ai phụ trách thì quay về "Chờ duyệt". */
    @Transactional
    public void boPhanCong(Integer donDatId, Integer phanCongId, String nguoiThucHien) {
        // Khóa phân công trước rồi tới đơn (cùng thứ tự với lúc CTV xác nhận/từ chối) để không khóa chéo nhau
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Phân công không còn tồn tại (có thể đã được người khác bỏ)."));
        DonDatDichVu don = donDatDichVuRepository.findByIdForUpdate(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt dịch vụ."));
        if (!pc.getDonDat().getId().equals(donDatId)) {
            throw new IllegalArgumentException("Phân công không thuộc đơn này.");
        }
        if ("DangThucHien".equalsIgnoreCase(don.getTrangThai()) || "HoanThanh".equalsIgnoreCase(don.getTrangThai())
                || "HoanThanh".equalsIgnoreCase(pc.getTrangThai())) {
            throw new IllegalArgumentException("Đơn đang thực hiện hoặc đã hoàn thành, không bỏ phân công được.");
        }

        String ten = pc.getCongTacVien().getHoTen();
        lichLamViecRepository.deleteAll(lichLamViecRepository.findByPhanCong_Id(phanCongId));
        phanCongCTVRepository.delete(pc);
        phanCongCTVRepository.flush();

        boolean conNguoi = phanCongCTVRepository.findAllByDonDat_Id(donDatId).stream()
                .anyMatch(p -> !"TuChoi".equalsIgnoreCase(p.getTrangThai()));
        String trangThaiCu = don.getTrangThai();
        if (!conNguoi && "DaXacNhan".equalsIgnoreCase(trangThaiCu)) {
            don.setTrangThai("ChoDuyet");
            donDatDichVuRepository.save(don);
        }
        ghiLichSu(don, trangThaiCu, don.getTrangThai(), nguoiThucHien, "Bỏ phân công cộng tác viên " + ten);
    }

    private void ghiLichSu(DonDatDichVu don, String cu, String moi, String nguoi, String ghiChu) {
        String ten = nguoi == null || nguoi.isBlank() ? "CSKH" : nguoi;
        lichSuTrangThaiDonRepository.save(LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu(cu)
                .trangThaiMoi(moi)
                .nguoiThucHien(ten.length() > 50 ? ten.substring(0, 50) : ten)
                .thoiGian(LocalDateTime.now())
                .ghiChu(ghiChu)
                .build());
    }
}
