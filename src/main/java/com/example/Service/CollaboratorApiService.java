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
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.Repository.DichVuCTVRepository dichVuCTVRepository;
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
    private BoQuaDonStore boQuaDonStore;
    @org.springframework.beans.factory.annotation.Autowired
    private TamQuanHcm tamQuanHcm;
    @org.springframework.beans.factory.annotation.Autowired
    private BieuMauTaiSanRepository bieuMauTaiSanRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private ChiTietTaiSanRepository chiTietTaiSanRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private MinioService minioService;

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

    public static final java.math.BigDecimal TY_LE_THU_NHAP_CTV = new java.math.BigDecimal("0.80");
    public static final java.math.BigDecimal TY_LE_PHI_NEN_TANG = new java.math.BigDecimal("0.20");

    /** Tính tiền công thực nhận của CTV sau khi trừ 20% phí nền tảng (= 80% giá trị đơn) */
    public static java.math.BigDecimal tinhThuNhapCtv(java.math.BigDecimal thanhTien) {
        if (thanhTien == null || thanhTien.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return java.math.BigDecimal.ZERO;
        }
        return thanhTien.multiply(TY_LE_THU_NHAP_CTV).setScale(0, java.math.RoundingMode.HALF_UP);
    }

    /** Tính phí chiết khấu nền tảng (20% giá trị đơn) */
    public static java.math.BigDecimal tinhPhiNenTang(java.math.BigDecimal thanhTien) {
        if (thanhTien == null || thanhTien.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return java.math.BigDecimal.ZERO;
        }
        return thanhTien.subtract(tinhThuNhapCtv(thanhTien));
    }

    /** Tính tổng số dư ví thu nhập (80%) của CTV từ tất cả các đơn đã hoàn thành */
    public java.math.BigDecimal tinhSoDuViCtv(Integer congTacVienId) {
        if (congTacVienId == null) return java.math.BigDecimal.ZERO;
        List<PhanCongCTV> list = phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(congTacVienId);
        // Gộp các phân công đã xong theo đơn: đơn nhiều dịch vụ chỉ tính phần dịch vụ CTV làm
        Map<DonDatDichVu, List<PhanCongCTV>> xongTheoDon = new LinkedHashMap<>();
        for (PhanCongCTV pc : list) {
            if (pc == null || !PhanCongTheoDichVu.hieuLuc(pc)) continue;
            DonDatDichVu don = pc.getDonDat();
            boolean isCompleted = "HoanThanh".equalsIgnoreCase(pc.getTrangThai())
                    || (don != null && "HoanThanh".equalsIgnoreCase(don.getTrangThai()));
            if (isCompleted && don != null && don.getThanhTien() != null) {
                xongTheoDon.computeIfAbsent(don, k -> new ArrayList<>()).add(pc);
            }
        }
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Map.Entry<DonDatDichVu, List<PhanCongCTV>> e : xongTheoDon.entrySet()) {
            total = total.add(tinhThuNhapCtv(PhanCongTheoDichVu.tienCuaCtv(e.getKey(), e.getValue())));
        }
        return total;
    }

    /**
     * Tiền đơn ứng với phần việc của CTV ở phân công này (giá trị để tính 80% tiền công hiển thị cho CTV):
     * phân công cả đơn = cả đơn; đơn nhiều dịch vụ = các dịch vụ CTV đó nhận trong đơn.
     */
    private java.math.BigDecimal tienDonCuaCtv(PhanCongCTV pc) {
        if (pc == null || pc.getDonDat() == null) return java.math.BigDecimal.ZERO;
        DonDatDichVu don = pc.getDonDat();
        if (pc.getChiTietDonDat() == null || pc.getCongTacVien() == null) {
            return don.getThanhTien() != null ? don.getThanhTien() : java.math.BigDecimal.ZERO;
        }
        Integer ctvId = pc.getCongTacVien().getId();
        Set<Integer> dong = new LinkedHashSet<>();
        dong.add(pc.getChiTietDonDat().getId());
        for (PhanCongCTV x : phanCongCTVRepository.findAllByDonDat_Id(don.getId())) {
            if (!PhanCongTheoDichVu.hieuLuc(x) || x.getCongTacVien() == null || !ctvId.equals(x.getCongTacVien().getId())) continue;
            if (x.getChiTietDonDat() == null) return don.getThanhTien() != null ? don.getThanhTien() : java.math.BigDecimal.ZERO;
            dong.add(x.getChiTietDonDat().getId());
        }
        return PhanCongTheoDichVu.tienTheoDong(don, dong);
    }

    /** Trích xuất danh sách đường dẫn / objectName hình ảnh tài sản từ request */
    private List<String> trichXuatDanhSachAnh(AssignmentActionRequest req) {
        List<String> images = new ArrayList<>();
        if (req == null) return images;
        if (req.getDanhSachHinhAnh() != null) {
            for (String img : req.getDanhSachHinhAnh()) {
                if (img != null && !img.trim().isEmpty()) {
                    images.add(img.trim());
                }
            }
        }
        if (req.getChiTietTaiSanList() != null) {
            for (Map<String, String> item : req.getChiTietTaiSanList()) {
                if (item != null) {
                    String img = item.get("hinhAnh");
                    if (img != null && !img.trim().isEmpty() && !images.contains(img.trim())) {
                        images.add(img.trim());
                    }
                }
            }
        }
        return images;
    }

    /**
     * Lưu Biểu mẫu tài sản (BieuMauTaiSan) và Chi tiết tài sản (ChiTietTaiSan) vào CSDL MySQL,
     * liên kết với object key của ảnh đã tải lên MinIO (don-dat/tai-san/...).
     *
     * @param loaiBieuMau "TruocDichVu" hoặc "SauDichVu"
     */
    public BieuMauTaiSan luuBieuMauTaiSan(DonDatDichVu don, CongTacVien ctv, String loaiBieuMau, AssignmentActionRequest req) {
        if (don == null || ctv == null || bieuMauTaiSanRepository == null || chiTietTaiSanRepository == null) {
            return null;
        }
        String prefix = "TruocDichVu".equalsIgnoreCase(loaiBieuMau) ? "BMT-" : "BMS-";
        String suffix = String.valueOf(System.currentTimeMillis() % 1_000_000L);
        String maBieuMau = prefix + don.getId() + "-" + suffix;
        if (maBieuMau.length() > 20) {
            maBieuMau = maBieuMau.substring(0, 20);
        }

        String ghiChuDacBiet = (req != null && req.getGhiChuTaiSan() != null && !req.getGhiChuTaiSan().trim().isEmpty())
                ? req.getGhiChuTaiSan().trim()
                : ((req != null && req.getKetQuaThucHien() != null && !req.getKetQuaThucHien().trim().isEmpty())
                        ? req.getKetQuaThucHien().trim()
                        : null);

        BieuMauTaiSan bm = BieuMauTaiSan.builder()
                .maBieuMau(maBieuMau)
                .donDat(don)
                .congTacVien(ctv)
                .loaiBieuMau("TruocDichVu".equalsIgnoreCase(loaiBieuMau) ? "TruocDichVu" : "SauDichVu")
                .thoiGianGhiNhan(LocalDateTime.now())
                .xacNhanKhachHang(false)
                .ghiChuDacBiet(ghiChuDacBiet)
                .build();
        bm = bieuMauTaiSanRepository.save(bm);

        String defaultKhuVuc = (req != null && req.getKhuVucVatDung() != null && !req.getKhuVucVatDung().trim().isEmpty())
                ? req.getKhuVucVatDung().trim()
                : "Toàn bộ khu vực thực hiện";
        String defaultMoTa = (req != null && req.getMoTaHienTrang() != null && !req.getMoTaHienTrang().trim().isEmpty())
                ? req.getMoTaHienTrang().trim()
                : ("TruocDichVu".equalsIgnoreCase(loaiBieuMau)
                        ? "Tài sản khu vực thực hiện nguyên vẹn trước khi bắt đầu dịch vụ"
                        : "Đã hoàn thành dịch vụ, tài sản khu vực thực hiện nguyên vẹn");

        if (req != null && req.getChiTietTaiSanList() != null && !req.getChiTietTaiSanList().isEmpty()) {
            for (Map<String, String> ctMap : req.getChiTietTaiSanList()) {
                if (ctMap == null) continue;
                String rawImg = ctMap.get("hinhAnh");
                if (rawImg == null || rawImg.trim().isEmpty()) continue;
                String storedImg = minioService != null ? minioService.toStoredValue(rawImg) : rawImg.trim();
                String kv = ctMap.get("khuVucVatDung") != null && !ctMap.get("khuVucVatDung").trim().isEmpty()
                        ? ctMap.get("khuVucVatDung").trim() : defaultKhuVuc;
                String mt = ctMap.get("moTaHienTrang") != null && !ctMap.get("moTaHienTrang").trim().isEmpty()
                        ? ctMap.get("moTaHienTrang").trim() : defaultMoTa;
                chiTietTaiSanRepository.save(ChiTietTaiSan.builder()
                        .bieuMau(bm)
                        .khuVucVatDung(kv)
                        .moTaHienTrang(mt)
                        .hinhAnh(storedImg)
                        .build());
            }
        } else {
            List<String> images = trichXuatDanhSachAnh(req);
            for (String rawImg : images) {
                String storedImg = minioService != null ? minioService.toStoredValue(rawImg) : rawImg.trim();
                chiTietTaiSanRepository.save(ChiTietTaiSan.builder()
                        .bieuMau(bm)
                        .khuVucVatDung(defaultKhuVuc)
                        .moTaHienTrang(defaultMoTa)
                        .hinhAnh(storedImg)
                        .build());
            }
        }
        return bm;
    }

    /**
     * Lấy danh sách hình ảnh & chi tiết tài sản Trước dịch vụ (TruocDichVu) và Sau dịch vụ (SauDichVu)
     * của một đơn đặt để hiển thị trên App CTV, App Khách hàng, Web Khách hàng và Web CSKH.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> layThongTinTaiSanDon(Integer donDatId) {
        Map<String, Object> res = new LinkedHashMap<>();
        List<String> anhTruoc = new ArrayList<>();
        List<Map<String, Object>> chiTietTruoc = new ArrayList<>();
        List<String> anhSau = new ArrayList<>();
        List<Map<String, Object>> chiTietSau = new ArrayList<>();

        if (donDatId != null && bieuMauTaiSanRepository != null && chiTietTaiSanRepository != null) {
            List<BieuMauTaiSan> bieuMaus = bieuMauTaiSanRepository.findByDonDat_IdOrderByThoiGianGhiNhanAsc(donDatId);
            for (BieuMauTaiSan bm : bieuMaus) {
                boolean isTruoc = "TruocDichVu".equalsIgnoreCase(bm.getLoaiBieuMau());
                List<ChiTietTaiSan> cts = chiTietTaiSanRepository.findByBieuMau_Id(bm.getId());
                for (ChiTietTaiSan ct : cts) {
                    String viewUrl = minioService != null ? minioService.getViewUrl(ct.getHinhAnh()) : ct.getHinhAnh();
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", ct.getId());
                    item.put("bieuMauId", bm.getId());
                    item.put("maBieuMau", bm.getMaBieuMau());
                    item.put("loaiBieuMau", bm.getLoaiBieuMau());
                    item.put("khuVucVatDung", ct.getKhuVucVatDung());
                    item.put("moTaHienTrang", ct.getMoTaHienTrang());
                    item.put("hinhAnh", ct.getHinhAnh());
                    item.put("hinhAnhUrl", viewUrl);
                    item.put("thoiGianGhiNhan", bm.getThoiGianGhiNhan());
                    item.put("ghiChuDacBiet", bm.getGhiChuDacBiet());
                    if (isTruoc) {
                        if (viewUrl != null && !viewUrl.isBlank()) anhTruoc.add(viewUrl);
                        chiTietTruoc.add(item);
                    } else {
                        if (viewUrl != null && !viewUrl.isBlank()) anhSau.add(viewUrl);
                        chiTietSau.add(item);
                    }
                }
            }
        }

        res.put("anhTruocDichVu", anhTruoc);
        res.put("chiTietTruocDichVu", chiTietTruoc);
        res.put("anhSauDichVu", anhSau);
        res.put("chiTietSauDichVu", chiTietSau);
        res.put("daChupTruocDichVu", !anhTruoc.isEmpty());
        res.put("daChupSauDichVu", !anhSau.isEmpty());
        return res;
    }

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
        // Đơn nhiều dịch vụ: CTV có thể có nhiều phân công (mỗi dịch vụ một dòng) – gộp thành một dòng mỗi đơn
        Map<Integer, List<PhanCongCTV>> pcTheoDon = new LinkedHashMap<>();
        for (PhanCongCTV x : list) pcTheoDon.computeIfAbsent(x.getDonDat().getId(), k -> new ArrayList<>()).add(x);
        Set<Integer> donDaLay = new HashSet<>();
        for (PhanCongCTV pc : list) {
            DonDatDichVu don = pc.getDonDat();
            if (!donDaLay.add(don.getId())) continue;
            List<PhanCongCTV> cungDon = pcTheoDon.get(don.getId());
            // Ưu tiên phân công còn hiệu lực (phân công đầu tiên giữ lịch làm việc)
            pc = cungDon.stream().filter(PhanCongTheoDichVu::hieuLuc)
                    .min(Comparator.comparing(PhanCongCTV::getId)).orElse(pc);
            List<PhanCongCTV> hieuLucCuaToi = cungDon.stream().filter(PhanCongTheoDichVu::hieuLuc).toList();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("phanCongId", pc.getId());
            item.put("maPhanCong", pc.getMaPhanCong());
            item.put("trangThaiPhanCong", pc.getTrangThai());
            item.put("trangThaiDon", don.getTrangThai());
            item.put("trangThai", don.getTrangThai());
            item.put("thoiGianPhanCong", pc.getThoiGianPhanCong());
            item.put("donDatId", don.getId());
            item.put("maDonDat", don.getMaDonDat());
            String tenDichVu = PhanCongTheoDichVu.tenDichVuCuaCtv(don, hieuLucCuaToi.isEmpty() ? cungDon : hieuLucCuaToi);
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
            java.math.BigDecimal tienCuaToi = tienDonCuaCtv(pc);
            item.put("thanhTien", tienCuaToi);
            item.put("thanhTienDon", don.getThanhTien());
            item.put("phiNenTang", tinhPhiNenTang(tienCuaToi));
            item.put("tienThucNhan", tinhThuNhapCtv(tienCuaToi));
            item.put("tyLeChietKhau", 20);
            item.put("tyLeThucNhan", 80);
            item.putAll(layThongTinTaiSanDon(don.getId()));
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
        java.math.BigDecimal tienCuaToi = tienDonCuaCtv(pc);
        result.put("thanhTien", tienCuaToi);
        result.put("thanhTienDon", don.getThanhTien());
        result.put("phiNenTang", tinhPhiNenTang(tienCuaToi));
        result.put("tienThucNhan", tinhThuNhapCtv(tienCuaToi));
        result.put("tyLeChietKhau", 20);
        result.put("tyLeThucNhan", 80);

        Map<String, Object> khMap = new LinkedHashMap<>();
        khMap.put("hoTen", don.getKhachHang().getHoTen());
        khMap.put("soDienThoai", don.getKhachHang().getSoDienThoai());
        khMap.put("diaChi", CustomerAddressService.diaChiDayDu(don.getDiaChi()));
            khMap.put("khuVuc", don.getDiaChi() != null && don.getDiaChi().getKhuVuc() != null ? don.getDiaChi().getKhuVuc().getTenKhuVuc() : "");
        result.put("khachHang", khMap);
        result.putAll(layThongTinTaiSanDon(don.getId()));

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

        // Hủy các ca chưa làm của phân công này, để lịch của CTV không còn bị chiếm (ca đã làm giữ lại để tính công)
        for (LichLamViec l : lichLamViecRepository.findByPhanCong_Id(pc.getId())) {
            if ("SapToi".equals(l.getTrangThai())) {
                l.setTrangThai("Huy");
                lichLamViecRepository.save(l);
            }
        }

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
        return startAssignment(phanCongId, null);
    }

    /**
     * Gắn ảnh tài sản (không bắt buộc) vào phân công SAU KHI bắt đầu / hoàn thành thành công:
     * app chỉ tải ảnh lên MinIO khi máy chủ đã nhận thao tác, rồi gửi object key qua đây.
     *
     * @param loaiBieuMau "TruocDichVu" hoặc "SauDichVu"
     */
    @Transactional
    public Map<String, Object> guiAnhTaiSan(Integer phanCongId, String loaiBieuMau, AssignmentActionRequest req) {
        if (!"TruocDichVu".equals(loaiBieuMau) && !"SauDichVu".equals(loaiBieuMau)) {
            throw new IllegalArgumentException("Loại ảnh tài sản phải là TruocDichVu hoặc SauDichVu.");
        }
        PhanCongCTV pc = phanCongCTVRepository.findById(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));
        if (!daBatDauLam(pc)) {
            throw new IllegalArgumentException("Chỉ gửi ảnh tài sản khi công việc đã bắt đầu.");
        }
        if (trichXuatDanhSachAnh(req).isEmpty()) {
            throw new IllegalArgumentException("Chưa có ảnh tài sản để lưu.");
        }
        BieuMauTaiSan bm = luuBieuMauTaiSan(pc.getDonDat(), pc.getCongTacVien(), loaiBieuMau, req);
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("loaiBieuMau", loaiBieuMau);
        res.put("bieuMauId", bm != null ? bm.getId() : null);
        res.put("danhSachHinhAnh", trichXuatDanhSachAnh(req));
        return res;
    }

    /**
     * Bắt đầu ca chỉ đổi trạng thái đơn và buổi làm (phân công vẫn DaXacNhan), nên xét cả buổi làm của CTV:
     * phân công đang làm / đã xong, hoặc có buổi DangThucHien / HoanThanh.
     */
    private boolean daBatDauLam(PhanCongCTV pc) {
        String tt = pc.getTrangThai();
        if ("DangThucHien".equalsIgnoreCase(tt) || "HoanThanh".equalsIgnoreCase(tt)) return true;
        List<LichLamViec> sches = lichLamViecRepository.findByPhanCong_Id(pc.getId());
        if (sches.isEmpty() && pc.getDonDat() != null) {
            // Đơn nhiều dịch vụ: lịch gắn với phân công đầu tiên của CTV trong đơn – chỉ lấy lịch của chính CTV này
            Integer idCtv = pc.getCongTacVien() != null ? pc.getCongTacVien().getId() : null;
            sches = lichLamViecRepository.findByPhanCong_DonDat_Id(pc.getDonDat().getId()).stream()
                    .filter(l -> idCtv == null || (l.getCongTacVien() != null && idCtv.equals(l.getCongTacVien().getId())))
                    .toList();
        }
        return sches.stream().anyMatch(l -> "DangThucHien".equalsIgnoreCase(l.getTrangThai())
                || "HoanThanh".equalsIgnoreCase(l.getTrangThai()));
    }

    @Transactional
    public Map<String, Object> startAssignment(Integer phanCongId, AssignmentActionRequest req) {
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
            LocalDateTime latestStart = (gioKetThuc != null && gioKetThuc.isBefore(gioBatDau)) ? LocalDateTime.of(ngayLam.plusDays(1), gioKetThuc) : LocalDateTime.of(ngayLam, gioKetThuc != null ? gioKetThuc : gioBatDau.plusHours(2));

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

        // Ảnh tài sản trước khi làm không bắt buộc: có ảnh thì lưu biểu mẫu
        List<String> anhTruocMoi = trichXuatDanhSachAnh(req);
        if (!anhTruocMoi.isEmpty() && pc.getCongTacVien() != null) {
            luuBieuMauTaiSan(don, pc.getCongTacVien(), "TruocDichVu", req);
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
                .ghiChu("CTV đã chụp ảnh tài sản trước khi làm và bắt đầu thực hiện ca làm việc")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("donDatId", don.getId());
        res.put("trangThaiDon", "DangThucHien");
        res.put("trangThaiPhanCong", pc.getTrangThai());
        res.putAll(layThongTinTaiSanDon(don.getId()));
        res.put("message", "Đã gửi ảnh tài sản trước khi làm và bắt đầu thực hiện ca làm việc!");
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
                if (req != null && req.get("danhSachHinhAnh") instanceof List<?> rawList && !rawList.isEmpty() && pc.getCongTacVien() != null) {
                    AssignmentActionRequest taiSanReq = new AssignmentActionRequest();
                    List<String> dsAnh = new ArrayList<>();
                    for (Object o : rawList) {
                        if (o != null && !o.toString().isBlank()) dsAnh.add(o.toString().trim());
                    }
                    taiSanReq.setDanhSachHinhAnh(dsAnh);
                    if (req.get("khuVucVatDung") != null) taiSanReq.setKhuVucVatDung(req.get("khuVucVatDung").toString());
                    if (req.get("moTaHienTrang") != null) taiSanReq.setMoTaHienTrang(req.get("moTaHienTrang").toString());
                    if (req.get("ghiChuTaiSan") != null) taiSanReq.setGhiChuTaiSan(req.get("ghiChuTaiSan").toString());
                    luuBieuMauTaiSan(don, pc.getCongTacVien(), "TruocDichVu", taiSanReq);
                }
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
                res.putAll(layThongTinTaiSanDon(don.getId()));
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
            // Đơn nhiều dịch vụ: lịch gắn với phân công đầu tiên của CTV trong đơn – chỉ lấy lịch của chính CTV này
            Integer idCtvLich = pc.getCongTacVien() != null ? pc.getCongTacVien().getId() : null;
            sches = new ArrayList<>(lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()).stream()
                    .filter(l -> idCtvLich == null || (l.getCongTacVien() != null && idCtvLich.equals(l.getCongTacVien().getId())))
                    .toList());
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
        // Bấm hoàn thành lần nữa (mạng chập chờn, gọi lại API) khi không còn buổi nào để làm thì không ghi thêm gì
        if (activeSession == null && "HoanThanh".equalsIgnoreCase(pc.getTrangThai())) {
            throw new IllegalArgumentException("Công việc này đã hoàn thành trước đó.");
        }

        // Ảnh tài sản sau khi làm không bắt buộc: có ảnh thì lưu biểu mẫu
        List<String> anhSauMoi = trichXuatDanhSachAnh(req);
        if (!anhSauMoi.isEmpty() && pc.getCongTacVien() != null) {
            luuBieuMauTaiSan(don, pc.getCongTacVien(), "SauDichVu", req);
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
            // CTV xong phần mình: mọi phân công của CTV này trong đơn hoàn thành;
            // đơn chỉ hoàn thành khi mọi CTV của đơn đã xong (đơn nhiều dịch vụ)
            Integer idCtvXong = pc.getCongTacVien() != null ? pc.getCongTacVien().getId() : null;
            pc.setTrangThai("HoanThanh");
            phanCongCTVRepository.save(pc);
            List<PhanCongCTV> pcsDon = phanCongCTVRepository.findAllByDonDat_Id(don.getId());
            for (PhanCongCTV x : pcsDon) {
                if (x.getId().equals(pc.getId())) {
                    x.setTrangThai("HoanThanh");
                } else if (idCtvXong != null && x.getCongTacVien() != null && idCtvXong.equals(x.getCongTacVien().getId())
                        && "DaXacNhan".equals(x.getTrangThai())) {
                    x.setTrangThai("HoanThanh");
                    phanCongCTVRepository.save(x);
                }
            }
            if (PhanCongTheoDichVu.moiPhanCongXong(pcsDon)) {
                don.setTrangThai("HoanThanh");
                donDatDichVuRepository.save(don);
            }
        }

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("DangThucHien")
                .trangThaiMoi(nextRemaining != null ? "DaXacNhan" : don.getTrangThai())
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

        java.math.BigDecimal tongTienDon = tienDonCuaCtv(pc); // đơn nhiều dịch vụ: phần dịch vụ CTV này làm
        java.math.BigDecimal phiNenTang = tinhPhiNenTang(tongTienDon);
        java.math.BigDecimal tienThucNhan = tinhThuNhapCtv(tongTienDon);
        String tongTienStr = String.format("%,d", tongTienDon.longValue()) + "đ";
        String phiStr = String.format("%,d", phiNenTang.longValue()) + "đ";
        String thucNhanStr = String.format("%,d", tienThucNhan.longValue()) + "đ";

        // 1. Tạo thông báo hoàn thành đơn cho CTV (ghi rõ nhiệm vụ gì, ngày nào, mấy giờ, chiết khấu 20% & thực nhận 80%)
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String prefixCTV = "CTV " + tenCTV;
            String chiTietTien = tongTienDon.compareTo(java.math.BigDecimal.ZERO) > 0
                    ? ". Tổng đơn: " + tongTienStr + " | Phí nền tảng (20%): -" + phiStr + " | Tiền công thực nhận (80%): +" + thucNhanStr + " đã được cộng vào Ví thu nhập"
                    : "";
            ThongBao tbComplete = ThongBao.builder()
                    .maThongBao("TB-CTV-HT-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .tieuDe(prefixCTV + " – Hoàn thành nhiệm vụ: " + don.getMaDonDat())
                    .noiDung(prefixCTV + " đã hoàn thành nhiệm vụ " + tenDichVu + " (Đơn " + don.getMaDonDat() + ") vào lúc " + gioHoanThanh + ", " + thu + " (ngày " + ngayHoanThanh + ")"
                            + chiTietTien
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
        java.math.BigDecimal soDuViMoi = pc.getCongTacVien() != null
                ? tinhSoDuViCtv(pc.getCongTacVien().getId())
                : tienThucNhan;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("donDatId", don.getId());
        res.put("donDatTrangThai", don.getTrangThai());
        res.put("tongTienDon", tongTienDon);
        res.put("phiNenTang", phiNenTang);
        res.put("tienThucNhan", tienThucNhan);
        res.put("tyLeChietKhau", 20);
        res.put("tyLeThucNhan", 80);
        res.put("soDuVi", soDuViMoi);
        res.putAll(layThongTinTaiSanDon(don.getId()));
        res.put("message", "Đã gửi ảnh tài sản sau dịch vụ & xác nhận hoàn thành! +" + thucNhanStr + " (80% tiền công) đã được cộng vào Ví thu nhập.");
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
                        java.math.BigDecimal tong = tienDonCuaCtv(pc);
                        java.math.BigDecimal thucNhan = tinhThuNhapCtv(tong);
                        String tienStr = thucNhan.compareTo(java.math.BigDecimal.ZERO) > 0
                                ? String.format("%,d", thucNhan.longValue()) + "đ (80% tiền công)"
                                : "";
                        LocalDateTime tg = pc.getThoiGianXacNhan() != null ? pc.getThoiGianXacNhan()
                                : (pc.getThoiGianPhanCong() != null ? pc.getThoiGianPhanCong() : LocalDateTime.now());
                        ThongBao tbComplete = ThongBao.builder()
                                .maThongBao("TB-HT-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                                .tieuDe("Đơn " + maDon + " đã hoàn thành")
                                .noiDung("Bạn thực nhận +" + tienStr + " từ đơn " + maDon + " (đã trừ 20% phí nền tảng). Tiền đã được cộng vào Ví thu nhập!")
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
                    List<PhanCongCTV> pcCuaToi = phanCongCTVRepository.findAllByDonDat_Id(d.getId()).stream()
                            .filter(x -> PhanCongTheoDichVu.hieuLuc(x) && x.getCongTacVien() != null
                                    && x.getCongTacVien().getId().equals(congTacVienId)).toList();
                    String tenDv = PhanCongTheoDichVu.tenDichVuCuaCtv(d, pcCuaToi.isEmpty() ? List.of(pc) : pcCuaToi);
                    item.put("tenDichVu", tenDv);
                    String fullDiaChi = CustomerAddressService.diaChiDayDu(d.getDiaChi());
                    item.put("diaChi", fullDiaChi);
                    item.put("diaChiChiTiet", fullDiaChi);
                    item.put("khuVuc", d.getDiaChi() != null && d.getDiaChi().getKhuVuc() != null ? d.getDiaChi().getKhuVuc().getTenKhuVuc() : "");
                    item.put("khachHangTen", d.getKhachHang() != null ? d.getKhachHang().getHoTen() : "Khách hàng");
                    item.put("khachHangPhone", d.getKhachHang() != null ? d.getKhachHang().getSoDienThoai() : "");
                    item.put("yeuCauDacBiet", d.getYeuCauDacBiet());
                    java.math.BigDecimal tienCuaToi = tienDonCuaCtv(pcCuaToi.isEmpty() ? pc : pcCuaToi.get(0));
                    item.put("thanhTien", tienCuaToi);
                    item.put("thanhTienDon", d.getThanhTien());
                    item.put("phiNenTang", tinhPhiNenTang(tienCuaToi));
                    item.put("tienThucNhan", tinhThuNhapCtv(tienCuaToi));
                    item.put("tyLeChietKhau", 20);
                    item.put("tyLeThucNhan", 80);
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
                    item.putAll(layThongTinTaiSanDon(d.getId()));
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
            java.math.BigDecimal tienCuaToi = tienDonCuaCtv(llv.getPhanCong());
            result.put("thanhTien", tienCuaToi);
            result.put("thanhTienDon", d.getThanhTien());
            result.put("phiNenTang", tinhPhiNenTang(tienCuaToi));
            result.put("tienThucNhan", tinhThuNhapCtv(tienCuaToi));
            result.put("tyLeChietKhau", 20);
            result.put("tyLeThucNhan", 80);
            result.put("khachHangTen", d.getKhachHang().getHoTen());
            result.put("khachHangPhone", d.getKhachHang().getSoDienThoai());
            result.putAll(layThongTinTaiSanDon(d.getId()));
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
        java.math.BigDecimal soDuVi = tinhSoDuViCtv(ctv.getId());
        m.put("soDuVi", soDuVi);
        m.put("tongThuNhap", soDuVi);
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
        // Kỹ năng CTV: đơn nhiều dịch vụ chỉ hiện khi còn dịch vụ trống mà CTV làm được
        Set<Integer> dichVuCtv = dichVuCTVRepository == null ? Set.of() : dichVuCTVRepository.findByCongTacVien_Id(ctvId).stream()
                .filter(d -> "HoatDong".equalsIgnoreCase(d.getTrangThai()) && d.getDichVu() != null)
                .map(d -> d.getDichVu().getId()).collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime bayGio = LocalDateTime.now();
        for (DonDatDichVu don : donList) {
            LocalDateTime hanNhan = hanNhanDonService.hanNhan(don);
            // Chưa phát hành (chờ khách chuyển khoản) hoặc đã hết thời gian nhận thì không hiện cho CTV
            if (hanNhan == null || bayGio.isAfter(hanNhan)) continue;
            // CTV đã bấm bỏ qua đơn này thì không hiện lại
            if (boQuaDonStore != null && boQuaDonStore.daBoQua(ctvId, don.getId())) continue;
            // Lọc theo khu vực: nếu xác định được khu vực của CTV thì chỉ lấy đơn cùng khu vực
            if (quanHuyenCtv != null || !khuVucIds.isEmpty()) {
                if (!isDonInKhuVuc(don, quanHuyenCtv, khuVucIds, allKhuVuc)) {
                    continue; // Bỏ qua đơn ngoài khu vực
                }
            }

            // Đơn nhiều dịch vụ: từng dịch vụ (đã có người / CTV có kỹ năng); không còn dòng nào nhận được thì bỏ qua
            List<ChiTietDonDat> cts = don.getChiTietList() != null ? don.getChiTietList() : List.of();
            List<Map<String, Object>> dichVus = new ArrayList<>();
            if (cts.size() >= 2) {
                List<PhanCongCTV> pcs = phanCongCTVRepository.findAllByDonDat_Id(don.getId());
                Set<Integer> conTrong = PhanCongTheoDichVu.chiTietConTrong(cts, pcs);
                boolean nhanDuoc = false;
                for (ChiTietDonDat ct : cts) {
                    Integer dvId = ct.getDichVu() != null ? ct.getDichVu().getId() : null;
                    boolean coNguoi = !conTrong.contains(ct.getId());
                    boolean coKyNang = dichVuCtv.contains(dvId);
                    if (!coNguoi && coKyNang) nhanDuoc = true;
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("chiTietId", ct.getId());
                    d.put("dichVuId", dvId);
                    d.put("tenDichVu", ct.getDichVu() != null ? ct.getDichVu().getTenDichVu() : "Dịch vụ");
                    d.put("thoiGianPhut", ct.getDichVu() != null ? DatNhieuDichVu.phut(ct.getDichVu()) : null);
                    d.put("daCoNguoiNhan", coNguoi);
                    d.put("ctvCoKyNang", coKyNang);
                    dichVus.add(d);
                }
                if (!nhanDuoc) continue;
                // CTV trùng lịch trên khung giờ của đơn thì không hiện (nhận cũng sẽ bị từ chối)
                PhanCongCTV pcCuaToi = pcs.stream().filter(x -> PhanCongTheoDichVu.hieuLuc(x) && x.getCongTacVien() != null
                        && ctvId.equals(x.getCongTacVien().getId())).findFirst().orElse(null);
                if (phanCongService.lyDoTrungLich(ctvId, don, pcCuaToi != null ? pcCuaToi.getId() : null) != null) continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("donDatId", don.getId());
            item.put("dichVus", dichVus);
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
            item.put("phiNenTang", tinhPhiNenTang(don.getThanhTien()));
            item.put("tienThucNhan", tinhThuNhapCtv(don.getThanhTien()));
            item.put("tyLeChietKhau", 20);
            item.put("tyLeThucNhan", 80);
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
     * CTV nhận đơn từ pool (first-come-first-served): khóa dòng đơn (SELECT ... FOR UPDATE) nên nhiều CTV
     * bấm cùng lúc được xử lý lần lượt, người sau thấy dữ liệu người trước đã lưu.
     */
    @Transactional
    public Map<String, Object> acceptOrderFromPool(Integer congTacVienId, Integer donDatId) {
        return acceptOrderFromPool(congTacVienId, donDatId, null);
    }

    /**
     * Nhận đơn từ pool. Đơn một dịch vụ: nhận cả đơn như trước. Đơn nhiều dịch vụ (giỏ hàng): CTV nhận các dòng
     * [chiTietIds] (rỗng = mọi dòng còn trống mà CTV có kỹ năng), mỗi dòng một phân công; đơn chỉ sang
     * DaXacNhan khi mọi dịch vụ đã có người (PhanCongTheoDichVu).
     */
    @Transactional
    public Map<String, Object> acceptOrderFromPool(Integer congTacVienId, Integer donDatId, List<Integer> chiTietIds) {
        // Resolve congTacVienId
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId).orElse(null);
        if (ctv == null) {
            TaiKhoan tk = taiKhoanRepository.findById(congTacVienId).orElse(null);
            if (tk != null) ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
        }
        if (ctv == null) throw new IllegalArgumentException("Không tìm thấy CTV ID: " + congTacVienId);
        if (ctv.getTaiKhoan() == null) throw new IllegalStateException("CTV chưa có tài khoản");

        DonDatDichVu don = donDatDichVuRepository.findByIdForUpdate(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ID: " + donDatId));
        if (!"ChoDuyet".equals(don.getTrangThai())) {
            throw new IllegalStateException("Đơn này vừa có người nhận rồi! Vui lòng chọn đơn khác.");
        }
        // Hết thời gian nhận đơn (15 phút kể từ lúc phát hành): báo lỗi, không nhận gì
        hanNhanDonService.kiemTraConHan(don);

        List<ChiTietDonDat> cts = don.getChiTietList() != null ? don.getChiTietList() : List.of();
        boolean nhieu = cts.size() >= 2;
        List<PhanCongCTV> pcsCu = phanCongCTVRepository.findAllByDonDat_Id(don.getId());
        final Integer idCtv = ctv.getId();
        // Phân công đang hiệu lực khác của chính CTV này trong đơn (đã có lịch): không tạo lịch lần hai
        PhanCongCTV pcCuaToi = pcsCu.stream()
                .filter(x -> PhanCongTheoDichVu.hieuLuc(x) && x.getCongTacVien() != null && idCtv.equals(x.getCongTacVien().getId()))
                .findFirst().orElse(null);

        List<ChiTietDonDat> dongNhan = new ArrayList<>();
        if (nhieu) {
            Set<Integer> chiTietCuaDon = new LinkedHashSet<>();
            Map<Integer, Integer> dichVuCuaChiTiet = new HashMap<>();
            for (ChiTietDonDat ct : cts) {
                chiTietCuaDon.add(ct.getId());
                dichVuCuaChiTiet.put(ct.getId(), ct.getDichVu() != null ? ct.getDichVu().getId() : null);
            }
            Set<Integer> dichVuCtv = dichVuCTVRepository.findByCongTacVien_Id(idCtv).stream()
                    .filter(d -> "HoatDong".equalsIgnoreCase(d.getTrangThai()) && d.getDichVu() != null)
                    .map(d -> d.getDichVu().getId()).collect(Collectors.toSet());
            Set<Integer> conTrong = PhanCongTheoDichVu.chiTietConTrong(cts, pcsCu);
            List<Integer> muonNhan = chiTietIds == null || chiTietIds.isEmpty()
                    ? PhanCongTheoDichVu.chonMacDinh(conTrong, dichVuCtv, dichVuCuaChiTiet) : chiTietIds;
            if (muonNhan.isEmpty()) throw new IllegalStateException("Bạn chưa đăng ký kỹ năng cho dịch vụ nào còn trống trong đơn này.");
            String loi = PhanCongTheoDichVu.loiNhan(chiTietCuaDon, conTrong, dichVuCtv, dichVuCuaChiTiet, muonNhan);
            if (loi != null) throw new IllegalStateException(loi);
            for (ChiTietDonDat ct : cts) if (muonNhan.contains(ct.getId())) dongNhan.add(ct);
        }

        // Các buổi cần làm: gói tháng tính lại từ ngày thực hiện, trừ số buổi CTV trước đã hoàn thành
        List<LocalDate> ngayCanLam = new ArrayList<>(PhanCongService.cacBuoi(don));
        long soBuoiDaXong = lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId()).stream()
                .filter(l -> "HoanThanh".equalsIgnoreCase(l.getTrangThai())).count();
        if (soBuoiDaXong > 0 && ngayCanLam.size() > soBuoiDaXong) {
            ngayCanLam = new ArrayList<>(ngayCanLam.subList(0, (int) (ngayCanLam.size() - soBuoiDaXong)));
        }
        LocalTime gioKT = PhanCongService.gioKetThuc(don);

        // Kiểm tra trùng lịch của CTV trên mọi buổi (bỏ qua lịch của chính CTV trong đơn này); lỗi thì rollback
        congTacVienRepository.findByIdForUpdate(idCtv);
        phanCongService.kiemTraDonHoacBaoLoi(idCtv, don, pcCuaToi != null ? pcCuaToi.getId() : null, false);

        // Đơn một dịch vụ: một phân công cả đơn; nhiều dịch vụ: mỗi dòng CTV nhận một phân công
        List<PhanCongCTV> pcMoi = new ArrayList<>();
        for (ChiTietDonDat dong : nhieu ? dongNhan : java.util.Collections.<ChiTietDonDat>singletonList(null)) {
            pcMoi.add(phanCongCTVRepository.save(PhanCongCTV.builder()
                    .maPhanCong(MaSinh.tao("PC-"))
                    .donDat(don)
                    .congTacVien(ctv)
                    .chiTietDonDat(dong)
                    .trangThai("DaXacNhan")
                    .thoiGianPhanCong(LocalDateTime.now())
                    .thoiGianXacNhan(LocalDateTime.now())
                    .build()));
        }
        PhanCongCTV pc = pcMoi.get(0);

        // Tạo Lịch làm việc (gói tháng: mỗi buổi còn lại một lịch) – mỗi CTV một lịch cho cả đơn
        if (pcCuaToi == null) phanCongService.taoLichLamViec(pc, ngayCanLam, don.getGioBatDau(), gioKT, null);

        List<PhanCongCTV> tatCaPc = new ArrayList<>(pcsCu);
        tatCaPc.addAll(pcMoi);
        boolean duNguoi = !nhieu || PhanCongTheoDichVu.duNguoi(cts, tatCaPc);
        if (duNguoi) {
            don.setTrangThai("DaXacNhan");
            donDatDichVuRepository.save(don);
        }

        // Lưu lịch sử
        String tenDongNhan = dongNhan.stream().map(ct -> ct.getDichVu() != null ? ct.getDichVu().getTenDichVu() : "Dịch vụ")
                .collect(Collectors.joining(", "));
        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("ChoDuyet")
                .trangThaiMoi(duNguoi ? "DaXacNhan" : "ChoDuyet")
                .nguoiThucHien("CongTacVien: " + ctv.getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu(!nhieu ? "CTV tự nhận đơn từ pool qua Mobile App"
                        : "CTV nhận: " + tenDongNhan + (duNguoi ? ". Đủ cộng tác viên cho mọi dịch vụ" : ""))
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
        res.put("phanCongIds", pcMoi.stream().map(PhanCongCTV::getId).toList());
        res.put("maDonDat", don.getMaDonDat());
        res.put("trangThai", duNguoi ? "DaXacNhan" : "ChoDuyet");
        res.put("duNguoi", duNguoi);
        res.put("message", duNguoi ? "Đã nhận đơn thành công!"
                : "Đã nhận " + tenDongNhan + ". Đơn đang chờ cộng tác viên cho các dịch vụ còn lại.");
        return res;
    }

    /**
     * CTV bỏ qua / từ chối đơn từ pool trên ứng dụng.
     * Không chèn bản ghi vào DB: lưu trên Redis để đơn không hiện lại với CTV này (kể cả khi mở lại app).
     */
    public Map<String, Object> rejectOrderFromPool(Integer congTacVienId, Integer donDatId, String lyDo) {
        // App có thể gửi id tài khoản thay cho id CTV – quy về id CTV như danh sách việc mới
        Integer ctvId = congTacVienRepository.findById(congTacVienId).map(CongTacVien::getId)
                .or(() -> congTacVienRepository.findByTaiKhoan_Id(congTacVienId).map(CongTacVien::getId))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên ID: " + congTacVienId));
        if (!donDatDichVuRepository.existsById(donDatId)) {
            throw new IllegalArgumentException("Không tìm thấy đơn ID: " + donDatId);
        }
        if (boQuaDonStore != null) boQuaDonStore.boQua(ctvId, donDatId, lyDo);
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

