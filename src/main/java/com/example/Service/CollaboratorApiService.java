package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CollaboratorApiService {

    @org.springframework.beans.factory.annotation.Autowired
    private PhanCongService phanCongService;

    @org.springframework.beans.factory.annotation.Autowired
    private KhuVucCtvService khuVucCtvService;

    @org.springframework.beans.factory.annotation.Autowired
    private KhuVucService khuVucService;
    private final PhanCongCTVRepository phanCongCTVRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private HanNhanDonService hanNhanDonService;
    private final DonDatDichVuRepository donDatDichVuRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ViTriCtvStore viTriCtvStore;
    @org.springframework.beans.factory.annotation.Autowired
    private TamQuanHcm tamQuanHcm;

    public CollaboratorApiService(
            PhanCongCTVRepository phanCongCTVRepository,
            DonDatDichVuRepository donDatDichVuRepository,
            LichLamViecRepository lichLamViecRepository,
            CongTacVienRepository congTacVienRepository,
            LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository,
            ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
            ThongBaoRepository thongBaoRepository,
            TaiKhoanRepository taiKhoanRepository,
            KhachHangRepository khachHangRepository) {
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.lichSuTrangThaiDonRepository = lichSuTrangThaiDonRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
    }

    private static final Comparator<LichLamViec> CHRONO_LLV_COMPARATOR = Comparator
            .comparing((LichLamViec l) -> l.getNgayLam() != null ? l.getNgayLam() : LocalDate.MAX)
            .thenComparing(l -> l.getGioBatDau() != null ? l.getGioBatDau() : LocalTime.MIN)
            .thenComparing(LichLamViec::getId);

    private int tinhBuoiThu(LichLamViec llv, int fallbackIndex) {
        if (llv != null && llv.getMaLichLamViec() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("-B(\\d+)$").matcher(llv.getMaLichLamViec());
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }
        return fallbackIndex >= 0 ? fallbackIndex + 1 : 1;
    }

    // ==========================================
    // UC-CTV01: QUẢN LÝ ĐƠN ĐƯỢC PHÂN CÔNG
    // ==========================================
    public List<Map<String, Object>> getAssignments(Integer congTacVienId, String trangThai) {
        List<PhanCongCTV> list;
        if (trangThai != null && !trangThai.isBlank()) {
            list = phanCongCTVRepository.findByCongTacVien_IdAndTrangThaiOrderByThoiGianPhanCongDesc(congTacVienId, trangThai);
        } else {
            list = phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(congTacVienId);
        }

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PhanCongCTV pc : list) {
            DonDatDichVu don = pc.getDonDat();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("phanCongId", pc.getId());
            item.put("maPhanCong", pc.getMaPhanCong());
            item.put("trangThaiPhanCong", pc.getTrangThai());
            item.put("trangThaiDon", don.getTrangThai());
            item.put("trangThai", don.getTrangThai());
            item.put("thoiGianPhanCong", pc.getThoiGianPhanCong());
            item.put("donDatId", don.getId());
            item.put("maDonDat", don.getMaDonDat());
            String tenDichVu = DatNhieuDichVu.tenDichVuCuaDon(don, "");
            item.put("tenDichVu", tenDichVu);

            List<LichLamViec> sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_Id(pc.getId()));
            if (sches.isEmpty()) {
                sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()));
            }
            sches.sort(CHRONO_LLV_COMPARATOR);

            boolean isGoi = "GoiThang".equalsIgnoreCase(don.getLoaiHinhDat()) || sches.size() > 1;
            int soBuoi = isGoi ? Math.max(sches.size(), LichGoiThang.soBuoi(null, tenDichVu)) : 1;

            LichLamViec targetLlv = null;
            int targetIdx = -1;
            // Ưu tiên tìm buổi quá hạn chưa hoàn thành trước, nếu không có thì lấy buổi sắp tới gần nhất theo ngày
            for (int i = 0; i < sches.size(); i++) {
                LichLamViec l = sches.get(i);
                String st = l.getTrangThai() != null ? l.getTrangThai() : "";
                if (!"HoanThanh".equalsIgnoreCase(st) && !"DaHuy".equalsIgnoreCase(st) && !"Huy".equalsIgnoreCase(st)) {
                    if (l.getNgayLam() != null && l.getNgayLam().isBefore(today)) {
                        targetLlv = l;
                        targetIdx = i;
                        break;
                    }
                    if (targetLlv == null) {
                        targetLlv = l;
                        targetIdx = i;
                    }
                }
            }
            if (targetLlv == null && !sches.isEmpty()) {
                targetLlv = sches.get(0);
                targetIdx = 0;
            }

            LichLamViec nextLlv = null;
            if (isGoi && targetIdx >= 0) {
                for (int j = targetIdx + 1; j < sches.size(); j++) {
                    LichLamViec cand = sches.get(j);
                    String st = cand.getTrangThai() != null ? cand.getTrangThai() : "";
                    if (!"DaHuy".equalsIgnoreCase(st) && !"Huy".equalsIgnoreCase(st)) {
                        nextLlv = cand;
                        break;
                    }
                }
            }

            int buoiThu = tinhBuoiThu(targetLlv, targetIdx);
            long soBuoiConLai = sches.stream()
                    .filter(s -> !"HoanThanh".equalsIgnoreCase(s.getTrangThai())
                            && !"DaHuy".equalsIgnoreCase(s.getTrangThai())
                            && !"Huy".equalsIgnoreCase(s.getTrangThai()))
                    .count();

            boolean isBuoiLamBu = false;
            if (targetLlv != null && targetLlv.getNgayLam() != null && isGoi) {
                for (LichLamViec other : sches) {
                    if (!other.getId().equals(targetLlv.getId()) && other.getNgayLam() != null) {
                        int otherB = tinhBuoiThu(other, -1);
                        if (buoiThu < otherB && targetLlv.getNgayLam().isAfter(other.getNgayLam())) {
                            isBuoiLamBu = true;
                            break;
                        }
                    }
                }
            }

            item.put("lichLamViecId", targetLlv != null ? targetLlv.getId() : null);
            item.put("ngayThucHien", targetLlv != null && targetLlv.getNgayLam() != null ? targetLlv.getNgayLam() : don.getNgayThucHien());
            item.put("gioBatDau", targetLlv != null && targetLlv.getGioBatDau() != null ? targetLlv.getGioBatDau() : don.getGioBatDau());
            item.put("gioKetThuc", targetLlv != null && targetLlv.getGioKetThuc() != null ? targetLlv.getGioKetThuc() : don.getGioKetThuc());
            item.put("isGoiThang", isGoi);
            item.put("soBuoi", soBuoi);
            item.put("buoiThu", buoiThu);
            item.put("soBuoiConLai", soBuoiConLai);
            item.put("isBuoiLamBu", isBuoiLamBu);
            item.put("ngayBuoiTiepTheo", nextLlv != null ? nextLlv.getNgayLam() : null);
            item.put("gioBuoiTiepTheo", nextLlv != null ? nextLlv.getGioBatDau() : null);

            item.put("diaChi", CustomerAddressService.diaChiDayDu(don.getDiaChi()));
            item.put("khuVuc", don.getDiaChi() != null && don.getDiaChi().getKhuVuc() != null ? don.getDiaChi().getKhuVuc().getTenKhuVuc() : "");
            item.put("khachHangTen", don.getKhachHang().getHoTen());
            item.put("khachHangPhone", don.getKhachHang().getSoDienThoai());
            item.put("yeuCauDacBiet", don.getYeuCauDacBiet());
            item.put("thanhTien", don.getThanhTien());
            result.add(item);
        }
        return result;
    }

    /**
     * CTV gửi vị trí GPS từ ứng dụng (5 giây/lần), kèm quận/huyện app tra từ toạ độ.
     * - Mỗi lần: chỉ lưu vị trí lên Redis (ViTriCtvStore), không ghi CSDL.
     * - Quận lưu trên Redis là tên khu vực đã khớp (bảng KhuVuc). Khi CTV sang quận khác
     *   (VD Quận Bình Thạnh → Quận Tân Phú) mới ghi CSDL: thay toàn bộ khu vực nhận việc (KhuVucCTV)
     *   bằng đúng quận đang đứng. Không tra được quận thì giữ quận cũ trên Redis, không đổi gì.
     */
    @Transactional
    public Map<String, Object> updateLocation(Integer congTacVienId, java.math.BigDecimal viDo, java.math.BigDecimal kinhDo,
                                              String quanHuyen, String tinhThanh, String diaChi) {
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId).orElse(null);
        if (ctv == null) {
            TaiKhoan tk = taiKhoanRepository.findById(congTacVienId).orElse(null);
            if (tk != null) {
                ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
            }
        }
        if (ctv == null) {
            throw new IllegalArgumentException("Không tìm thấy cộng tác viên ID: " + congTacVienId);
        }
        final Integer resolvedCtvId = ctv.getId();

        // 1. Xác định quận: Ưu tiên toạ độ GPS chính xác (tamQuanHcm) để tránh lỗi geocoder
        //    trả về "Thành phố Hồ Chí Minh" hoặc tên phường mới gây lệch quận; sau đó mới tới quanHuyen và diaChi.
        Optional<KhuVuc> kvOpt = Optional.empty();
        if (khuVucService != null) {
            if (tamQuanHcm != null && viDo != null && kinhDo != null) {
                String quanTheoToaDo = tamQuanHcm.quanCua(viDo.doubleValue(), kinhDo.doubleValue());
                if (quanTheoToaDo != null) {
                    kvOpt = khuVucService.timTheoTen(quanTheoToaDo);
                }
            }
            if (kvOpt.isEmpty() && quanHuyen != null && !quanHuyen.isBlank()) {
                kvOpt = khuVucService.timTheoTen(quanHuyen);
            }
            if (kvOpt.isEmpty() && diaChi != null && !diaChi.isBlank()) {
                kvOpt = khuVucService.doanQuan(diaChi, khuVucService.danhSach());
            }
        }

        // 2. Quận lần trước trên Redis (đọc trước khi ghi đè)
        String quanTruoc = viTriCtvStore.get(resolvedCtvId).map(ViTriCtvStore.ViTri::quanHuyen).orElse("");
        String quanMoi = kvOpt.map(KhuVuc::getTenKhuVuc).orElse("");

        // 3. Lưu vị trí lên Redis; không tra được quận thì giữ quận cũ để lần sau còn so sánh
        ViTriCtvStore.ViTri vt = viTriCtvStore.update(resolvedCtvId, viDo, kinhDo,
                quanMoi.isBlank() ? quanTruoc : quanMoi, tinhThanh, diaChi);

        // 4. Chỉ đọc / ghi CSDL khi sang quận khác (hoặc lần đầu ghi nhận quận)
        boolean daDongBoDB = false;
        if (kvOpt.isPresent() && khuVucCtvService != null && quanDaDoi(quanTruoc, quanMoi)) {
            Integer idMoi = kvOpt.get().getId();
            List<Integer> dangNhan = khuVucCtvService.dangNhan(resolvedCtvId).stream().map(KhuVuc::getId).toList();
            if (canGhiKhuVuc(dangNhan, idMoi)) {
                khuVucCtvService.capNhat(resolvedCtvId, List.of(idMoi));
                daDongBoDB = true;
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("congTacVienId", resolvedCtvId);
        result.put("viDo", vt.viDo());
        result.put("kinhDo", vt.kinhDo());
        result.put("quanHuyen", vt.quanHuyen());
        result.put("tinhThanh", vt.tinhThanh());
        result.put("khuVucHienTai", vt.quanHuyen());
        result.put("dongBoDatabase", daDongBoDB);
        result.put("thoiGianCapNhatViTri", vt.thoiGian());
        return result;
    }

    /** Có quận mới và khác quận lần trước (lần đầu chưa có quận trước cũng tính là đổi). */
    static boolean quanDaDoi(String quanTruoc, String quanMoi) {
        if (quanMoi == null || quanMoi.isBlank()) return false;
        if (quanTruoc == null || quanTruoc.isBlank()) return true;
        return !quanTruoc.trim().equalsIgnoreCase(quanMoi.trim());
    }

    /**
     * Khu vực nhận việc chỉ theo GPS: thay toàn bộ bằng quận đang đứng, trừ khi danh sách đã đúng là quận đó.
     */
    static boolean canGhiKhuVuc(List<Integer> dangNhan, Integer idMoi) {
        return !(dangNhan.size() == 1 && dangNhan.contains(idMoi));
    }

    /** Overload tương thích ngược */
    @Transactional
    public Map<String, Object> updateLocation(Integer congTacVienId, java.math.BigDecimal viDo, java.math.BigDecimal kinhDo) {
        return updateLocation(congTacVienId, viDo, kinhDo, "", "", "");
    }

    public Map<String, Object> getAssignmentDetail(Integer phanCongId) {
        PhanCongCTV pc = phanCongCTVRepository.findById(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        DonDatDichVu don = pc.getDonDat();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("phanCongId", pc.getId());
        result.put("maPhanCong", pc.getMaPhanCong());
        result.put("trangThaiPhanCong", pc.getTrangThai());
        result.put("thoiGianPhanCong", pc.getThoiGianPhanCong());
        result.put("thoiGianXacNhan", pc.getThoiGianXacNhan());
        result.put("lyDoTuChoi", pc.getLyDoTuChoi());

        result.put("donDatId", don.getId());
        result.put("maDonDat", don.getMaDonDat());
        String tenDichVu = DatNhieuDichVu.tenDichVuCuaDon(don, "");
        result.put("tenDichVu", tenDichVu);
        result.put("loaiHinhDat", don.getLoaiHinhDat());
        result.put("ngayThucHien", don.getNgayThucHien());
        result.put("gioBatDau", don.getGioBatDau());
        result.put("gioKetThuc", don.getGioKetThuc());
        result.put("yeuCauDacBiet", don.getYeuCauDacBiet());
        result.put("ghiChu", don.getGhiChu());
        result.put("thanhTien", don.getThanhTien());

        Map<String, Object> khMap = new LinkedHashMap<>();
        khMap.put("hoTen", don.getKhachHang().getHoTen());
        khMap.put("soDienThoai", don.getKhachHang().getSoDienThoai());
        khMap.put("diaChi", CustomerAddressService.diaChiDayDu(don.getDiaChi()));
            khMap.put("khuVuc", don.getDiaChi() != null && don.getDiaChi().getKhuVuc() != null ? don.getDiaChi().getKhuVuc().getTenKhuVuc() : "");
        result.put("khachHang", khMap);

        return result;
    }

    public Map<String, Object> acceptAssignment(Integer phanCongId) {
        // Khóa phân công: CTV bấm hai lần hoặc CSKH đổi phân công cùng lúc sẽ được xử lý lần lượt
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        DonDatDichVu don = pc.getDonDat();
        PhanCongService.kiemTraKhongTuPhucVu(pc.getCongTacVien(), don);
        // Đơn đã hủy (kể cả tự hủy do quá hạn) hoặc đã quá giờ bắt đầu thì không nhận được nữa
        if ("DaHuy".equalsIgnoreCase(don.getTrangThai())) {
            throw new IllegalArgumentException("Đơn này đã bị hủy.");
        }
        if (!"DaXacNhan".equalsIgnoreCase(pc.getTrangThai()) && don.getNgayThucHien() != null && don.getGioBatDau() != null
                && LocalDateTime.of(don.getNgayThucHien(), don.getGioBatDau()).isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Đơn này đã quá giờ bắt đầu, không nhận được nữa.");
        }
        // Đơn chưa có ai nhận: chỉ nhận được trong thời gian nhận đơn
        if (!"DaXacNhan".equalsIgnoreCase(pc.getTrangThai()) && phanCongCTVRepository.findAllByDonDat_Id(don.getId()).stream()
                .noneMatch(x -> "DaXacNhan".equalsIgnoreCase(x.getTrangThai()) || "HoanThanh".equalsIgnoreCase(x.getTrangThai()))) {
            hanNhanDonService.kiemTraConHan(don);
        }

        // CTV tự nhận đơn cũng phải theo quy tắc xếp lịch: không trùng giờ, cách việc khác ít nhất 1 tiếng,
        // không quá tải trong ngày. Khóa CTV để hai lần nhận đơn cùng lúc được kiểm tra lần lượt.
        if (!"DaXacNhan".equalsIgnoreCase(pc.getTrangThai())) {
            congTacVienRepository.findByIdForUpdate(pc.getCongTacVien().getId());
            // Gói tháng: kiểm tra mọi buổi trong tháng
            phanCongService.kiemTraDonHoacBaoLoi(pc.getCongTacVien().getId(), don, pc.getId(), false);
        }

        pc.setTrangThai("DaXacNhan"); 
        pc.setThoiGianXacNhan(LocalDateTime.now());
        phanCongCTVRepository.save(pc);

        String oldStatus = don.getTrangThai();
        don.setTrangThai("DaXacNhan");
        donDatDichVuRepository.save(don);

        // Lưu lịch sử trạng thái đơn
        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu(oldStatus)
                .trangThaiMoi("DaXacNhan")
                .nguoiThucHien("CongTacVien: " + pc.getCongTacVien().getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("Cộng tác viên xác nhận nhận đơn qua Mobile App")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Tạo lịch làm việc của CTV này (gói tháng: mỗi buổi một lịch)
        if (lichLamViecRepository.findByPhanCong_Id(pc.getId()).isEmpty()) {
            phanCongService.taoLichLamViec(pc, PhanCongService.cacBuoi(don), don.getGioBatDau(),
                    PhanCongService.gioKetThuc(don), null);
        }

        // Tự động gửi thông báo nhắc lịch / xác nhận ca làm cho CTV
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String tieuDe;
            String noiDung;
            String tenCTV = pc.getCongTacVien().getHoTen();
            String prefixCTV = "CTV " + tenCTV;
            String tenKhach = (don.getKhachHang() != null && don.getKhachHang().getHoTen() != null)
                    ? don.getKhachHang().getHoTen() : "Khách hàng";
            String diaChiStr = CustomerAddressService.diaChiDayDu(don.getDiaChi());
            String suffixKH = " của KH " + tenKhach + (diaChiStr.isEmpty() ? "" : " (Đ/c: " + diaChiStr + ")");

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate ngayThucHien = don.getNgayThucHien();
            LocalTime gioBatDau = don.getGioBatDau();
            String gioStr = gioBatDau != null ? gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm")) : "giờ quy định";
            String ngayStr = ngayThucHien != null ? ngayThucHien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            String thu = getThuTrongTuan(ngayThucHien);

            if (ngayThucHien != null && !ngayThucHien.isAfter(today)) {
                tieuDe = prefixCTV + " – Nhắc lịch ca làm gấp: " + don.getMaDonDat();
                noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " hôm nay (" + thu + ", " + ngayStr + "). Vui lòng nhớ có mặt trước " + gioStr + " hôm nay!";
            } else if (ngayThucHien != null && ngayThucHien.isEqual(tomorrow)) {
                tieuDe = prefixCTV + " – Nhắc lịch ca làm ngày mai: " + don.getMaDonDat();
                noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào ngày mai (" + thu + ", " + ngayStr + "). Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
            } else {
                tieuDe = prefixCTV + " – Nhắc lịch ca làm: " + don.getMaDonDat();
                noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào " + thu + ", ngày " + ngayStr + ". Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
            }

            ThongBao tbNhacLich = ThongBao.builder()
                    .maThongBao("TB-NL-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .tieuDe(tieuDe)
                    .noiDung(noiDung)
                    .nguoiGui("Hệ thống CSKH")
                    .nhomNhan("CaNhan")
                    .thoiGianGui(LocalDateTime.now())
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(tbNhacLich);

            ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                    .thongBao(tbNhacLich)
                    .taiKhoan(pc.getCongTacVien().getTaiKhoan())
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build();
            thongBaoNguoiDungRepository.save(tbnd);
        }

        // Thông báo cho khách hàng: ThongBaoSuKienService gửi theo sự kiện phân công / trạng thái đơn

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiPhanCong", pc.getTrangThai());
        res.put("donDatTrangThai", don.getTrangThai());
        res.put("message", "Đã xác nhận nhận đơn thành công. Lịch làm việc đã được cập nhật vào thời gian biểu.");
        return res;
    }

    public Map<String, Object> rejectAssignment(Integer phanCongId, AssignmentActionRequest req) {
        // Khóa phân công: CTV bấm hai lần hoặc CSKH đổi phân công cùng lúc sẽ được xử lý lần lượt
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        pc.setTrangThai("TuChoi");
        String rawLyDo = (req != null) ? req.getLyDoTuChoi() : null;
        String cleanLyDo = (rawLyDo != null) ? rawLyDo.trim().replaceAll("\\s+", " ") : "";
        String lyDo = !cleanLyDo.isEmpty() ? cleanLyDo : "Bận lịch cá nhân";
        pc.setLyDoTuChoi(lyDo);
        phanCongCTVRepository.save(pc);

        DonDatDichVu don = pc.getDonDat();
        don.setTrangThai("ChoDuyet"); // Trả lại về trạng thái chờ để HCNS phân công người khác
        donDatDichVuRepository.save(don);

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("DaPhanCong")
                .trangThaiMoi("ChoDuyet")
                .nguoiThucHien("CongTacVien: " + pc.getCongTacVien().getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV từ chối đơn. Lý do: " + lyDo)
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Tạo thông báo xác nhận đã từ chối đơn cho CTV
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String tenCTV = pc.getCongTacVien().getHoTen();
            String prefixCTV = "CTV " + tenCTV;
            ThongBao tbTuChoi = ThongBao.builder()
                    .maThongBao("TB-TC-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .tieuDe(prefixCTV + " đã từ chối đơn: " + don.getMaDonDat())
                    .noiDung(prefixCTV + " đã từ chối nhận đơn " + don.getMaDonDat() + ". Lý do: \"" + lyDo + "\". Hệ thống đã chuyển lại đơn cho phòng CSKH để điều phối nhân sự khác.")
                    .nguoiGui("Hệ thống CSKH")
                    .nhomNhan("CaNhan")
                    .thoiGianGui(LocalDateTime.now())
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(tbTuChoi);

            ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                    .thongBao(tbTuChoi)
                    .taiKhoan(pc.getCongTacVien().getTaiKhoan())
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build();
            thongBaoNguoiDungRepository.save(tbnd);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiPhanCong", "TuChoi");
        res.put("message", "Đã từ chối đơn. Hệ thống sẽ thông báo phòng HCNS để phân công lại.");
        return res;
    }

    @Transactional
    public Map<String, Object> startAssignment(Integer phanCongId) {
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        DonDatDichVu don = pc.getDonDat();
        List<LichLamViec> sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_Id(pc.getId()));
        if (sches.isEmpty()) {
            sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()));
        }
        sches.sort(CHRONO_LLV_COMPARATOR);

        LichLamViec targetLlv = null;
        for (LichLamViec llv : sches) {
            if ("SapToi".equalsIgnoreCase(llv.getTrangThai())) {
                targetLlv = llv;
                break;
            }
        }

        // RÀNG BUỘC THỜI GIAN LÀM VIỆC (Ngày & Giờ):
        LocalDate ngayLam = (targetLlv != null && targetLlv.getNgayLam() != null)
                ? targetLlv.getNgayLam()
                : don.getNgayThucHien();
        LocalTime gioBatDau = (targetLlv != null && targetLlv.getGioBatDau() != null)
                ? targetLlv.getGioBatDau()
                : (don.getGioBatDau() != null ? don.getGioBatDau() : LocalTime.of(8, 0));
        LocalTime gioKetThuc = (targetLlv != null && targetLlv.getGioKetThuc() != null)
                ? targetLlv.getGioKetThuc()
                : (don.getGioKetThuc() != null ? don.getGioKetThuc() : gioBatDau.plusHours(2));

        if (ngayLam != null && gioBatDau != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime earliestStart = LocalDateTime.of(ngayLam, gioBatDau).minusMinutes(30);
            LocalDateTime latestStart = LocalDateTime.of(ngayLam, gioKetThuc != null ? gioKetThuc : gioBatDau.plusHours(2));

            if (now.isBefore(earliestStart)) {
                String gioStr = gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm"));
                String ngayStr = ngayLam.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                throw new IllegalArgumentException("Chưa đến giờ bắt đầu ca làm việc! Ca làm bắt đầu lúc "
                        + gioStr + " ngày " + ngayStr + " (bạn chỉ có thể bắt đầu trước tối đa 30 phút).");
            }
            if (now.isAfter(latestStart)) {
                throw new IllegalArgumentException("Ca làm việc này đã quá hạn! Vui lòng thực hiện xử lý ca quá hạn với Khách hàng.");
            }
        }

        don.setTrangThai("DangThucHien");
        donDatDichVuRepository.save(don);

        if (targetLlv != null) {
            targetLlv.setTrangThai("DangThucHien");
            lichLamViecRepository.save(targetLlv);
        }

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("DaPhanCong")
                .trangThaiMoi("DangThucHien")
                .nguoiThucHien("CongTacVien: " + (pc.getCongTacVien() != null ? pc.getCongTacVien().getHoTen() : "CTV"))
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV bắt đầu thực hiện ca làm việc")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiDon", "DangThucHien");
        res.put("trangThaiPhanCong", pc.getTrangThai());
        res.put("message", "Đã bắt đầu thực hiện ca làm việc!");
        return res;
    }

    /**
     * Xử lý ca làm quá hạn sau khi CTV đã gọi điện và thống nhất với Khách hàng:
     * - START_TODAY: Làm bù ngay hôm nay (cập nhật ngayThucHien & ngayLam = hôm nay, chuyển sang DangThucHien)
     * - RESCHEDULE_KEEP: Dời sang ngày mới & CTV này vẫn nhận làm
     * - RESCHEDULE_RELEASE: Dời sang ngày mới nhưng CTV này bận (cập nhật ngayThucHien = ngày mới, trả đơn về ChoDuyet đang tìm CTV)
     * - CANCEL_ORDER: Khách hàng không còn nhu cầu, hủy đơn quá hạn (DaHuy)
     */
    @Transactional
    public Map<String, Object> resolveOverdueAssignment(Integer phanCongId, Map<String, Object> req) {
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));
        DonDatDichVu don = donDatDichVuRepository.findByIdForUpdate(pc.getDonDat().getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt dịch vụ."));

        String action = req != null && req.get("action") != null ? req.get("action").toString().trim() : "";
        String ngayMoiStr = req != null && req.get("ngayMoi") != null ? req.get("ngayMoi").toString().trim() : "";
        String gioBatDauMoiStr = req != null && req.get("gioBatDauMoi") != null ? req.get("gioBatDauMoi").toString().trim() : "";
        String rawGhiChu = req != null && req.get("ghiChu") != null ? req.get("ghiChu").toString().trim() : "";
        String cleanGhiChu = rawGhiChu.replaceAll("\\s+", " ");

        Integer reqLlvId = null;
        if (req != null && req.get("lichLamViecId") != null && !req.get("lichLamViecId").toString().isBlank()) {
            try {
                reqLlvId = Integer.valueOf(req.get("lichLamViecId").toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        LocalDate today = LocalDate.now();
        String todayStr = today.format(dateFmt);
        String tenCtv = pc.getCongTacVien() != null ? pc.getCongTacVien().getHoTen() : "CTV";
        String nguoiThucHien = "CongTacVien: " + tenCtv;
        if (nguoiThucHien.length() > 50) nguoiThucHien = nguoiThucHien.substring(0, 50);

        String oldOrderStatus = don.getTrangThai();
        List<LichLamViec> sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_Id(pc.getId()));
        if (sches.isEmpty()) {
            sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()));
        }
        // Sắp xếp theo ngày tăng dần để xử lý đúng trình tự thời gian
        sches.sort(CHRONO_LLV_COMPARATOR);

        boolean isGoiThang = "GoiThang".equalsIgnoreCase(don.getLoaiHinhDat()) || sches.size() > 1;
        if (isGoiThang && ("RESCHEDULE_RELEASE".equals(action) || "CANCEL_ORDER".equals(action))) {
            throw new IllegalArgumentException("Đơn gói dịch vụ phải do cùng 1 CTV phụ trách trọn vẹn toàn bộ các buổi, không được tách lẻ hoặc hủy ngang. Vui lòng chọn làm bù hôm nay hoặc dời ngày làm bù sau buổi cuối cùng của gói.");
        }
        LichLamViec targetLlv = null;
        int targetIdx = -1;

        if (reqLlvId != null) {
            for (int i = 0; i < sches.size(); i++) {
                if (reqLlvId.equals(sches.get(i).getId())) {
                    targetLlv = sches.get(i);
                    targetIdx = i;
                    break;
                }
            }
        }
        if (targetLlv == null) {
            for (int i = 0; i < sches.size(); i++) {
                LichLamViec l = sches.get(i);
                String st = l.getTrangThai() != null ? l.getTrangThai() : "";
                if (!"HoanThanh".equalsIgnoreCase(st) && !"DaHuy".equalsIgnoreCase(st) && !"Huy".equalsIgnoreCase(st)) {
                    if (l.getNgayLam() != null && l.getNgayLam().isBefore(today)) {
                        targetLlv = l;
                        targetIdx = i;
                        break;
                    }
                    if (targetLlv == null) {
                        targetLlv = l;
                        targetIdx = i;
                    }
                }
            }
        }
        if (targetLlv == null && !sches.isEmpty()) {
            targetLlv = sches.get(0);
            targetIdx = 0;
        }

        int buoiThu = tinhBuoiThu(targetLlv, targetIdx);
        LichLamViec nextSession = null;
        int nextBuoiThu = buoiThu + 1;
        if (isGoiThang && targetIdx >= 0) {
            for (int j = targetIdx + 1; j < sches.size(); j++) {
                LichLamViec cand = sches.get(j);
                String st = cand.getTrangThai() != null ? cand.getTrangThai() : "";
                if (!"DaHuy".equalsIgnoreCase(st) && !"Huy".equalsIgnoreCase(st)) {
                    nextSession = cand;
                    nextBuoiThu = tinhBuoiThu(cand, j);
                    break;
                }
            }
        }

        LocalDate oldDate = (targetLlv != null && targetLlv.getNgayLam() != null)
                ? targetLlv.getNgayLam()
                : don.getNgayThucHien();
        String oldDateStr = oldDate != null ? oldDate.format(dateFmt) : "";

        LocalTime origStart = (targetLlv != null && targetLlv.getGioBatDau() != null)
                ? targetLlv.getGioBatDau()
                : (don.getGioBatDau() != null ? don.getGioBatDau() : LocalTime.of(8, 0));
        LocalTime origEnd = (targetLlv != null && targetLlv.getGioKetThuc() != null)
                ? targetLlv.getGioKetThuc()
                : (don.getGioKetThuc() != null ? don.getGioKetThuc() : origStart.plusHours(2));
        long durationMins = java.time.Duration.between(origStart, origEnd).toMinutes();
        if (durationMins <= 0) durationMins = 120;

        LocalTime batDauMoi = origStart;
        LocalTime ketThucMoi = origEnd;
        if (!gioBatDauMoiStr.isEmpty()) {
            String normalizedTime = gioBatDauMoiStr.length() >= 5 ? gioBatDauMoiStr.substring(0, 5) : gioBatDauMoiStr;
            batDauMoi = LocalTime.parse(normalizedTime);
            ketThucMoi = batDauMoi.plusMinutes(durationMins);
        }

        // Xác định ngày mục tiêu cho các phương án dời lịch / làm bù hôm nay
        LocalDate targetDate = null;
        if ("START_TODAY".equals(action)) {
            targetDate = today;
            LocalTime nowTime = LocalTime.now();
            if (nowTime.isAfter(LocalTime.of(21, 0))) {
                throw new IllegalArgumentException("Đã quá muộn trong ngày (" + nowTime.format(timeFmt)
                        + "), không còn khung giờ phù hợp để dọn dẹp hôm nay! Vui lòng chọn Phương án 2 (Dời sang ngày mới).");
            }
            if (batDauMoi.isBefore(nowTime.minusMinutes(5))) {
                throw new IllegalArgumentException("Giờ làm bù hôm nay (" + batDauMoi.format(timeFmt)
                        + ") không được ở trong quá khứ so với thời điểm hiện tại (" + nowTime.format(timeFmt) + ")!");
            }
        } else if ("RESCHEDULE_KEEP".equals(action) || "RESCHEDULE_RELEASE".equals(action)) {
            if (ngayMoiStr.isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn ngày hẹn mới mà khách hàng yêu cầu.");
            }
            targetDate = LocalDate.parse(ngayMoiStr);
            if (targetDate.isBefore(today)) {
                throw new IllegalArgumentException("Ngày hẹn mới không được ở trong quá khứ (phải từ hôm nay " + todayStr + " trở đi).");
            }
            if (targetDate.equals(today) && batDauMoi.isBefore(LocalTime.now().minusMinutes(5))) {
                throw new IllegalArgumentException("Giờ hẹn mới hôm nay (" + batDauMoi.format(timeFmt)
                        + ") không được ở trong quá khứ so với thời điểm hiện tại!");
            }
        }

        // KIỂM TRA 1: Ràng buộc ngày cho Đơn Gói Tháng & không trùng buổi khác trong cùng đơn
        if (targetDate != null) {
            String targetDateFmt = targetDate.format(dateFmt);
            if (isGoiThang) {
                // Đơn gói tháng: Mỗi ngày chỉ có tối đa 1 buổi, không được chọn ngày đã có buổi khác của gói
                for (int i = 0; i < sches.size(); i++) {
                    LichLamViec other = sches.get(i);
                    if (targetLlv != null && other.getId().equals(targetLlv.getId())) continue;
                    String st = other.getTrangThai() != null ? other.getTrangThai() : "";
                    if ("DaHuy".equalsIgnoreCase(st) || "Huy".equalsIgnoreCase(st)) continue;
                    if (targetDate.equals(other.getNgayLam())) {
                        LocalTime oStart = other.getGioBatDau() != null ? other.getGioBatDau() : LocalTime.of(8, 0);
                        LocalTime oEnd = other.getGioKetThuc() != null ? other.getGioKetThuc() : oStart.plusHours(2);
                        throw new IllegalArgumentException("Đơn gói tháng: Ngày " + targetDateFmt + " đã có lịch của Buổi " + (i + 1)
                                + " (" + oStart.format(timeFmt) + " - " + oEnd.format(timeFmt)
                                + ")! Vui lòng chọn ngày khác chưa có ca làm hoặc chọn làm bù sau buổi cuối cùng.");
                    }
                }
            } else {
                // Đơn đơn lẻ: Kiểm tra không trùng giờ với bất kỳ buổi nào khác của chính đơn này trong cùng ngày targetDate
                for (int i = 0; i < sches.size(); i++) {
                    LichLamViec other = sches.get(i);
                    if (targetLlv != null && other.getId().equals(targetLlv.getId())) continue;
                    String st = other.getTrangThai() != null ? other.getTrangThai() : "";
                    if ("DaHuy".equalsIgnoreCase(st) || "Huy".equalsIgnoreCase(st)) continue;
                    if (targetDate.equals(other.getNgayLam())) {
                        LocalTime oStart = other.getGioBatDau() != null ? other.getGioBatDau() : LocalTime.of(8, 0);
                        LocalTime oEnd = other.getGioKetThuc() != null ? other.getGioKetThuc() : oStart.plusHours(2);
                        if (batDauMoi.isBefore(oEnd) && oStart.isBefore(ketThucMoi)) {
                            throw new IllegalArgumentException("Ngày " + targetDateFmt + " đơn này đã có ca từ "
                                    + oStart.format(timeFmt) + " - " + oEnd.format(timeFmt)
                                    + ". Vui lòng chọn ngày khác hoặc đổi khung giờ làm bù!");
                        }
                    }
                }
            }
        }

        // KIỂM TRA 2: Kiểm tra trùng khung giờ (Sáng / Chiều) với tất cả các ca/đơn khác của CTV trong ngày targetDate
        if (targetDate != null && ("START_TODAY".equals(action) || "RESCHEDULE_KEEP".equals(action)) && pc.getCongTacVien() != null) {
            String targetDateFmt = targetDate.format(dateFmt);
            Integer ctvId = pc.getCongTacVien().getId();
            List<LichLamViec> existingOnDate = lichLamViecRepository
                    .findByCongTacVien_IdAndNgayLamBetweenOrderByNgayLamAscGioBatDauAsc(
                            ctvId, targetDate, targetDate);

            for (LichLamViec ex : existingOnDate) {
                if (targetLlv != null && ex.getId().equals(targetLlv.getId())) continue;
                String st = ex.getTrangThai() != null ? ex.getTrangThai() : "";
                // Chỉ bỏ qua ca đã hủy; cả ca SapToi, DangThucHien lẫn HoanThanh trong khung giờ đó đều đã chiếm lịch của CTV
                if ("DaHuy".equalsIgnoreCase(st) || "Huy".equalsIgnoreCase(st)) continue;
                if (ex.getPhanCong() != null && "TuChoi".equalsIgnoreCase(ex.getPhanCong().getTrangThai())) continue;

                LocalTime exStart = ex.getGioBatDau() != null ? ex.getGioBatDau() : LocalTime.of(8, 0);
                LocalTime exEnd = ex.getGioKetThuc() != null ? ex.getGioKetThuc() : exStart.plusHours(2);

                if (batDauMoi.isBefore(exEnd) && exStart.isBefore(ketThucMoi)) {
                    String exMaDon = (ex.getPhanCong() != null && ex.getPhanCong().getDonDat() != null)
                            ? ex.getPhanCong().getDonDat().getMaDonDat()
                            : ex.getMaLichLamViec();
                    if ("START_TODAY".equals(action)) {
                        throw new IllegalArgumentException("Không thể xác nhận 'Làm bù ngay hôm nay' (" + targetDateFmt
                                + ") vì lịch hôm nay của bạn bị trùng với ca " + exStart.format(timeFmt) + " - " + exEnd.format(timeFmt)
                                + " (Đơn " + exMaDon + "). Dù khách hàng rảnh nhưng lịch của bạn bị trùng, vui lòng chọn Phương án 2 (Dời sang ngày mới), Phương án 3 (Tìm CTV khác) hoặc đổi sang khung giờ Sáng/Chiều còn trống!");
                    }
                    String goiY;
                    if (!exEnd.isAfter(LocalTime.of(13, 0))) {
                        goiY = "Khung buổi sáng đã bận (" + exStart.format(timeFmt) + " - " + exEnd.format(timeFmt)
                                + "), bạn có thể đổi Giờ làm bù sang buổi chiều (ví dụ 14:00) hoặc chọn ngày khác!";
                    } else if (!exStart.isBefore(LocalTime.of(12, 0))) {
                        goiY = "Khung buổi chiều đã bận (" + exStart.format(timeFmt) + " - " + exEnd.format(timeFmt)
                                + "), bạn có thể đổi Giờ làm bù sang buổi sáng (ví dụ 08:00) hoặc chọn ngày khác!";
                    } else {
                        goiY = "Vui lòng đổi Giờ làm bù sang khung giờ còn trống trong ngày hoặc chọn ngày khác!";
                    }
                    throw new IllegalArgumentException("Trùng lịch làm việc! Ngày " + targetDateFmt
                            + " bạn đã có ca từ " + exStart.format(timeFmt) + " - " + exEnd.format(timeFmt)
                            + " (Đơn " + exMaDon + "). " + goiY);
                }
            }

            // Kiểm tra thêm các đơn phân công khác của CTV có ngày thực hiện trùng targetDate (phòng trường hợp đơn chưa sinh LichLamViec)
            List<PhanCongCTV> allPcOfCtv = phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(ctvId);
            for (PhanCongCTV otherPc : allPcOfCtv) {
                if (otherPc.getId().equals(pc.getId())) continue;
                if ("TuChoi".equalsIgnoreCase(otherPc.getTrangThai())) continue;
                DonDatDichVu otherDon = otherPc.getDonDat();
                if (otherDon == null) continue;
                if ("DaHuy".equalsIgnoreCase(otherDon.getTrangThai()) || "Huy".equalsIgnoreCase(otherDon.getTrangThai())) continue;
                if (targetDate.equals(otherDon.getNgayThucHien())) {
                    LocalTime exStart = otherDon.getGioBatDau() != null ? otherDon.getGioBatDau() : LocalTime.of(8, 0);
                    LocalTime exEnd = otherDon.getGioKetThuc() != null ? otherDon.getGioKetThuc() : exStart.plusHours(2);
                    if (batDauMoi.isBefore(exEnd) && exStart.isBefore(ketThucMoi)) {
                        if ("START_TODAY".equals(action)) {
                            throw new IllegalArgumentException("Không thể xác nhận 'Làm bù ngay hôm nay' (" + targetDateFmt
                                    + ") vì lịch hôm nay của bạn bị trùng với ca " + exStart.format(timeFmt) + " - " + exEnd.format(timeFmt)
                                    + " (Đơn " + otherDon.getMaDonDat() + "). Dù khách hàng rảnh nhưng lịch của bạn bị trùng, vui lòng chọn Phương án 2 (Dời sang ngày mới) hoặc đổi sang khung giờ Sáng/Chiều còn trống!");
                        }
                        throw new IllegalArgumentException("Trùng lịch làm việc! Ngày " + targetDateFmt
                                + " bạn đã có ca từ " + exStart.format(timeFmt) + " - " + exEnd.format(timeFmt)
                                + " (Đơn " + otherDon.getMaDonDat() + "). Vui lòng đổi sang khung giờ Sáng/Chiều còn trống hoặc chọn ngày khác!");
                    }
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("donDatId", don.getId());

        String buoiLabel = isGoiThang ? " (Buổi " + buoiThu + "/" + sches.size() + ")" : "";
        String khungGioMoiStr = batDauMoi.format(timeFmt) + " - " + ketThucMoi.format(timeFmt);

        switch (action) {
            case "START_TODAY": {
                if (!isGoiThang || buoiThu == 1) {
                    don.setNgayThucHien(today);
                }
                if (!isGoiThang) {
                    don.setGioBatDau(batDauMoi);
                    don.setGioKetThuc(ketThucMoi);
                }
                don.setTrangThai("DangThucHien");
                donDatDichVuRepository.save(don);

                if (targetLlv != null) {
                    targetLlv.setNgayLam(today);
                    targetLlv.setGioBatDau(batDauMoi);
                    targetLlv.setGioKetThuc(ketThucMoi);
                    targetLlv.setTrangThai("DangThucHien");
                    lichLamViecRepository.save(targetLlv);
                }

                String note = "Khách hàng đồng ý làm bù ca quá hạn" + buoiLabel + " (Lịch cũ: " + oldDateStr
                        + " -> Hôm nay: " + todayStr + ", khung giờ: " + khungGioMoiStr + ")"
                        + (cleanGhiChu.isEmpty() ? "" : ". Ghi chú: " + cleanGhiChu);
                lichSuTrangThaiDonRepository.save(LichSuTrangThaiDon.builder()
                        .donDat(don)
                        .trangThaiCu(oldOrderStatus)
                        .trangThaiMoi("DangThucHien")
                        .nguoiThucHien(nguoiThucHien)
                        .thoiGian(LocalDateTime.now())
                        .ghiChu(note)
                        .build());

                res.put("trangThaiDon", "DangThucHien");
                res.put("trangThaiPhanCong", pc.getTrangThai());
                res.put("ngayThucHien", today.toString());
                res.put("gioBatDau", batDauMoi.format(timeFmt));
                res.put("gioKetThuc", ketThucMoi.format(timeFmt));
                res.put("message", "Đã cập nhật lịch làm bù" + buoiLabel + " hôm nay (" + todayStr + " lúc " + khungGioMoiStr + ") và bắt đầu thực hiện!");
                break;
            }
            case "RESCHEDULE_KEEP": {
                LocalDate newDate = targetDate;
                if (!isGoiThang) {
                    don.setNgayThucHien(newDate);
                    don.setGioBatDau(batDauMoi);
                    don.setGioKetThuc(ketThucMoi);
                } else {
                    // Gói tháng: ngày thực hiện trên đơn là ngày của buổi sớm nhất còn phải làm
                    LocalDate earliestDate = newDate;
                    for (LichLamViec l : sches) {
                        if (targetLlv != null && l.getId().equals(targetLlv.getId())) continue;
                        String st = l.getTrangThai() != null ? l.getTrangThai() : "";
                        if ("SapToi".equalsIgnoreCase(st) || "DangThucHien".equalsIgnoreCase(st)) {
                            if (l.getNgayLam() != null && l.getNgayLam().isBefore(earliestDate)) {
                                earliestDate = l.getNgayLam();
                            }
                        }
                    }
                    don.setNgayThucHien(earliestDate);
                }
                don.setTrangThai("DaXacNhan");
                donDatDichVuRepository.save(don);

                if (targetLlv != null) {
                    targetLlv.setNgayLam(newDate);
                    targetLlv.setGioBatDau(batDauMoi);
                    targetLlv.setGioKetThuc(ketThucMoi);
                    targetLlv.setTrangThai("SapToi");
                    lichLamViecRepository.save(targetLlv);
                }

                String newDateFmt = newDate.format(dateFmt);
                String note = "Khách hàng thống nhất dời ca quá hạn" + buoiLabel + " (Lịch cũ: " + oldDateStr
                        + " -> Ngày mới: " + newDateFmt + ", khung giờ: " + khungGioMoiStr + "), CTV tiếp tục phụ trách"
                        + (cleanGhiChu.isEmpty() ? "" : ". Ghi chú: " + cleanGhiChu);
                lichSuTrangThaiDonRepository.save(LichSuTrangThaiDon.builder()
                        .donDat(don)
                        .trangThaiCu(oldOrderStatus)
                        .trangThaiMoi("DaXacNhan")
                        .nguoiThucHien(nguoiThucHien)
                        .thoiGian(LocalDateTime.now())
                        .ghiChu(note)
                        .build());

                res.put("trangThaiDon", "DaXacNhan");
                res.put("trangThaiPhanCong", pc.getTrangThai());
                res.put("ngayThucHien", newDate.toString());
                res.put("gioBatDau", batDauMoi.format(timeFmt));
                res.put("gioKetThuc", ketThucMoi.format(timeFmt));
                res.put("message", "Đã dời lịch làm" + buoiLabel + " sang ngày " + newDateFmt + " (" + khungGioMoiStr + ") thành công!");
                break;
            }
            case "RESCHEDULE_RELEASE": {
                LocalDate newDate = targetDate;
                String newDateFmt = newDate.format(dateFmt);
                String lyDo = !cleanGhiChu.isEmpty()
                        ? cleanGhiChu
                        : "Khách dời ca quá hạn" + buoiLabel + " (" + oldDateStr + ") sang " + newDateFmt + " (" + khungGioMoiStr + "), CTV bận lịch ngày mới";

                // Gói tháng: giữ lại các buổi CTV đã HOÀN THÀNH (lịch sử, tính công), chỉ trả các buổi chưa làm về pool
                List<LichLamViec> buoiDaXong = new ArrayList<>();
                List<LichLamViec> buoiChuaLam = new ArrayList<>();
                for (LichLamViec l : sches) {
                    if ("HoanThanh".equalsIgnoreCase(l.getTrangThai())) buoiDaXong.add(l);
                    else buoiChuaLam.add(l);
                }
                if (!buoiChuaLam.isEmpty()) {
                    lichLamViecRepository.deleteAll(buoiChuaLam);
                }
                if (buoiDaXong.isEmpty()) {
                    phanCongCTVRepository.delete(pc);
                } else {
                    pc.setTrangThai("HoanThanh");
                    pc.setLyDoTuChoi("Đã làm " + buoiDaXong.size() + " buổi, trả các buổi còn lại về Đang tìm CTV: " + lyDo);
                    phanCongCTVRepository.save(pc);
                }
                phanCongCTVRepository.flush();

                don.setNgayThucHien(newDate);
                don.setGioBatDau(batDauMoi);
                don.setGioKetThuc(ketThucMoi);
                don.setTrangThai("ChoDuyet");
                donDatDichVuRepository.save(don);

                String note = "Khách dời lịch" + buoiLabel + " từ " + oldDateStr + " sang " + newDateFmt
                        + " (" + khungGioMoiStr + "). CTV " + tenCtv + " bận nên trả đơn về Đang tìm CTV. Lý do: " + lyDo;
                lichSuTrangThaiDonRepository.save(LichSuTrangThaiDon.builder()
                        .donDat(don)
                        .trangThaiCu(oldOrderStatus)
                        .trangThaiMoi("ChoDuyet")
                        .nguoiThucHien(nguoiThucHien)
                        .thoiGian(LocalDateTime.now())
                        .ghiChu(note)
                        .build());

                res.put("trangThaiDon", "ChoDuyet");
                res.put("trangThaiPhanCong", "TuChoi");
                res.put("ngayThucHien", newDate.toString());
                res.put("gioBatDau", batDauMoi.format(timeFmt));
                res.put("gioKetThuc", ketThucMoi.format(timeFmt));
                res.put("message", "Đã cập nhật ngày mới (" + newDateFmt + " lúc " + khungGioMoiStr + ") và chuyển đơn về trạng thái Đang tìm CTV!");
                break;
            }
            case "CANCEL_ORDER": {
                for (LichLamViec l : sches) {
                    if ("DangThucHien".equalsIgnoreCase(l.getTrangThai())) {
                        throw new IllegalStateException("Đơn đang có buổi làm ở trạng thái Đang thực hiện (Ngày "
                                + (l.getNgayLam() != null ? l.getNgayLam().format(dateFmt) : "")
                                + "), không thể hủy trực tiếp!");
                    }
                }
                String lyDo = !cleanGhiChu.isEmpty()
                        ? cleanGhiChu
                        : "Ca làm" + buoiLabel + " ngày " + oldDateStr + " đã quá hạn, khách hàng xác nhận hủy đơn";

                pc.setTrangThai("TuChoi");
                pc.setLyDoTuChoi(lyDo);
                phanCongCTVRepository.save(pc);
                if (!sches.isEmpty()) {
                    lichLamViecRepository.deleteAll(sches);
                }

                don.setTrangThai("DaHuy");
                donDatDichVuRepository.save(don);

                lichSuTrangThaiDonRepository.save(LichSuTrangThaiDon.builder()
                        .donDat(don)
                        .trangThaiCu(oldOrderStatus)
                        .trangThaiMoi("DaHuy")
                        .nguoiThucHien(nguoiThucHien)
                        .thoiGian(LocalDateTime.now())
                        .ghiChu("Hủy đơn quá hạn" + buoiLabel + " theo xác nhận của khách hàng: " + lyDo)
                        .build());

                res.put("trangThaiDon", "DaHuy");
                res.put("trangThaiPhanCong", "TuChoi");
                res.put("message", "Đã hủy đơn quá hạn" + buoiLabel + " theo xác nhận của khách hàng.");
                break;
            }
            default:
                throw new IllegalArgumentException("Phương án xử lý không hợp lệ: " + action);
        }

        return res;
    }

    public Map<String, Object> completeAssignment(Integer phanCongId, AssignmentActionRequest req) {
        // Khóa phân công: CTV bấm hai lần hoặc CSKH đổi phân công cùng lúc sẽ được xử lý lần lượt
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        DonDatDichVu don = pc.getDonDat();
        List<LichLamViec> sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_Id(pc.getId()));
        if (sches.isEmpty()) {
            sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()));
        }
        sches.sort(CHRONO_LLV_COMPARATOR);

        String cleanKetQua = (req != null && req.getKetQuaThucHien() != null && !req.getKetQuaThucHien().trim().isEmpty())
                ? req.getKetQuaThucHien().trim().replaceAll("\\s+", " ")
                : "Hoàn thành công việc";

        // Xác định buổi đang thực hiện (hoặc buổi chưa hoàn thành đầu tiên) để hoàn thành đúng 1 buổi
        LichLamViec activeSession = null;
        for (LichLamViec llv : sches) {
            if ("DangThucHien".equalsIgnoreCase(llv.getTrangThai())) {
                activeSession = llv;
                break;
            }
        }
        if (activeSession == null) {
            for (LichLamViec llv : sches) {
                if ("SapToi".equalsIgnoreCase(llv.getTrangThai())) {
                    activeSession = llv;
                    break;
                }
            }
        }
        if (activeSession != null) {
            activeSession.setTrangThai("HoanThanh");
            activeSession.setKetQuaThucHien(cleanKetQua);
            lichLamViecRepository.save(activeSession);
        }

        // Kiểm tra xem gói tháng còn buổi SapToi nào chưa làm không
        LichLamViec nextRemaining = null;
        for (LichLamViec llv : sches) {
            if ("SapToi".equalsIgnoreCase(llv.getTrangThai())) {
                nextRemaining = llv;
                break;
            }
        }

        if (nextRemaining != null) {
            // Còn buổi tiếp theo trong gói tháng -> giữ phân công ở DaXacNhan, cập nhật ngày thực hiện sang buổi kế tiếp
            pc.setTrangThai("DaXacNhan");
            phanCongCTVRepository.save(pc);
            if (nextRemaining.getNgayLam() != null) {
                don.setNgayThucHien(nextRemaining.getNgayLam());
            }
            don.setTrangThai("DaXacNhan");
            donDatDichVuRepository.save(don);
        } else {
            pc.setTrangThai("HoanThanh");
            phanCongCTVRepository.save(pc);
            don.setTrangThai("HoanThanh");
            donDatDichVuRepository.save(don);
        }

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("DangThucHien")
                .trangThaiMoi(nextRemaining != null ? "DaXacNhan" : "HoanThanh")
                .nguoiThucHien("CongTacVien: " + pc.getCongTacVien().getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV hoàn thành ca làm việc. Kết quả: " + cleanKetQua)
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        String tenDichVu = DatNhieuDichVu.tenDichVuCuaDon(don, "Dịch vụ gia đình");
        String tenCTV = pc.getCongTacVien() != null ? pc.getCongTacVien().getHoTen() : "Cộng tác viên";
        String gioHoanThanh = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String ngayHoanThanh = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String thu = getThuTrongTuan(LocalDate.now());

        // 1. Tạo thông báo hoàn thành đơn cho CTV (ghi rõ nhiệm vụ gì, ngày nào, mấy giờ)
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String prefixCTV = "CTV " + tenCTV;
            String tienStr = don.getThanhTien() != null ? String.format("%,d", don.getThanhTien().longValue()) + "đ" : "";
            ThongBao tbComplete = ThongBao.builder()
                    .maThongBao("TB-CTV-HT-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .tieuDe(prefixCTV + " – Hoàn thành nhiệm vụ: " + don.getMaDonDat())
                    .noiDung(prefixCTV + " đã hoàn thành nhiệm vụ " + tenDichVu + " (Đơn " + don.getMaDonDat() + ") vào lúc " + gioHoanThanh + ", " + thu + " (ngày " + ngayHoanThanh + ")"
                            + (tienStr.isEmpty() ? "" : ". Tiền công nhận được: " + tienStr)
                            + ". Chúc mừng bạn đã hoàn thành xuất sắc ca làm việc!")
                    .nguoiGui("Hệ thống bTaskee")
                    .nhomNhan("CaNhan")
                    .thoiGianGui(LocalDateTime.now())
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(tbComplete);

            ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                    .thongBao(tbComplete)
                    .taiKhoan(pc.getCongTacVien().getTaiKhoan())
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build();
            thongBaoNguoiDungRepository.save(tbnd);
        }

        // Thông báo cho khách hàng: ThongBaoSuKienService gửi theo sự kiện phân công / trạng thái đơn

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("donDatTrangThai", "HoanThanh");
        res.put("message", "Đã cập nhật trạng thái hoàn thành dịch vụ. Khách hàng có thể tiến hành đánh giá.");
        return res;
    }

    // ==========================================
    // UC-CTV02: NHẬN THÔNG BÁO CỦA CỘNG TÁC VIÊN
    // ==========================================
    public List<Map<String, Object>> getCollaboratorNotifications(Integer taiKhoanId) {
        // Resolve TaiKhoan linh hoạt (hỗ trợ truyền taiKhoanId hoặc congTacVienId)
        TaiKhoan tk = taiKhoanRepository.findById(taiKhoanId).orElse(null);
        CongTacVien ctv = null;
        if (tk == null) {
            ctv = congTacVienRepository.findById(taiKhoanId).orElse(null);
            if (ctv != null) {
                tk = ctv.getTaiKhoan();
            }
        } else {
            ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
            if (ctv == null) {
                ctv = congTacVienRepository.findById(tk.getId()).orElse(null);
            }
        }
        if (tk == null) {
            return Collections.emptyList();
        }
        Integer actualTaiKhoanId = tk.getId();

        // 1. Tự động đồng bộ các thông báo chung từ Ban Giám Đốc (nhomNhan = CongTacVien hoặc TatCa)
        List<ThongBao> broadcastList = thongBaoRepository.findAll().stream()
                .filter(tb -> "DaGui".equalsIgnoreCase(tb.getTrangThai()) &&
                        ("CongTacVien".equalsIgnoreCase(tb.getNhomNhan()) || "TatCa".equalsIgnoreCase(tb.getNhomNhan())))
                .toList();

        List<ThongBaoNguoiDung> existingList = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(actualTaiKhoanId);
        Set<Integer> existingTbIds = new HashSet<>();
        for (ThongBaoNguoiDung item : existingList) {
            if (item.getThongBao() != null) {
                existingTbIds.add(item.getThongBao().getId());
            }
        }

        List<ThongBaoNguoiDung> newlyCreated = new ArrayList<>();
        for (ThongBao tb : broadcastList) {
            if (!existingTbIds.contains(tb.getId())) {
                ThongBaoNguoiDung nb = ThongBaoNguoiDung.builder()
                        .thongBao(tb)
                        .taiKhoan(tk)
                        .daDoc(false)
                        .trangThai("DaGui")
                        .build();
                newlyCreated.add(nb);
                existingTbIds.add(tb.getId());
            }
        }

        // 2. Tự động đồng bộ thông báo cho các đơn phân công thực tế của CTV (Hoàn thành, Đơn mới)
        if (ctv != null) {
            List<PhanCongCTV> pcList = phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(ctv.getId());
            Set<String> notifiedKeys = new HashSet<>();
            for (ThongBaoNguoiDung item : existingList) {
                if (item.getThongBao() != null) {
                    String fullText = ((item.getThongBao().getTieuDe() != null ? item.getThongBao().getTieuDe() : "") + " "
                            + (item.getThongBao().getNoiDung() != null ? item.getThongBao().getNoiDung() : "")).toLowerCase();
                    for (PhanCongCTV pc : pcList) {
                        if (pc.getDonDat() != null && pc.getDonDat().getMaDonDat() != null) {
                            String code = pc.getDonDat().getMaDonDat().toLowerCase();
                            if (fullText.contains(code)) {
                                if (fullText.contains("hoàn thành")) {
                                    notifiedKeys.add(pc.getDonDat().getMaDonDat() + "_HOAN_THANH");
                                } else {
                                    notifiedKeys.add(pc.getDonDat().getMaDonDat() + "_DON_MOI");
                                }
                            }
                        }
                    }
                }
            }

            for (PhanCongCTV pc : pcList) {
                if (pc.getDonDat() == null || pc.getDonDat().getMaDonDat() == null) continue;
                String maDon = pc.getDonDat().getMaDonDat();
                if ("HoanThanh".equalsIgnoreCase(pc.getTrangThai())) {
                    if (!notifiedKeys.contains(maDon + "_HOAN_THANH")) {
                        String tienStr = pc.getDonDat().getThanhTien() != null
                                ? String.format("%,d", pc.getDonDat().getThanhTien().longValue()) + "đ"
                                : "";
                        LocalDateTime tg = pc.getThoiGianXacNhan() != null ? pc.getThoiGianXacNhan()
                                : (pc.getThoiGianPhanCong() != null ? pc.getThoiGianPhanCong() : LocalDateTime.now());
                        ThongBao tbComplete = ThongBao.builder()
                                .maThongBao("TB-HT-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                                .tieuDe("Đơn " + maDon + " đã hoàn thành")
                                .noiDung("Bạn nhận được " + tienStr + " từ đơn " + maDon + ". Chúc mừng bạn đã hoàn thành xuất sắc ca làm việc!")
                                .nguoiGui("Hệ thống")
                                .nhomNhan("CaNhan")
                                .thoiGianGui(tg)
                                .trangThai("DaGui")
                                .build();
                        thongBaoRepository.save(tbComplete);

                        ThongBaoNguoiDung nb = ThongBaoNguoiDung.builder()
                                .thongBao(tbComplete)
                                .taiKhoan(tk)
                                .daDoc(false)
                                .trangThai("DaGui")
                                .build();
                        newlyCreated.add(nb);
                        notifiedKeys.add(maDon + "_HOAN_THANH");
                    }
                } else if ("DaPhanCong".equalsIgnoreCase(pc.getTrangThai()) || "ChoXacNhan".equalsIgnoreCase(pc.getTrangThai())) {
                    if (!notifiedKeys.contains(maDon + "_DON_MOI")) {
                        LocalDateTime tg = pc.getThoiGianPhanCong() != null ? pc.getThoiGianPhanCong() : LocalDateTime.now();
                        ThongBao tbAssign = ThongBao.builder()
                                .maThongBao("TB-PC-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                                .tieuDe("Bạn có đơn mới: " + maDon)
                                .noiDung("Bạn vừa được phân công đơn mới " + maDon + ". Vui lòng kiểm tra chi tiết và xác nhận nhận đơn!")
                                .nguoiGui("Hệ thống CSKH")
                                .nhomNhan("CaNhan")
                                .thoiGianGui(tg)
                                .trangThai("DaGui")
                                .build();
                        thongBaoRepository.save(tbAssign);

                        ThongBaoNguoiDung nb = ThongBaoNguoiDung.builder()
                                .thongBao(tbAssign)
                                .taiKhoan(tk)
                                .daDoc(false)
                                .trangThai("DaGui")
                                .build();
                        newlyCreated.add(nb);
                        notifiedKeys.add(maDon + "_DON_MOI");
                    }
                }
            }
        }

        // 3. Nếu CTV chưa có thông báo cá nhân nào từ trước, tạo các thông báo thực tế theo trạng thái tài khoản
        if (existingList.isEmpty() && newlyCreated.isEmpty()) {
            ThongBao welcomeTb = ThongBao.builder()
                    .maThongBao("TB-WELCOME-" + actualTaiKhoanId)
                    .tieuDe("Chào mừng đến bTaskee")
                    .noiDung("Chào mừng bạn gia nhập đội ngũ Cộng tác viên Neatify! Hoàn thiện hồ sơ để bắt đầu nhận được nhiều đơn hơn.")
                    .nguoiGui("Ban Giám đốc")
                    .nhomNhan("CaNhan")
                    .thoiGianGui(LocalDateTime.now().minusDays(2))
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(welcomeTb);
            newlyCreated.add(ThongBaoNguoiDung.builder().thongBao(welcomeTb).taiKhoan(tk).daDoc(false).trangThai("DaGui").build());
        }

        if (!newlyCreated.isEmpty()) {
            thongBaoNguoiDungRepository.saveAll(newlyCreated);
            existingList = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(actualTaiKhoanId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ThongBaoNguoiDung tbnd : existingList) {
            if (tbnd.getThongBao() == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tbnd.getId());
            m.put("thongBaoId", tbnd.getThongBao().getId());
            m.put("tieuDe", tbnd.getThongBao().getTieuDe());
            m.put("noiDung", tbnd.getThongBao().getNoiDung());
            m.put("nguoiGui", tbnd.getThongBao().getNguoiGui());
            m.put("thoiGianGui", tbnd.getThongBao().getThoiGianGui());
            m.put("daDoc", tbnd.getDaDoc());
            m.put("thoiGianDoc", tbnd.getThoiGianDoc());
            m.put("loai", resolveLoaiThongBao(tbnd.getThongBao().getTieuDe(), tbnd.getThongBao().getNhomNhan()));
            result.add(m);
        }
        return result;
    }

    private String resolveLoaiThongBao(String tieuDe, String nhomNhan) {
        if (tieuDe == null) return "heThong";
        String lower = tieuDe.toLowerCase();
        if (lower.contains("nhắc lịch") || lower.contains("giờ làm") || lower.contains("ca làm") || lower.contains("sắp đến")) {
            return "nhacLich";
        }
        if (lower.contains("hoàn thành") || lower.contains("thanh toán") || lower.contains("tiền")) {
            return "hoanThanh";
        }
        if (lower.contains("đơn mới") || lower.contains("phân công") || lower.contains("nhận đơn") || lower.contains("giao đơn")) {
            return "donMoi";
        }
        if (lower.contains("từ chối") || lower.contains("hủy")) {
            return "huy";
        }
        return "heThong";
    }

    private String getThuTrongTuan(LocalDate date) {
        if (date == null) return "";
        switch (date.getDayOfWeek()) {
            case MONDAY: return "Thứ Hai";
            case TUESDAY: return "Thứ Ba";
            case WEDNESDAY: return "Thứ Tư";
            case THURSDAY: return "Thứ Năm";
            case FRIDAY: return "Thứ Sáu";
            case SATURDAY: return "Thứ Bảy";
            case SUNDAY: return "Chủ Nhật";
            default: return "";
        }
    }

    public void markNotificationRead(Integer notificationId, Integer taiKhoanId) {
        thongBaoNguoiDungRepository.findById(notificationId).ifPresent(tbnd -> {
            tbnd.setDaDoc(true);
            tbnd.setThoiGianDoc(LocalDateTime.now());
            thongBaoNguoiDungRepository.save(tbnd);
        });
    }

    public void markAllNotificationsRead(Integer taiKhoanId) {
        // Resolve TaiKhoan nếu truyền ctvId
        Integer actualId = taiKhoanId;
        TaiKhoan tk = taiKhoanRepository.findById(taiKhoanId).orElse(null);
        if (tk == null) {
            CongTacVien ctv = congTacVienRepository.findById(taiKhoanId).orElse(null);
            if (ctv != null && ctv.getTaiKhoan() != null) {
                actualId = ctv.getTaiKhoan().getId();
            }
        }
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(actualId);
        for (ThongBaoNguoiDung tb : list) {
            tb.setDaDoc(true);
            tb.setThoiGianDoc(LocalDateTime.now());
        }
        thongBaoNguoiDungRepository.saveAll(list);
    }

    // ==========================================
    // UC-CTV03: XEM THỜI GIAN BIỂU LÀM VIỆC
    // ==========================================
    public List<Map<String, Object>> getSchedules(Integer congTacVienId, LocalDate fromDate, LocalDate toDate, String trangThai) {
        List<LichLamViec> list;
        if (fromDate != null && toDate != null) {
            list = lichLamViecRepository.findByCongTacVien_IdAndNgayLamBetweenOrderByNgayLamAscGioBatDauAsc(congTacVienId, fromDate, toDate);
        } else if (trangThai != null && !trangThai.isBlank()) {
            list = lichLamViecRepository.findByCongTacVien_IdAndTrangThaiOrderByNgayLamAscGioBatDauAsc(congTacVienId, trangThai);
        } else {
            list = lichLamViecRepository.findByCongTacVien_IdOrderByNgayLamAscGioBatDauAsc(congTacVienId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Map<Integer, List<LichLamViec>> donSessionsCache = new HashMap<>();

        for (LichLamViec llv : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", llv.getId());
            item.put("lichLamViecId", llv.getId());
            item.put("maLichLamViec", llv.getMaLichLamViec());
            item.put("ngayLam", llv.getNgayLam());
            item.put("gioBatDau", llv.getGioBatDau());
            item.put("gioKetThuc", llv.getGioKetThuc());
            item.put("trangThai", llv.getTrangThai());
            item.put("ketQuaThucHien", llv.getKetQuaThucHien());

            if (llv.getPhanCong() != null) {
                PhanCongCTV pc = llv.getPhanCong();
                item.put("phanCongId", pc.getId());
                item.put("trangThaiPhanCong", pc.getTrangThai());

                if (pc.getDonDat() != null) {
                    DonDatDichVu d = pc.getDonDat();
                    item.put("donDatId", d.getId());
                    item.put("maDonDat", d.getMaDonDat());
                    item.put("trangThaiDon", d.getTrangThai());
                    String tenDv = DatNhieuDichVu.tenDichVuCuaDon(d, "");
                    item.put("tenDichVu", tenDv);
                    String fullDiaChi = CustomerAddressService.diaChiDayDu(d.getDiaChi());
                    item.put("diaChi", fullDiaChi);
                    item.put("diaChiChiTiet", fullDiaChi);
                    item.put("khuVuc", d.getDiaChi() != null && d.getDiaChi().getKhuVuc() != null ? d.getDiaChi().getKhuVuc().getTenKhuVuc() : "");
                    item.put("khachHangTen", d.getKhachHang() != null ? d.getKhachHang().getHoTen() : "Khách hàng");
                    item.put("khachHangPhone", d.getKhachHang() != null ? d.getKhachHang().getSoDienThoai() : "");
                    item.put("yeuCauDacBiet", d.getYeuCauDacBiet());
                    item.put("thanhTien", d.getThanhTien());
                    item.put("loaiHinhDat", d.getLoaiHinhDat());
                    item.put("loaiHinh", d.getLoaiHinhDat());

                    List<LichLamViec> allDonSches = donSessionsCache.computeIfAbsent(d.getId(), k -> {
                        List<LichLamViec> s = new ArrayList<>(lichLamViecRepository.findByPhanCong_Id(pc.getId()));
                        if (s.isEmpty()) {
                            s = new ArrayList<>(lichLamViecRepository.findByPhanCong_DonDat_Id(d.getId()));
                        }
                        s.sort(CHRONO_LLV_COMPARATOR);
                        return s;
                    });

                    boolean isGoi = "GoiThang".equalsIgnoreCase(d.getLoaiHinhDat()) || allDonSches.size() > 1;
                    int soBuoi = isGoi ? Math.max(allDonSches.size(), LichGoiThang.soBuoi(null, tenDv)) : 1;
                    int curIdx = -1;
                    for (int i = 0; i < allDonSches.size(); i++) {
                        if (allDonSches.get(i).getId().equals(llv.getId())) {
                            curIdx = i;
                            break;
                        }
                    }
                    int buoiThu = tinhBuoiThu(llv, curIdx);
                    LichLamViec nextLlv = null;
                    if (isGoi && curIdx >= 0) {
                        for (int j = curIdx + 1; j < allDonSches.size(); j++) {
                            LichLamViec cand = allDonSches.get(j);
                            String st = cand.getTrangThai() != null ? cand.getTrangThai() : "";
                            if (!"DaHuy".equalsIgnoreCase(st) && !"Huy".equalsIgnoreCase(st)) {
                                nextLlv = cand;
                                break;
                            }
                        }
                    }

                    long soBuoiConLai = allDonSches.stream()
                            .filter(s -> !"HoanThanh".equalsIgnoreCase(s.getTrangThai())
                                    && !"DaHuy".equalsIgnoreCase(s.getTrangThai())
                                    && !"Huy".equalsIgnoreCase(s.getTrangThai()))
                            .count();

                    boolean isBuoiLamBu = false;
                    if (llv.getNgayLam() != null && isGoi) {
                        for (LichLamViec other : allDonSches) {
                            if (!other.getId().equals(llv.getId()) && other.getNgayLam() != null) {
                                int otherB = tinhBuoiThu(other, -1);
                                if (buoiThu < otherB && llv.getNgayLam().isAfter(other.getNgayLam())) {
                                    isBuoiLamBu = true;
                                    break;
                                }
                            }
                        }
                    }

                    item.put("soBuoi", soBuoi);
                    item.put("buoiThu", buoiThu);
                    item.put("soBuoiConLai", soBuoiConLai);
                    item.put("isBuoiLamBu", isBuoiLamBu);
                    item.put("isGoiThang", isGoi);
                    item.put("ngayBuoiTiepTheo", nextLlv != null ? nextLlv.getNgayLam() : null);
                    item.put("gioBuoiTiepTheo", nextLlv != null ? nextLlv.getGioBatDau() : null);
                }
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getScheduleDetail(Integer lichLamViecId) {
        LichLamViec llv = lichLamViecRepository.findById(lichLamViecId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc ID: " + lichLamViecId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", llv.getId());
        result.put("maLichLamViec", llv.getMaLichLamViec());
        result.put("ngayLam", llv.getNgayLam());
        result.put("gioBatDau", llv.getGioBatDau());
        result.put("gioKetThuc", llv.getGioKetThuc());
        result.put("trangThai", llv.getTrangThai());
        result.put("ketQuaThucHien", llv.getKetQuaThucHien());

        if (llv.getPhanCong() != null && llv.getPhanCong().getDonDat() != null) {
            DonDatDichVu d = llv.getPhanCong().getDonDat();
            result.put("donDatId", d.getId());
            result.put("maDonDat", d.getMaDonDat());
            String tenDv = DatNhieuDichVu.tenDichVuCuaDon(d, "");
            result.put("tenDichVu", tenDv);
            result.put("diaChi", CustomerAddressService.diaChiDayDu(d.getDiaChi()));
            result.put("khuVuc", d.getDiaChi() != null && d.getDiaChi().getKhuVuc() != null ? d.getDiaChi().getKhuVuc().getTenKhuVuc() : "");
            result.put("yeuCauDacBiet", d.getYeuCauDacBiet());
            result.put("khachHangTen", d.getKhachHang().getHoTen());
            result.put("khachHangPhone", d.getKhachHang().getSoDienThoai());
        }
        return result;
    }
    // ==========================================
    // UC-CTV04: QUẢN LÝ HỒ SƠ & TRẠNG THÁI (MỚI THÊM)
    // ==========================================

    /**
     * Lấy thông tin hồ sơ của Cộng tác viên
     */
    public Map<String, Object> getCollaboratorProfile(Integer congTacVienId) {
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên ID: " + congTacVienId));
        return toProfileMap(ctv);
    }

    /**
     * Dùng khi CTV đăng nhập trên app: nếu tài khoản đã có hồ sơ thì trả về hồ sơ đó,
     * nếu chưa có thì tạo một hồ sơ rỗng (chỉ có thông tin tối thiểu) để CTV bổ sung sau.
     */
    public Map<String, Object> ensureCollaboratorProfile(Integer taiKhoanId) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(taiKhoanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + taiKhoanId));
        if (!"CongTacVien".equalsIgnoreCase(taiKhoan.getLoaiTaiKhoan())) {
            throw new IllegalArgumentException("Tài khoản này không phải tài khoản Cộng tác viên.");
        }

        Optional<CongTacVien> existing = congTacVienRepository.findByTaiKhoan_Id(taiKhoanId);
        if (existing.isPresent()) {
            Map<String, Object> result = toProfileMap(existing.get());
            result.put("daTaoMoi", false);
            return result;
        }

        CongTacVien ctv = CongTacVien.builder()
                .maCongTacVien("CTV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .taiKhoan(taiKhoan)
                .hoTen("")
                .noiCuTru("")
                .soDienThoai(taiKhoan.getSoDienThoai() != null ? taiKhoan.getSoDienThoai() : "")
                .diemDanhGia(java.math.BigDecimal.ZERO)
                .capDo("Moi")
                .trangThai("ChoDuyet")
                .ngayDangKy(LocalDate.now())
                .build();
        ctv = congTacVienRepository.save(ctv);

        Map<String, Object> result = toProfileMap(ctv);
        result.put("daTaoMoi", true);
        return result;
    }

    /** Trả hồ sơ dạng Map phẳng (tránh serialize proxy LAZY của TaiKhoan) */
    private Map<String, Object> toProfileMap(CongTacVien ctv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ctv.getId());
        m.put("maCongTacVien", ctv.getMaCongTacVien());
        m.put("taiKhoanId", ctv.getTaiKhoan() != null ? ctv.getTaiKhoan().getId() : null);
        m.put("hoTen", ctv.getHoTen());
        m.put("ngaySinh", ctv.getNgaySinh() != null ? ctv.getNgaySinh().toString() : null);
        m.put("gioiTinh", ctv.getGioiTinh());
        m.put("noiCuTru", ctv.getNoiCuTru());
        // Quận, huyện nhận việc (app CTV: mục "Khu vực nhận việc")
        m.put("khuVucNhanViec", khuVucCtvService != null ? khuVucCtvService.tomTat(ctv.getId()) : "");
        m.put("soDienThoai", ctv.getSoDienThoai());
        m.put("diemDanhGia", ctv.getDiemDanhGia());
        m.put("capDo", ctv.getCapDo());
        m.put("trangThai", ctv.getTrangThai());
        m.put("ngayDangKy", ctv.getNgayDangKy() != null ? ctv.getNgayDangKy().toString() : null);
        return m;
    }

    // ==========================================
    // UC-CTV-POOL: NHẬN ĐƠN TỪ POOL CHUNG
    // ==========================================

    /**
     * Lấy danh sách đơn đang tìm CTV (pool chung).
     * Tự động lọc các đơn việc mới quanh khu vực của CTV:
     * - Lấy khu vực CTV từ Redis (vị trí GPS mới nhất) hoặc từ bảng KhuVucCTV trong DB.
     * - Chỉ hiển thị các đơn có địa chỉ thuộc cùng khu vực/quận huyện của CTV.
     */
    public List<Map<String, Object>> getAvailableOrders(Integer congTacVienId) {
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId).orElse(null);
        if (ctv == null) {
            TaiKhoan tk = taiKhoanRepository.findById(congTacVienId).orElse(null);
            if (tk != null) {
                ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
            }
        }
        final Integer ctvId = ctv != null ? ctv.getId() : congTacVienId;

        // 1. Lấy vị trí / khu vực CTV từ Redis
        Optional<ViTriCtvStore.ViTri> vtOpt = viTriCtvStore.get(ctvId);
        String quanHuyenCtv = vtOpt.map(ViTriCtvStore.ViTri::quanHuyen).filter(s -> !s.isBlank()).orElse(null);

        // 2. Lấy khu vực CTV từ DB (KhuVucCTV)
        List<KhuVuc> khuVucDb = (khuVucCtvService != null) ? khuVucCtvService.dangNhan(ctvId) : List.of();
        Set<Integer> khuVucIds = khuVucDb.stream().map(KhuVuc::getId).collect(Collectors.toSet());
        if (quanHuyenCtv == null && !khuVucDb.isEmpty()) {
            quanHuyenCtv = khuVucDb.get(0).getTenKhuVuc();
        }

        List<DonDatDichVu> donList = donDatDichVuRepository.findByTrangThaiOrderByNgayTaoDesc("ChoDuyet");
        List<KhuVuc> allKhuVuc = (khuVucService != null) ? khuVucService.danhSach() : List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime bayGio = LocalDateTime.now();
        for (DonDatDichVu don : donList) {
            LocalDateTime hanNhan = hanNhanDonService.hanNhan(don);
            // Chưa phát hành (chờ khách chuyển khoản) hoặc đã hết thời gian nhận thì không hiện cho CTV
            if (hanNhan == null || bayGio.isAfter(hanNhan)) continue;
            // Lọc theo khu vực: nếu xác định được khu vực của CTV thì chỉ lấy đơn cùng khu vực
            if (quanHuyenCtv != null || !khuVucIds.isEmpty()) {
                if (!isDonInKhuVuc(don, quanHuyenCtv, khuVucIds, allKhuVuc)) {
                    continue; // Bỏ qua đơn ngoài khu vực
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("donDatId", don.getId());
            item.put("hanNhan", hanNhan);
            item.put("soGiayConLai", hanNhan == null ? null : Math.max(0, java.time.Duration.between(bayGio, hanNhan).getSeconds()));
            item.put("maDonDat", don.getMaDonDat());
            String tenDichVu = DatNhieuDichVu.tenDichVuCuaDon(don, "Dịch vụ");
            item.put("tenDichVu", tenDichVu);
            item.put("ngayThucHien", don.getNgayThucHien());
            item.put("gioBatDau", don.getGioBatDau());
            item.put("gioKetThuc", don.getGioKetThuc());
            item.put("diaChi", CustomerAddressService.diaChiDayDu(don.getDiaChi()));

            String tenKhuVucDon = "";
            if (don.getDiaChi() != null) {
                if (don.getDiaChi().getKhuVuc() != null) {
                    tenKhuVucDon = don.getDiaChi().getKhuVuc().getTenKhuVuc();
                } else if (khuVucService != null && don.getDiaChi().getDiaChiChiTiet() != null) {
                    tenKhuVucDon = khuVucService.doanQuan(don.getDiaChi().getDiaChiChiTiet(), allKhuVuc)
                            .map(KhuVuc::getTenKhuVuc).orElse("");
                }
            }
            item.put("khuVuc", tenKhuVucDon);
            item.put("yeuCauDacBiet", don.getYeuCauDacBiet());
            item.put("thanhTien", don.getThanhTien());
            item.put("ngayTao", don.getNgayTao());
            // Không expose thông tin khách hàng cho CTV khi đơn chưa được nhận
            item.put("khachHangTen", "Khách hàng");
            item.put("khachHangPhone", "");
            result.add(item);
        }
        return result;
    }

    private boolean isDonInKhuVuc(DonDatDichVu don, String quanHuyenCtv, Set<Integer> khuVucIds, List<KhuVuc> allKhuVuc) {
        if (don.getDiaChi() == null) return false;
        DiaChiKhachHang dc = don.getDiaChi();

        // 1. Khớp qua id của KhuVuc
        if (dc.getKhuVuc() != null) {
            KhuVuc kv = dc.getKhuVuc();
            if (khuVucIds.contains(kv.getId())) return true;
            if (quanHuyenCtv != null && !quanHuyenCtv.isBlank()) {
                String q1 = KhuVucService.chuanHoa(kv.getTenKhuVuc());
                String q2 = KhuVucService.chuanHoa(quanHuyenCtv);
                if (q1.contains(q2) || q2.contains(q1)) return true;
            }
        }

        // 2. Khớp qua DiaChiChiTiet
        if (dc.getDiaChiChiTiet() != null && !dc.getDiaChiChiTiet().isBlank()) {
            if (khuVucService != null) {
                Optional<KhuVuc> guessed = khuVucService.doanQuan(dc.getDiaChiChiTiet(), allKhuVuc);
                if (guessed.isPresent()) {
                    KhuVuc kv = guessed.get();
                    if (khuVucIds.contains(kv.getId())) return true;
                    if (quanHuyenCtv != null && !quanHuyenCtv.isBlank()) {
                        String q1 = KhuVucService.chuanHoa(kv.getTenKhuVuc());
                        String q2 = KhuVucService.chuanHoa(quanHuyenCtv);
                        if (q1.contains(q2) || q2.contains(q1)) return true;
                    }
                }
            }
            if (quanHuyenCtv != null && !quanHuyenCtv.isBlank()) {
                String dcChuan = KhuVucService.chuanHoa(dc.getDiaChiChiTiet());
                String qCtvChuan = KhuVucService.chuanHoa(quanHuyenCtv);
                if (dcChuan.contains(qCtvChuan)) return true;
            }
        }
        return false;
    }

    /**
     * CTV nhận đơn từ pool (atomic - first-come-first-served).
     * Dùng database-level lock để đảm bảo chỉ 1 CTV thắng khi nhiều người ấn cùng lúc.
     */
    @Transactional
    public Map<String, Object> acceptOrderFromPool(Integer congTacVienId, Integer donDatId) {
        // Resolve congTacVienId
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId).orElse(null);
        if (ctv == null) {
            TaiKhoan tk = taiKhoanRepository.findById(congTacVienId).orElse(null);
            if (tk != null) ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
        }
        if (ctv == null) throw new IllegalArgumentException("Không tìm thấy CTV ID: " + congTacVienId);
        if (ctv.getTaiKhoan() == null) throw new IllegalStateException("CTV chưa có tài khoản");

        // !! ATOMIC UPDATE - chỉ 1 request thắng !!
        int rowsAffected = donDatDichVuRepository.atomicAcceptOrder(donDatId);
        if (rowsAffected == 0) {
            throw new IllegalStateException("Đơn này vừa có người nhận rồi! Vui lòng chọn đơn khác.");
        }

        DonDatDichVu don = donDatDichVuRepository.findById(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ID: " + donDatId));
        // Hết thời gian nhận đơn (15 phút kể từ lúc phát hành): báo lỗi, giao dịch hoàn tác lệnh nhận ở trên
        hanNhanDonService.kiemTraConHan(don);

        // Các buổi cần làm: gói tháng tính lại từ ngày thực hiện, trừ số buổi CTV trước đã hoàn thành
        List<LocalDate> ngayCanLam = new ArrayList<>(PhanCongService.cacBuoi(don));
        long soBuoiDaXong = lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()).stream()
                .filter(l -> "HoanThanh".equalsIgnoreCase(l.getTrangThai())).count();
        if (soBuoiDaXong > 0 && ngayCanLam.size() > soBuoiDaXong) {
            ngayCanLam = new ArrayList<>(ngayCanLam.subList(0, (int) (ngayCanLam.size() - soBuoiDaXong)));
        }
        LocalTime gioKT = PhanCongService.gioKetThuc(don);

        // Kiểm tra trùng lịch của CTV trên mọi buổi (lỗi sẽ rollback cả lệnh nhận đơn ở trên)
        congTacVienRepository.findByIdForUpdate(ctv.getId());
        phanCongService.kiemTraDonHoacBaoLoi(ctv.getId(), don, null, false);

        // Tạo 1 bản ghi PhanCongCTV duy nhất khi nhận việc
        PhanCongCTV pc = PhanCongCTV.builder()
                .maPhanCong("PC-" + (System.currentTimeMillis() % 1000000))
                .donDat(don)
                .congTacVien(ctv)
                .trangThai("DaXacNhan")
                .thoiGianPhanCong(LocalDateTime.now())
                .thoiGianXacNhan(LocalDateTime.now())
                .build();
        phanCongCTVRepository.save(pc);

        // Tạo Lịch làm việc (gói tháng: mỗi buổi còn lại một lịch)
        phanCongService.taoLichLamViec(pc, ngayCanLam, don.getGioBatDau(), gioKT, null);

        // Lưu lịch sử
        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("ChoDuyet")
                .trangThaiMoi("DaXacNhan")
                .nguoiThucHien("CongTacVien: " + ctv.getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV tự nhận đơn từ pool qua Mobile App")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Thông báo xác nhận cho CTV
        LocalDate ngayThucHien = don.getNgayThucHien();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalTime gioBatDau = don.getGioBatDau();
        String gioStr = gioBatDau != null ? gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm")) : "giờ quy định";
        String ngayStr = ngayThucHien != null ? ngayThucHien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String thu = getThuTrongTuan(ngayThucHien);

        String tieuDe;
        String noiDung;
        String tenCTV = ctv.getHoTen();
        String prefixCTV = "CTV " + tenCTV;
        String tenKhach = (don.getKhachHang() != null && don.getKhachHang().getHoTen() != null)
                ? don.getKhachHang().getHoTen() : "Khách hàng";
        String diaChiStr = CustomerAddressService.diaChiDayDu(don.getDiaChi());
        String suffixKH = " của KH " + tenKhach + (diaChiStr.isEmpty() ? "" : " (Đ/c: " + diaChiStr + ")");

        if (ngayThucHien != null && !ngayThucHien.isAfter(today)) {
            tieuDe = prefixCTV + " – Nhắc lịch ca làm gấp: " + don.getMaDonDat();
            noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " hôm nay (" + thu + ", " + ngayStr + "). Vui lòng nhớ có mặt trước " + gioStr + " hôm nay!";
        } else if (ngayThucHien != null && ngayThucHien.isEqual(tomorrow)) {
            tieuDe = prefixCTV + " – Nhắc lịch ca làm ngày mai: " + don.getMaDonDat();
            noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào ngày mai (" + thu + ", " + ngayStr + "). Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
        } else {
            tieuDe = prefixCTV + " – Nhắc lịch ca làm: " + don.getMaDonDat();
            noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào " + thu + ", ngày " + ngayStr + ". Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
        }

        ThongBao tbNhacLich = ThongBao.builder()
                .maThongBao("TB-NL-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                .tieuDe(tieuDe)
                .noiDung(noiDung)
                .nguoiGui("Hệ thống CSKH")
                .nhomNhan("CaNhan")
                .thoiGianGui(LocalDateTime.now())
                .trangThai("DaGui")
                .build();
        thongBaoRepository.save(tbNhacLich);

        ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                .thongBao(tbNhacLich)
                .taiKhoan(ctv.getTaiKhoan())
                .daDoc(false)
                .trangThai("DaGui")
                .build();
        thongBaoNguoiDungRepository.save(tbnd);

        // Thông báo cho khách hàng: ThongBaoSuKienService gửi theo sự kiện phân công / trạng thái đơn

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("maDonDat", don.getMaDonDat());
        res.put("trangThai", "DaXacNhan");
        res.put("message", "Đã nhận đơn thành công!");
        return res;
    }

    /**
     * CTV bỏ qua / từ chối đơn từ pool trên ứng dụng.
     * Không chèn bản ghi vào DB. Mobile app tự ẩn đơn trên giao diện local.
     */
    public Map<String, Object> rejectOrderFromPool(Integer congTacVienId, Integer donDatId, String lyDo) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("donDatId", donDatId);
        res.put("message", "Đã bỏ qua đơn hàng");
        return res;
    }

    /**
     * Cập nhật trạng thái (Sẵn sàng / Tạm dừng) của CTV
     */
    public Map<String, Object> updateCollaboratorStatus(Integer congTacVienId, String trangThaiMoi) {
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên ID: " + congTacVienId));
        
        ctv.setTrangThai(trangThaiMoi);
        congTacVienRepository.save(ctv);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("congTacVienId", ctv.getId());
        result.put("trangThaiMoi", ctv.getTrangThai());
        result.put("message", "Đã cập nhật trạng thái hoạt động thành công");
        return result;
    }

    /**
     * Tạo đơn test DangTimCTV (Pool) + Thông báo Broadcast gửi TẤT CẢ CTV
     */
    public Map<String, Object> createTestPoolOrder(String maDon, String tenDichVu, Integer thanhTien) {
        String finalMaDon = (maDon != null && !maDon.isBlank()) ? maDon : "DON-POOL-" + (System.currentTimeMillis() % 100000);
        
        // Lấy đơn mẫu để lấy khachHang & diaChi
        DonDatDichVu sampleOrder = donDatDichVuRepository.findAll().stream().findFirst().orElse(null);
        if (sampleOrder == null) throw new IllegalStateException("Chưa có đơn mẫu nào trong DB");

        java.math.BigDecimal tien = thanhTien != null ? java.math.BigDecimal.valueOf(thanhTien) : java.math.BigDecimal.valueOf(320000);
        DonDatDichVu don = DonDatDichVu.builder()
                .maDonDat(finalMaDon)
                .khachHang(sampleOrder.getKhachHang())
                .diaChi(sampleOrder.getDiaChi())
                .trangThai("ChoDuyet")
                .ngayTao(LocalDateTime.now())
                .ngayThucHien(LocalDate.now().plusDays(1))
                .gioBatDau(LocalTime.of(9, 0))
                .gioKetThuc(LocalTime.of(11, 0))
                .loaiHinhDat(sampleOrder.getLoaiHinhDat() != null ? sampleOrder.getLoaiHinhDat() : "TheoLan")
                .chiPhiGoc(tien)
                .soTienGiam(java.math.BigDecimal.ZERO)
                .thanhTien(tien)
                .yeuCauDacBiet("Căn hộ 2 phòng ngủ - Test Pool đơn mới")
                .build();
        don = donDatDichVuRepository.save(don);

        String sampleKhach = (sampleOrder.getKhachHang() != null && sampleOrder.getKhachHang().getHoTen() != null)
                ? sampleOrder.getKhachHang().getHoTen() : "Nguyễn Thị Thu Lan";
        String sampleDiaChi = (sampleOrder.getDiaChi() != null && sampleOrder.getDiaChi().getDiaChiChiTiet() != null)
                ? sampleOrder.getDiaChi().getDiaChiChiTiet() : "123 Nguyễn Trãi, Q.1";

        // Tạo thông báo Broadcast tới TẤT CẢ CTV (nhomNhan = CongTacVien)
        ThongBao tbBroadcast = ThongBao.builder()
                .maThongBao("TB-POOL-" + don.getId() + "-" + (System.currentTimeMillis() % 100000))
                .tieuDe("Bạn có đơn mới: " + finalMaDon)
                .noiDung("Có đơn mới " + (tenDichVu != null ? tenDichVu : "Dọn dẹp nhà cửa") + " của KH " + sampleKhach + " vào ngày mai lúc 09:00 (Địa chỉ: " + sampleDiaChi + "). Vui lòng vào ứng dụng bấm Nhận việc!")
                .nguoiGui("Hệ thống CSKH")
                .nhomNhan("CongTacVien")
                .thoiGianGui(LocalDateTime.now())
                .trangThai("DaGui")
                .build();
        thongBaoRepository.save(tbBroadcast);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("donDatId", don.getId());
        res.put("maDonDat", don.getMaDonDat());
        res.put("trangThai", don.getTrangThai());
        res.put("thongBaoId", tbBroadcast.getId());
        res.put("message", "Đã tạo đơn pool test thành công và phát thông báo tới tất cả CTV!");
        return res;
    }
}

