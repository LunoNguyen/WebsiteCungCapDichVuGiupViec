package com.example.Service;

import com.example.Config.CacheNames;
import com.example.DTO.view.DanhGiaView;
import com.example.DTO.view.DanhMucView;
import com.example.DTO.view.DichVuNoiBatView;
import com.example.DTO.view.DichVuView;
import com.example.DTO.view.KhuyenMaiView;
import com.example.Model.BangGiaDichVu;
import com.example.Model.ChuongTrinhKhuyenMai;
import com.example.Model.DanhGia;
import com.example.Model.DichVu;
import com.example.Model.KhuVuc;
import com.example.Model.LoaiDichVu;
import com.example.Model.MaKhuyenMai;
import com.example.Repository.BangGiaDichVuRepository;
import com.example.Repository.ChuongTrinhKhuyenMaiRepository;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.DanhGiaRepository;
import com.example.Repository.DichVuRepository;
import com.example.Repository.KhachHangRepository;
import com.example.Repository.KhuVucRepository;
import com.example.Repository.LoaiDichVuRepository;
import com.example.Repository.MaKhuyenMaiRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Dữ liệu cho website công khai (trang chủ, giới thiệu, dịch vụ, khuyến mãi).
 *
 * Các dữ liệu này được đọc rất nhiều nhưng hiếm khi thay đổi, nên kết quả được lưu vào Redis.
 * Khi nhân viên thêm/sửa/xóa danh mục, dịch vụ, bảng giá, khuyến mãi, đánh giá thì
 * CacheInvalidationInterceptor sẽ xóa cache tương ứng.
 *
 * Lưu ý: @Cacheable chỉ có tác dụng khi gọi từ lớp khác (qua proxy của Spring),
 * nên các hàm xử lý phụ bên dưới là hàm static nhận dữ liệu đã lấy sẵn.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Ảnh mặc định theo mã danh mục, dùng khi danh mục chưa được tải ảnh lên MinIO. */
    private static final Map<String, String> ANH_MAC_DINH = Map.of(
            "LDV-001", "/images/services/don-dep-nha-cua.png",
            "LDV-002", "/images/services/ve-sinh-may-lanh.png",
            "LDV-003", "/images/services/trong-tre.jpg",
            "LDV-004", "/images/services/nau-an-gia-dinh.jpg",
            "LDV-005", "/images/services/ve-sinh-van-phong.png");
    private static final String ANH_DU_PHONG = "/images/services/don-dep-nha-cua.png";

    /** Dịch vụ phổ biến ở trang chủ: {mã dịch vụ, tên hiển thị, ảnh}. Giá lấy từ CSDL. */
    private static final String[][] NOI_BAT = {
            {"DV-001", "Dọn dẹp nhà theo giờ", "/images/services/don-dep-nha-cua.png"},
            {"DV-051", "Vệ sinh máy lạnh", "/images/services/ve-sinh-may-lanh.png"},
            {"DV-123", "Nấu ăn gia đình", "/images/services/nau-an-gia-dinh.jpg"},
            {"DV-072", "Trông trẻ tại nhà", "/images/services/trong-tre.jpg"},
            {"DV-079", "Chăm sóc người cao tuổi", "/images/services/cham-soc-nguoi-cao-tuoi.png"},
            {"DV-129", "Vệ sinh văn phòng", "/images/services/ve-sinh-van-phong.png"}
    };

    private final LoaiDichVuRepository loaiDichVuRepository;
    private final DichVuRepository dichVuRepository;
    private final BangGiaDichVuRepository bangGiaDichVuRepository;
    private final KhuVucRepository khuVucRepository;
    private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
    private final MaKhuyenMaiRepository maKhuyenMaiRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final KhachHangRepository khachHangRepository;
    private final MinioService minioService;

    public CatalogService(LoaiDichVuRepository loaiDichVuRepository,
                          DichVuRepository dichVuRepository,
                          BangGiaDichVuRepository bangGiaDichVuRepository,
                          KhuVucRepository khuVucRepository,
                          ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository,
                          MaKhuyenMaiRepository maKhuyenMaiRepository,
                          DanhGiaRepository danhGiaRepository,
                          CongTacVienRepository congTacVienRepository,
                          KhachHangRepository khachHangRepository,
                          MinioService minioService) {
        this.loaiDichVuRepository = loaiDichVuRepository;
        this.dichVuRepository = dichVuRepository;
        this.bangGiaDichVuRepository = bangGiaDichVuRepository;
        this.khuVucRepository = khuVucRepository;
        this.chuongTrinhKhuyenMaiRepository = chuongTrinhKhuyenMaiRepository;
        this.maKhuyenMaiRepository = maKhuyenMaiRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.khachHangRepository = khachHangRepository;
        this.minioService = minioService;
    }

    // =========================================================================
    // DANH MỤC + DỊCH VỤ + GIÁ
    // =========================================================================

    /** Các danh mục đang hiển thị, kèm dịch vụ đang hoạt động và giá hiện tại. */
    @Cacheable(value = CacheNames.DANH_MUC_DICH_VU, key = "'tatCa'")
    public List<DanhMucView> getDanhMuc() {
        List<LoaiDichVu> loaiList = loaiDichVuRepository.findAll(
                Sort.by(Sort.Direction.ASC, "thuTuHienThi").and(Sort.by(Sort.Direction.ASC, "id")));

        Map<Integer, DanhMucView> theoId = new LinkedHashMap<>();
        Map<Integer, BigDecimal> giaThapNhat = new LinkedHashMap<>();
        for (LoaiDichVu loai : loaiList) {
            if (!"HienThi".equalsIgnoreCase(loai.getTrangThai())) {
                continue;
            }
            String anh = minioService.getViewUrl(loai.getHinhAnh());
            if (anh == null) {
                anh = ANH_MAC_DINH.getOrDefault(loai.getMaLoaiDichVu(), ANH_DU_PHONG);
            }
            theoId.put(loai.getId(), DanhMucView.builder()
                    .id(loai.getId())
                    .maLoaiDichVu(loai.getMaLoaiDichVu())
                    .tenLoaiDichVu(loai.getTenLoaiDichVu())
                    .moTa(loai.getMoTa())
                    .hinhAnhUrl(anh)
                    .giaTuText("")
                    .build());
        }

        List<DichVu> dichVus = new ArrayList<>(dichVuRepository.findByTrangThai("HoatDong"));
        dichVus.sort(Comparator.comparing(DichVu::getId));
        for (DichVu dv : dichVus) {
            DanhMucView danhMuc = dv.getLoaiDichVu() != null ? theoId.get(dv.getLoaiDichVu().getId()) : null;
            if (danhMuc == null) {
                continue;
            }
            BigDecimal gia = giaHienTai(dv);
            DichVuView view = DichVuView.builder()
                    .id(dv.getId())
                    .maDichVu(dv.getMaDichVu())
                    .tenDichVu(dv.getTenDichVu())
                    .moTa(dv.getMoTaChiTiet())
                    .loaiHinhDat(dv.getLoaiHinhDat())
                    .donViTinh(dv.getDonViTinh())
                    .thoiGianText(dinhDangThoiGian(dv.getThoiGianThucHien()))
                    .soBuoi(dv.getSoBuoi())
                    .soNguoiThucHien(dv.getSoNguoiThucHien())
                    .gia(gia)
                    .giaText(dinhDangTien(gia))
                    .build();

            if ("GoiThang".equalsIgnoreCase(dv.getLoaiHinhDat())) {
                danhMuc.getDichVuGoiThang().add(view);
            } else {
                danhMuc.getDichVuTheoLan().add(view);
            }
            danhMuc.setSoDichVu(danhMuc.getSoDichVu() + 1);
            if (danhMuc.getTieuBieu().size() < 4) {
                String tenNgan = tenNgan(dv.getTenDichVu());
                if (!danhMuc.getTieuBieu().contains(tenNgan)) {
                    danhMuc.getTieuBieu().add(tenNgan);
                }
            }
            if (gia.signum() > 0) {
                giaThapNhat.merge(danhMuc.getId(), gia, BigDecimal::min);
            }
        }

        for (DanhMucView danhMuc : theoId.values()) {
            BigDecimal min = giaThapNhat.get(danhMuc.getId());
            if (min != null) {
                danhMuc.setGiaTuText("từ " + dinhDangTien(min));
            }
        }
        return new ArrayList<>(theoId.values());
    }

    /** Giá hiện tại của dịch vụ (cột DichVu.GiaHienTai); nếu chưa có thì lấy từ lịch sử bảng giá đang áp dụng. */
    private BigDecimal giaHienTai(DichVu dv) {
        BigDecimal gia = dv.getGiaHienTai();
        if (gia != null && gia.signum() > 0) {
            return gia;
        }
        List<BangGiaDichVu> bangGias = bangGiaDichVuRepository.findByDichVu_IdAndTrangThai(dv.getId(), "DangApDung");
        if (!bangGias.isEmpty() && bangGias.get(0).getDonGia() != null) {
            return bangGias.get(0).getDonGia();
        }
        return BigDecimal.ZERO;
    }

    /** Tìm một danh mục trong danh sách đã lấy từ {@link #getDanhMuc()}. */
    public static DanhMucView timDanhMuc(List<DanhMucView> danhMucs, Integer id) {
        if (danhMucs == null || id == null) {
            return null;
        }
        for (DanhMucView danhMuc : danhMucs) {
            if (id.equals(danhMuc.getId())) {
                return danhMuc;
            }
        }
        return null;
    }

    /** Chọn các dịch vụ phổ biến cho trang chủ từ danh sách đã lấy từ {@link #getDanhMuc()}. */
    public static List<DichVuNoiBatView> chonDichVuNoiBat(List<DanhMucView> danhMucs) {
        List<DichVuNoiBatView> ketQua = new ArrayList<>();
        if (danhMucs == null) {
            return ketQua;
        }
        for (String[] cauHinh : NOI_BAT) {
            BigDecimal min = null;
            Integer danhMucId = null;
            for (DanhMucView danhMuc : danhMucs) {
                List<DichVuView> tatCa = new ArrayList<>(danhMuc.getDichVuTheoLan());
                tatCa.addAll(danhMuc.getDichVuGoiThang());
                for (DichVuView dv : tatCa) {
                    if (!cauHinh[0].equalsIgnoreCase(dv.getMaDichVu())) {
                        continue;
                    }
                    danhMucId = danhMuc.getId();
                    if (dv.getGia() != null && dv.getGia().signum() > 0 && (min == null || dv.getGia().compareTo(min) < 0)) {
                        min = dv.getGia();
                    }
                }
            }
            if (danhMucId != null) {
                ketQua.add(DichVuNoiBatView.builder()
                        .ten(cauHinh[1])
                        .hinhAnhUrl(cauHinh[2])
                        .giaTuText(min != null ? "từ " + dinhDangTien(min) : "Liên hệ")
                        .danhMucId(danhMucId)
                        .build());
            }
        }
        return ketQua;
    }

    // =========================================================================
    // KHU VỰC PHỤC VỤ
    // =========================================================================

    /** Tỉnh/thành → danh sách quận/huyện đang phục vụ. */
    @Cacheable(value = CacheNames.KHU_VUC, key = "'theoTinhThanh'")
    public Map<String, List<String>> getKhuVucTheoTinhThanh() {
        Map<String, List<String>> ketQua = new TreeMap<>();
        for (KhuVuc kv : khuVucRepository.findAll()) {
            if (!"HoatDong".equalsIgnoreCase(kv.getTrangThai()) || kv.getTinhThanh() == null) {
                continue;
            }
            List<String> quanHuyen = ketQua.computeIfAbsent(kv.getTinhThanh().trim(), k -> new ArrayList<>());
            String ten = kv.getQuanHuyen() != null ? kv.getQuanHuyen().trim() : "";
            if (!ten.isEmpty() && !quanHuyen.contains(ten)) {
                quanHuyen.add(ten);
            }
        }
        return new LinkedHashMap<>(ketQua);
    }

    /** id khu vực → "Quận/huyện, Tỉnh/thành" cho form đăng ký cộng tác viên. */
    @Cacheable(value = CacheNames.KHU_VUC, key = "'choDangKy'")
    public Map<Integer, String> getKhuVucChoDangKy() {
        List<KhuVuc> khuVucs = new ArrayList<>(khuVucRepository.findAll());
        khuVucs.removeIf(kv -> !"HoatDong".equalsIgnoreCase(kv.getTrangThai()));
        khuVucs.sort(Comparator.comparing((KhuVuc kv) -> kv.getTinhThanh() == null ? "" : kv.getTinhThanh())
                .thenComparing(kv -> kv.getQuanHuyen() == null ? "" : kv.getQuanHuyen()));
        Map<Integer, String> ketQua = new LinkedHashMap<>();
        for (KhuVuc kv : khuVucs) {
            ketQua.put(kv.getId(), kv.getQuanHuyen() + ", " + kv.getTinhThanh());
        }
        return ketQua;
    }

    // =========================================================================
    // KHUYẾN MÃI
    // =========================================================================

    /** Các chương trình khuyến mãi đang trong thời gian áp dụng. */
    @Cacheable(value = CacheNames.KHUYEN_MAI, key = "'dangChay'")
    public List<KhuyenMaiView> getKhuyenMaiDangChay() {
        LocalDate homNay = LocalDate.now();
        List<MaKhuyenMai> tatCaMa = maKhuyenMaiRepository.findAll();
        List<KhuyenMaiView> ketQua = new ArrayList<>();

        for (ChuongTrinhKhuyenMai ct : chuongTrinhKhuyenMaiRepository.findAll()) {
            if ("DaKetThuc".equalsIgnoreCase(ct.getTrangThai())
                    || ct.getNgayBatDau() == null || ct.getNgayKetThuc() == null
                    || homNay.isBefore(ct.getNgayBatDau()) || homNay.isAfter(ct.getNgayKetThuc())) {
                continue;
            }

            String mucGiam = "PhanTram".equalsIgnoreCase(ct.getLoaiGiam())
                    ? "Giảm " + ct.getGiaTriGiam().stripTrailingZeros().toPlainString() + "%"
                    : "Giảm " + dinhDangTien(ct.getGiaTriGiam());

            List<String> dieuKien = new ArrayList<>();
            if (ct.getSoTienGiamToiDa() != null && ct.getSoTienGiamToiDa().signum() > 0) {
                dieuKien.add("Tối đa " + dinhDangTien(ct.getSoTienGiamToiDa()));
            }
            if (ct.getDieuKienToiThieu() != null && ct.getDieuKienToiThieu().signum() > 0) {
                dieuKien.add("Đơn từ " + dinhDangTien(ct.getDieuKienToiThieu()));
            }

            String code = null;
            for (MaKhuyenMai ma : tatCaMa) {
                if (ma.getChuongTrinhKhuyenMai() != null
                        && ct.getId().equals(ma.getChuongTrinhKhuyenMai().getId())
                        && "HoatDong".equalsIgnoreCase(ma.getTrangThai())) {
                    code = ma.getCodeKhuyenMai();
                    break;
                }
            }

            ketQua.add(KhuyenMaiView.builder()
                    .tenChuongTrinh(ct.getTenChuongTrinh())
                    .moTa(ct.getMoTa())
                    .mucGiamText(mucGiam)
                    .dieuKienText(String.join(", ", dieuKien))
                    .hanText("đến hết " + ct.getNgayKetThuc().format(NGAY))
                    .code(code)
                    .build());
        }
        return ketQua;
    }

    // =========================================================================
    // ĐÁNH GIÁ CỦA KHÁCH HÀNG
    // =========================================================================

    /** Tối đa 6 đánh giá đã duyệt hiển thị, có nhận xét, mới nhất trước. */
    @Cacheable(value = CacheNames.DANH_GIA, key = "'noiBat'")
    public List<DanhGiaView> getDanhGiaNoiBat() {
        List<DanhGia> danhGias = new ArrayList<>(danhGiaRepository.findAll());
        danhGias.removeIf(dg -> !"HienThi".equalsIgnoreCase(dg.getTrangThai())
                || dg.getNhanXet() == null || dg.getNhanXet().isBlank()
                || dg.getNgayDanhGia() == null);
        danhGias.sort(Comparator.comparing(DanhGia::getNgayDanhGia).reversed());

        List<DanhGiaView> ketQua = new ArrayList<>();
        for (DanhGia dg : danhGias) {
            if (ketQua.size() >= 6) {
                break;
            }
            String ten = dg.getKhachHang() != null && dg.getKhachHang().getHoTen() != null
                    ? dg.getKhachHang().getHoTen().trim() : "Khách hàng";
            int chatLuong = dg.getDiemChatLuong() != null ? dg.getDiemChatLuong() : 5;
            int thaiDo = dg.getDiemThaiDo() != null ? dg.getDiemThaiDo() : chatLuong;
            ketQua.add(DanhGiaView.builder()
                    .tenKhachHang(ten)
                    .chuCaiDau(ten.isEmpty() ? "K" : ten.substring(0, 1).toUpperCase())
                    .soSao(Math.max(1, Math.min(5, Math.round((chatLuong + thaiDo) / 2.0f))))
                    .nhanXet(dg.getNhanXet().trim())
                    .ngayText(dg.getNgayDanhGia().format(NGAY))
                    .build());
        }
        return ketQua;
    }

    // =========================================================================
    // SỐ LIỆU CÔNG KHAI
    // =========================================================================

    /** Số liệu thật từ CSDL cho trang chủ và trang giới thiệu. */
    @Cacheable(value = CacheNames.THONG_KE, key = "'soLieuCongKhai'")
    public Map<String, Long> getSoLieuCongKhai() {
        Map<String, Long> soLieu = new LinkedHashMap<>();
        soLieu.put("dichVu", (long) dichVuRepository.findByTrangThai("HoatDong").size());
        soLieu.put("congTacVien", congTacVienRepository.findAll().stream()
                .filter(ctv -> "HoatDong".equalsIgnoreCase(ctv.getTrangThai())).count());
        soLieu.put("khachHang", khachHangRepository.count());
        soLieu.put("tinhThanh", khuVucRepository.findAll().stream()
                .filter(kv -> "HoatDong".equalsIgnoreCase(kv.getTrangThai()) && kv.getTinhThanh() != null)
                .map(kv -> kv.getTinhThanh().trim()).distinct().count());
        return soLieu;
    }

    // =========================================================================
    // ĐỊNH DẠNG
    // =========================================================================

    /** 192000 → "192.000đ"; 0 hoặc null → "Liên hệ". */
    public static String dinhDangTien(BigDecimal soTien) {
        if (soTien == null || soTien.signum() <= 0) {
            return "Liên hệ";
        }
        return String.format(Locale.US, "%,d", soTien.longValue()).replace(',', '.') + "đ";
    }

    /** 150 (phút) → "2 giờ 30 phút"; null → "". */
    public static String dinhDangThoiGian(Integer phut) {
        if (phut == null || phut <= 0) {
            return "";
        }
        int gio = phut / 60;
        int du = phut % 60;
        if (gio == 0) {
            return du + " phút";
        }
        return du == 0 ? gio + " giờ" : gio + " giờ " + du + " phút";
    }

    /** Rút gọn tên dịch vụ để hiện trong menu: bỏ phần trong ngoặc và thông số phía sau. */
    private static String tenNgan(String ten) {
        if (ten == null) {
            return "";
        }
        String ngan = ten.replaceAll("\\s*\\(.*?\\)", "")
                .replaceAll("\\s+(tối đa|dưới|trên|từ|nhỏ hơn|lớn hơn)\\s.*$", "")
                .replaceAll("\\s+\\d.*$", "")
                .trim();
        return ngan.isEmpty() ? ten.trim() : ngan;
    }
}
