package com.example.Service;

import com.example.DTO.request.*;
import com.example.DTO.response.ApiResponse;
import com.example.DTO.response.PriceCalculationResult;
import com.example.Model.*;
import com.example.Repository.*;
import com.example.Config.CacheNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class CustomerApiService {

    private final TaiKhoanRepository taiKhoanRepository;
    private final KhachHangRepository khachHangRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final KhuVucRepository khuVucRepository;
    // Danh sách quận, huyện TP.HCM dùng chung cho mọi form địa chỉ
    @org.springframework.beans.factory.annotation.Autowired
    private KhuVucService khuVucService;
    @org.springframework.beans.factory.annotation.Autowired
    private BangGiaService bangGiaService;
    @org.springframework.beans.factory.annotation.Autowired
    private DanhGiaService danhGiaService;
    @org.springframework.beans.factory.annotation.Autowired
    private DiaGioiService diaGioiService;
    private final OTPXacThucRepository otpXacThucRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final DichVuRepository dichVuRepository;
    private final LoaiDichVuRepository loaiDichVuRepository;
    private final BangGiaDichVuRepository bangGiaDichVuRepository;
    private final DichVuCTVRepository dichVuCTVRepository;
    private final KhuVucCTVRepository khuVucCTVRepository;
    private final ChungChiCTVRepository chungChiCTVRepository;
    private final HoSoCTVRepository hoSoCTVRepository;
    private final MaKhuyenMaiRepository maKhuyenMaiRepository;
    private final DonDatDichVuRepository donDatDichVuRepository;
    private final ChiTietDonDatRepository chiTietDonDatRepository;

    @Autowired
    private ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
    private final LichSuSuDungKhuyenMaiRepository lichSuSuDungKhuyenMaiRepository;
    private final LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final BienLaiRepository bienLaiRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final KhieuNaiRepository khieuNaiRepository;
    private final TaiLieuKhieuNaiRepository taiLieuKhieuNaiRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final PhanCongCTVRepository phanCongCTVRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final AuthService authService;
    private final SmsSender smsSender;

    /** Chuẩn hóa đường dẫn file MinIO (lưu object key vào CSDL). */
    @Autowired
    private MinioService minioService;

    /** Hạn chuyển khoản / hạn có người nhận: đồng hồ ở chi tiết đơn trên app. */
    @Autowired
    private HanNhanDonService hanNhanDonService;

    @Autowired
    private CustomerAddressService customerAddressService;

    @Autowired
    private ViTriCtvStore viTriCtvStore;

    @Autowired
    private CollaboratorApiService collaboratorApiService;

    @org.springframework.beans.factory.annotation.Value("${app.esms.otp-template:bTaskee: Ma xac thuc OTP cua ban la %s. Ma co hieu luc trong 5 phut.}")
    private String otpTemplate;

    @org.springframework.beans.factory.annotation.Value("${sepay.bank.code:MB}")
    private String sepayBankCode;

    @org.springframework.beans.factory.annotation.Value("${sepay.bank.account-no:0336676173}")
    private String sepayAccountNo;

    @org.springframework.beans.factory.annotation.Value("${sepay.bank.account-name:NGUYEN VUONG LE}")
    private String sepayAccountName;

    @org.springframework.beans.factory.annotation.Value("${sepay.transfer.prefix:BTK}")
    private String sepayTransferPrefix;

    public CustomerApiService(
            TaiKhoanRepository taiKhoanRepository,
            KhachHangRepository khachHangRepository,
            DiaChiKhachHangRepository diaChiKhachHangRepository,
            KhuVucRepository khuVucRepository,
            OTPXacThucRepository otpXacThucRepository,
            CongTacVienRepository congTacVienRepository,
            DichVuRepository dichVuRepository,
            LoaiDichVuRepository loaiDichVuRepository,
            BangGiaDichVuRepository bangGiaDichVuRepository,
            DichVuCTVRepository dichVuCTVRepository,
            KhuVucCTVRepository khuVucCTVRepository,
            ChungChiCTVRepository chungChiCTVRepository,
            HoSoCTVRepository hoSoCTVRepository,
            MaKhuyenMaiRepository maKhuyenMaiRepository,
            DonDatDichVuRepository donDatDichVuRepository,
            ChiTietDonDatRepository chiTietDonDatRepository,
            LichSuSuDungKhuyenMaiRepository lichSuSuDungKhuyenMaiRepository,
            LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository,
            HoaDonRepository hoaDonRepository,
            ChiTietHoaDonRepository chiTietHoaDonRepository,
            BienLaiRepository bienLaiRepository,
            DanhGiaRepository danhGiaRepository,
            KhieuNaiRepository khieuNaiRepository,
            TaiLieuKhieuNaiRepository taiLieuKhieuNaiRepository,
            ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
            ThongBaoRepository thongBaoRepository,
            PhanCongCTVRepository phanCongCTVRepository,
            LichLamViecRepository lichLamViecRepository,
            AuthService authService,
            SmsSender smsSender) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.khachHangRepository = khachHangRepository;
        this.diaChiKhachHangRepository = diaChiKhachHangRepository;
        this.khuVucRepository = khuVucRepository;
        this.otpXacThucRepository = otpXacThucRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.dichVuRepository = dichVuRepository;
        this.loaiDichVuRepository = loaiDichVuRepository;
        this.bangGiaDichVuRepository = bangGiaDichVuRepository;
        this.dichVuCTVRepository = dichVuCTVRepository;
        this.khuVucCTVRepository = khuVucCTVRepository;
        this.chungChiCTVRepository = chungChiCTVRepository;
        this.hoSoCTVRepository = hoSoCTVRepository;
        this.maKhuyenMaiRepository = maKhuyenMaiRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.chiTietDonDatRepository = chiTietDonDatRepository;
        this.lichSuSuDungKhuyenMaiRepository = lichSuSuDungKhuyenMaiRepository;
        this.lichSuTrangThaiDonRepository = lichSuTrangThaiDonRepository;
        this.hoaDonRepository = hoaDonRepository;
        this.chiTietHoaDonRepository = chiTietHoaDonRepository;
        this.bienLaiRepository = bienLaiRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.taiLieuKhieuNaiRepository = taiLieuKhieuNaiRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.thongBaoRepository = thongBaoRepository;
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.authService = authService;
        this.smsSender = smsSender;
    }

    // ==========================================
    // UC-KH01: ĐĂNG KÝ, XÁC THỰC OTP, ĐĂNG NHẬP
    // ==========================================
    private static final java.security.SecureRandom OTP_RANDOM = new java.security.SecureRandom();
    private static final long GIAN_CACH_GUI_LAI_OTP_GIAY = 60;

    /** Mã OTP 6 chữ số, sinh bằng SecureRandom để không đoán trước được. */
    private static String taoMaOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    /**
     * Tạo mã OTP (hết hạn sau 5 phút) và gửi tin nhắn SMS tới số điện thoại của tài khoản.
     * Gửi SMS thất bại thì ném IllegalArgumentException để transaction rollback (mã không được lưu)
     * và người dùng biết để thử lại, thay vì chờ một tin nhắn không bao giờ tới.
     * @return mã OTP vừa tạo
     */
    private String taoVaGuiOtp(TaiKhoan taiKhoan, String mucDich) {
        String soDienThoai = taiKhoan.getSoDienThoai();
        if (soDienThoai == null || soDienThoai.isBlank()) {
            throw new IllegalArgumentException("Tài khoản chưa có số điện thoại để nhận mã OTP.");
        }
        String otpCode = taoMaOtp();
        otpXacThucRepository.save(OTPXacThuc.builder()
                .taiKhoan(taiKhoan)
                .mucDich(mucDich)
                .maCode(otpCode)
                .thoiGianTao(LocalDateTime.now())
                .thoiGianHetHan(LocalDateTime.now().plusMinutes(5))
                .daSuDung(false)
                .build());

        // Nội dung phải khớp mẫu đã đăng ký với eSMS (app.esms.otp-template)
        String noiDungOtp = String.format(otpTemplate, otpCode);
        try {
            smsSender.send(soDienThoai, noiDungOtp);
        } catch (SmsSendException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
        return otpCode;
    }

    private TaiKhoan timTaiKhoan(String identifier) {
        String id = identifier.trim();
        return taiKhoanRepository.findBySoDienThoai(id)
                .or(() -> taiKhoanRepository.findByEmail(id))
                .or(() -> taiKhoanRepository.findByTenDangNhap(id))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với thông tin đã cung cấp."));
    }

    private static String mucDichOtp(String mucDich) {
        String md = (mucDich != null && !mucDich.isBlank()) ? mucDich.trim() : "DangKy";
        if (!java.util.Set.of("DangKy", "DatLaiMatKhau", "XacNhanGD").contains(md)) {
            throw new IllegalArgumentException("Mục đích OTP không hợp lệ.");
        }
        return md;
    }

    /** Lấy OTP mới nhất còn hiệu lực và so khớp; đúng thì đánh dấu đã dùng. */
    private void kiemTraVaDungOtp(TaiKhoan taiKhoan, String mucDich, String maCode) {
        OTPXacThuc otp = otpXacThucRepository.findTopByTaiKhoanAndMucDichAndDaSuDungFalseOrderByThoiGianTaoDesc(taiKhoan, mucDich)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã OTP hợp lệ hoặc mã đã được sử dụng."));
        if (otp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
        }
        if (!otp.getMaCode().equals(maCode.trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác.");
        }
        otp.setDaSuDung(true);
        otpXacThucRepository.save(otp);
    }

    /**
     * Một số điện thoại vừa làm khách hàng vừa làm cộng tác viên: bảng TaiKhoan có SĐT/tên đăng nhập
     * duy nhất, nên vai trò mới dùng chung tài khoản đã có (cùng SĐT, cùng mật khẩu), chỉ tạo thêm hồ sơ.
     * Người đăng ký phải nhập đúng mật khẩu của tài khoản đó để chứng minh là chủ số điện thoại.
     *
     * @return tài khoản để dùng chung, hoặc null nếu số điện thoại chưa có tài khoản
     */
    private TaiKhoan timTaiKhoanDeDungChung(String soDienThoai, String matKhau, String vaiTroMoi) {
        TaiKhoan tk = taiKhoanRepository.findBySoDienThoai(soDienThoai)
                .or(() -> taiKhoanRepository.findByTenDangNhap(soDienThoai))
                .orElse(null);
        if (tk == null) {
            return null;
        }
        if ("NhanVien".equalsIgnoreCase(tk.getLoaiTaiKhoan())) {
            throw new IllegalArgumentException("Số điện thoại này đã được dùng cho tài khoản nhân viên.");
        }
        if ("BiKhoa".equalsIgnoreCase(tk.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản của số điện thoại này đang bị khóa. Vui lòng liên hệ hỗ trợ.");
        }
        boolean coKhachHang = khachHangRepository.findByTaiKhoan_Id(tk.getId()).isPresent();
        boolean coCtv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).isPresent();
        boolean laKhachHang = "KhachHang".equals(vaiTroMoi);
        if (laKhachHang && coKhachHang) {
            throw new IllegalArgumentException("Số điện thoại này đã đăng ký tài khoản khách hàng.");
        }
        if (!laKhachHang && coCtv) {
            throw new IllegalArgumentException("Số điện thoại này đã đăng ký làm cộng tác viên.");
        }
        if (!authService.kiemTraMatKhau(tk, matKhau)) {
            String vaiTroCu = coCtv || "CongTacVien".equalsIgnoreCase(tk.getLoaiTaiKhoan()) ? "cộng tác viên" : "khách hàng";
            throw new IllegalArgumentException("Số điện thoại này đã có tài khoản " + vaiTroCu
                    + ". Vui lòng nhập đúng mật khẩu của tài khoản đó để dùng chung cho vai trò "
                    + (laKhachHang ? "khách hàng" : "cộng tác viên") + " (quên mật khẩu thì dùng chức năng Quên mật khẩu).");
        }
        return tk;
    }

    public Map<String, Object> registerCustomer(CustomerRegisterRequest req) {
        String soDienThoai = req.getSoDienThoai().trim();
        String email = (req.getEmail() != null && !req.getEmail().isBlank()) ? req.getEmail().trim() : (soDienThoai + "@customer.local");
        String tenDangNhap = (req.getTenDangNhap() != null && !req.getTenDangNhap().isBlank()) ? req.getTenDangNhap().trim() : soDienThoai;

        // Số điện thoại đã là cộng tác viên: dùng chung tài khoản đó
        TaiKhoan taiKhoanCoSan = timTaiKhoanDeDungChung(soDienThoai, req.getMatKhau(), "KhachHang");
        if (taiKhoanCoSan == null) {
            if (taiKhoanRepository.findByEmail(email).isPresent()) {
                throw new IllegalArgumentException("Email này đã được đăng ký trên hệ thống.");
            }
            if (taiKhoanRepository.findByTenDangNhap(tenDangNhap).isPresent()) {
                throw new IllegalArgumentException("Tên đăng nhập đã tồn tại, vui lòng chọn tên khác.");
            }
        }

        if (req.getNgaySinh() != null) {
            if (req.getNgaySinh().plusYears(18).isAfter(LocalDate.now())) {
                throw new IllegalArgumentException("Khách hàng phải từ đủ 18 tuổi trở lên.");
            }
            if (req.getNgaySinh().isBefore(LocalDate.now().minusYears(100))) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
            }
        }

        String suffix = com.example.Service.MaSinh.duoi(); // không trùng khi nhiều người đăng ký cùng lúc
        String maTaiKhoan = "TK-KH-" + suffix;
        String maKhachHang = "KH-" + suffix;

        // 1. Tạo tài khoản (ở trạng thái ChoDuyet cho đến khi xác thực OTP), hoặc dùng chung tài khoản CTV đã có
        TaiKhoan taiKhoan;
        if (taiKhoanCoSan != null) {
            taiKhoan = taiKhoanCoSan;
            if (req.getEmail() == null || req.getEmail().isBlank()) {
                email = taiKhoan.getEmail();
            }
        } else {
            // Chỉ tài khoản mới phải theo quy tắc; dùng chung thì mật khẩu cũ đã được kiểm tra khớp
            QuyTacMatKhau.batBuoc(req.getMatKhau().trim(), soDienThoai);
            //    Mật khẩu được mã hoá BCrypt ngay tại đây
            String encodedPassword = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                    .encode(req.getMatKhau().trim());
            taiKhoan = TaiKhoan.builder()
                    .maTaiKhoan(maTaiKhoan)
                    .tenDangNhap(tenDangNhap)
                    .matKhau(encodedPassword)
                    .email(email)
                    .soDienThoai(soDienThoai)
                    .loaiTaiKhoan("KhachHang")
                    .trangThai("ChoDuyet")
                    .build();
            taiKhoan = taiKhoanRepository.save(taiKhoan);
        }

        // 2. Tạo thông tin Khách hàng
        KhachHang khachHang = KhachHang.builder()
                .maKhachHang(maKhachHang)
                .taiKhoan(taiKhoan)
                .hoTen(req.getHoTen().trim())
                .ngaySinh(req.getNgaySinh())
                .gioiTinh(req.getGioiTinh() != null ? req.getGioiTinh() : "Khac")
                .soDienThoai(soDienThoai)
                .email(email)
                .ngayDangKy(LocalDate.now())
                .trangThai("HoatDong")
                .build();
        khachHang = khachHangRepository.save(khachHang);

        // 3. Địa chỉ nơi ở (nếu có) thành địa chỉ mặc định trong sổ địa chỉ
        if (req.getMaPhuong() != null) {
            com.example.DTO.request.AddressRequest dc = new com.example.DTO.request.AddressRequest();
            dc.setSoNhaTenDuong(req.getSoNhaTenDuong() != null ? req.getSoNhaTenDuong() : req.getDiaChiChiTiet());
            dc.setMaTinh(req.getMaTinh());
            dc.setMaQuan(req.getMaQuan());
            dc.setMaPhuong(req.getMaPhuong());
            dc.setKhuVucId(req.getKhuVucId());
            dc.setLaMacDinh(true);
            customerAddressService.them(khachHang.getId(), dc);
        } else if (req.getDiaChiChiTiet() != null && !req.getDiaChiChiTiet().isBlank()) {
            KhuVuc kv = null;
            if (req.getKhuVucId() != null) {
                kv = khuVucRepository.findById(req.getKhuVucId()).orElse(null);
            }
            DiaChiKhachHang diaChi = DiaChiKhachHang.builder()
                    .maDiaChi("DC-" + suffix)
                    .khachHang(khachHang)
                    .khuVuc(kv)
                    .diaChiChiTiet(req.getDiaChiChiTiet().trim())
                    .laMacDinh(true)
                    .trangThai("HoatDong")
                    .build();
            diaChiKhachHangRepository.save(diaChi);
        }

        // 4. Số điện thoại chưa xác thực: gửi OTP (6 chữ số, hết hạn sau 5 phút) qua SMS.
        //    Tài khoản dùng chung đã hoạt động (đã nhập đúng mật khẩu) thì không cần OTP.
        boolean canXacThucOtp = !"HoatDong".equalsIgnoreCase(taiKhoan.getTrangThai());
        String otpCode = canXacThucOtp ? taoVaGuiOtp(taiKhoan, "DangKy") : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("khachHangId", khachHang.getId());
        result.put("soDienThoai", soDienThoai);
        result.put("email", email);
        result.put("dungChungTaiKhoan", taiKhoanCoSan != null);
        result.put("canXacThucOtp", canXacThucOtp);
        if (canXacThucOtp && smsSender.laMoPhong()) {
            result.put("otpCode", otpCode); // Chỉ trả về khi SMS mô phỏng (app.sms.provider=mock)
        }
        result.put("thoiGianHetHanPhut", 5);
        result.put("message", canXacThucOtp
                ? "Đăng ký thành công! Mã OTP đã được gửi tới số " + soDienThoai + ". Vui lòng nhập mã OTP để kích hoạt tài khoản."
                : "Đã thêm vai trò khách hàng cho tài khoản " + soDienThoai + ". Bạn đăng nhập bằng mật khẩu hiện tại.");
        return result;
    }

    public Map<String, Object> verifyOtp(OtpVerifyRequest req) {
        String mucDich = mucDichOtp(req.getMucDich());
        TaiKhoan taiKhoan = timTaiKhoan(req.getIdentifier());

        kiemTraVaDungOtp(taiKhoan, mucDich, req.getMaCode());

        // Chỉ OTP đăng ký mới kích hoạt tài khoản đang chờ; không được mở khoá tài khoản bị khoá
        if ("DangKy".equals(mucDich) && "ChoDuyet".equals(taiKhoan.getTrangThai())) {
            taiKhoan.setTrangThai("HoatDong");
            taiKhoanRepository.save(taiKhoan);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verified", true);
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("tenDangNhap", taiKhoan.getTenDangNhap());
        result.put("trangThai", taiKhoan.getTrangThai());
        result.put("message", "Xác thực OTP thành công. Tài khoản đã được kích hoạt.");
        return result;
    }

    public Map<String, Object> sendOtp(OtpSendRequest req) {
        String mucDich = mucDichOtp(req.getMucDich());
        TaiKhoan taiKhoan = timTaiKhoan(req.getIdentifier());

        if ("DangKy".equals(mucDich) && !"ChoDuyet".equals(taiKhoan.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản đã được kích hoạt, không cần xác thực lại.");
        }
        if ("DatLaiMatKhau".equals(mucDich) && "BiKhoa".equals(taiKhoan.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản đang bị khoá. Vui lòng liên hệ bộ phận hỗ trợ.");
        }

        // Chống gửi liên tục: mỗi tài khoản chỉ được yêu cầu mã mới sau GIAN_CACH_GUI_LAI_OTP_GIAY giây
        otpXacThucRepository.findTopByTaiKhoanAndMucDichAndDaSuDungFalseOrderByThoiGianTaoDesc(taiKhoan, mucDich)
                .ifPresent(cu -> {
                    long daQua = java.time.Duration.between(cu.getThoiGianTao(), LocalDateTime.now()).getSeconds();
                    if (daQua < GIAN_CACH_GUI_LAI_OTP_GIAY) {
                        throw new IllegalArgumentException("Vui lòng chờ " + (GIAN_CACH_GUI_LAI_OTP_GIAY - daQua)
                                + " giây trước khi yêu cầu mã OTP mới.");
                    }
                });

        String otpCode = taoVaGuiOtp(taiKhoan, mucDich);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", taiKhoan.getId());
        if (smsSender.laMoPhong()) {
            result.put("otpCode", otpCode); // Chỉ trả về khi SMS mô phỏng (app.sms.provider=mock)
        }
        result.put("thoiGianHetHanPhut", 5);
        result.put("message", "Mã OTP mới đã được gửi tới số " + taiKhoan.getSoDienThoai() + ".");
        return result;
    }

    /** Quên mật khẩu: xác thực OTP (mục đích DatLaiMatKhau) đã gửi qua SMS rồi đặt mật khẩu mới. */
    public Map<String, Object> resetPassword(ResetPasswordRequest req) {
        TaiKhoan taiKhoan = timTaiKhoan(req.getIdentifier());
        QuyTacMatKhau.batBuoc(req.getMatKhauMoi().trim(), taiKhoan.getSoDienThoai());
        if ("BiKhoa".equals(taiKhoan.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản đang bị khoá. Vui lòng liên hệ bộ phận hỗ trợ.");
        }
        kiemTraVaDungOtp(taiKhoan, "DatLaiMatKhau", req.getMaCode());

        taiKhoan.setMatKhau(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode(req.getMatKhauMoi().trim()));
        taiKhoanRepository.save(taiKhoan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("tenDangNhap", taiKhoan.getTenDangNhap());
        return result;
    }

    public Map<String, Object> login(LoginRequest req) {
        String vaiTro = req.getVaiTro() != null ? req.getVaiTro().trim() : "";
        if (!vaiTro.isEmpty()) {
            return loginTheoVaiTro(req, vaiTro);
        }
        AuthService.AuthenticationResult auth = authService.authenticate(req.getTenDangNhap(), req.getMatKhau());
        if (!auth.success()) {
            throw new IllegalArgumentException(auth.message());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", auth.taiKhoanId());
        result.put("tenDangNhap", auth.tenDangNhap());
        result.put("loaiTaiKhoan", auth.loaiTaiKhoan());
        result.put("role", auth.role());
        result.put("fullName", auth.fullName());
        result.put("avatar", auth.avatar());

        if ("KhachHang".equalsIgnoreCase(auth.loaiTaiKhoan())) {
            khachHangRepository.findByTaiKhoan_Id(auth.taiKhoanId()).ifPresent(kh -> {
                result.put("khachHangId", kh.getId());
                result.put("maKhachHang", kh.getMaKhachHang());
                result.put("soDienThoai", kh.getSoDienThoai());
                result.put("email", kh.getEmail());
            });
        } else if ("CongTacVien".equalsIgnoreCase(auth.loaiTaiKhoan())) {
            congTacVienRepository.findByTaiKhoan_Id(auth.taiKhoanId()).ifPresent(ctv -> {
                result.put("congTacVienId", ctv.getId());
                result.put("maCongTacVien", ctv.getMaCongTacVien());
                result.put("diemDanhGia", ctv.getDiemDanhGia());
                result.put("capDo", ctv.getCapDo());
                result.put("trangThai", ctv.getTrangThai());
            });
        }
        return result;
    }

    /**
     * Đăng nhập app theo vai trò (tab Khách hàng / Cộng tác viên). Một tài khoản có thể có cả hai hồ sơ;
     * mỗi vai trò kiểm tra trạng thái hồ sơ riêng: khách hàng bị khóa không ảnh hưởng việc làm CTV và ngược lại.
     */
    private Map<String, Object> loginTheoVaiTro(LoginRequest req, String vaiTro) {
        boolean laKhachHang = "KhachHang".equalsIgnoreCase(vaiTro);
        if (!laKhachHang && !"CongTacVien".equalsIgnoreCase(vaiTro)) {
            throw new IllegalArgumentException("Vai trò đăng nhập không hợp lệ.");
        }
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(req.getTenDangNhap().trim()).orElse(null);
        if (tk == null || !authService.kiemTraMatKhau(tk, req.getMatKhau())) {
            throw new IllegalArgumentException("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }
        if ("NhanVien".equalsIgnoreCase(tk.getLoaiTaiKhoan())) {
            throw new IllegalArgumentException("Tài khoản nhân viên vui lòng đăng nhập trên website.");
        }
        if ("BiKhoa".equalsIgnoreCase(tk.getTrangThai())) {
            throw new IllegalArgumentException("Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", tk.getId());
        result.put("tenDangNhap", tk.getTenDangNhap());
        result.put("loaiTaiKhoan", laKhachHang ? "KhachHang" : "CongTacVien");
        result.put("soDienThoai", tk.getSoDienThoai());
        result.put("email", tk.getEmail());

        Optional<KhachHang> kh = khachHangRepository.findByTaiKhoan_Id(tk.getId());
        Optional<CongTacVien> ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId());
        result.put("laKhachHang", kh.isPresent());
        result.put("laCongTacVien", ctv.isPresent());

        if (laKhachHang) {
            KhachHang k = kh.orElseThrow(() -> new IllegalArgumentException(
                    "Số điện thoại này chưa đăng ký tài khoản khách hàng. Vui lòng đăng ký (dùng đúng mật khẩu hiện tại)."));
            if (!"HoatDong".equalsIgnoreCase(tk.getTrangThai())) {
                // App nhận câu này để chuyển sang bước nhập OTP
                throw new IllegalArgumentException("Tài khoản chưa được kích hoạt. Vui lòng xác thực mã OTP.");
            }
            if ("BiKhoa".equalsIgnoreCase(k.getTrangThai())) {
                throw new IllegalArgumentException("Tài khoản khách hàng đã bị khóa. Vui lòng liên hệ hỗ trợ.");
            }
            result.put("khachHangId", k.getId());
            result.put("maKhachHang", k.getMaKhachHang());
            result.put("fullName", k.getHoTen());
            if (k.getSoDienThoai() != null) result.put("soDienThoai", k.getSoDienThoai());
            if (k.getEmail() != null) result.put("email", k.getEmail());
            return result;
        }

        if (ctv.isEmpty()) {
            if (!"CongTacVien".equalsIgnoreCase(tk.getLoaiTaiKhoan())) {
                throw new IllegalArgumentException("Số điện thoại này chưa đăng ký làm cộng tác viên.");
            }
            // Tài khoản CTV do HCNS cấp nhưng chưa có hồ sơ: app sẽ gọi /profile/ensure
            if (!"HoatDong".equalsIgnoreCase(tk.getTrangThai())) {
                throw new IllegalArgumentException("Tài khoản cộng tác viên chưa được kích hoạt.");
            }
            result.put("fullName", tk.getTenDangNhap());
            return result;
        }
        CongTacVien c = ctv.get();
        switch (c.getTrangThai() != null ? c.getTrangThai() : "") {
            case "HoatDong" -> { }
            case "ChoDuyet" -> throw new IllegalArgumentException("Hồ sơ cộng tác viên đang chờ HCNS duyệt. Bạn sẽ nhận tin nhắn khi được duyệt.");
            case "TuChoi" -> throw new IllegalArgumentException("Hồ sơ cộng tác viên đã bị từ chối. Vui lòng liên hệ hỗ trợ.");
            default -> throw new IllegalArgumentException("Tài khoản cộng tác viên đang bị đình chỉ. Vui lòng liên hệ hỗ trợ.");
        }
        result.put("congTacVienId", c.getId());
        result.put("maCongTacVien", c.getMaCongTacVien());
        result.put("fullName", c.getHoTen());
        result.put("diemDanhGia", c.getDiemDanhGia());
        result.put("capDo", c.getCapDo());
        result.put("trangThai", c.getTrangThai());
        return result;
    }

    public Map<String, Object> socialLogin(SocialLoginRequest req) {
        String email = req.getEmail();
        if (email == null || email.isBlank()) {
            email = req.getProvider().toLowerCase() + "_" + req.getProviderId() + "@social.local";
        }
        final String finalEmail = email;
        String tenDangNhap = req.getProvider().toLowerCase() + "_" + req.getProviderId();

        TaiKhoan taiKhoan = taiKhoanRepository.findByTenDangNhap(tenDangNhap)
                .or(() -> taiKhoanRepository.findByEmail(finalEmail))
                .orElse(null);

        if (taiKhoan == null) {
            String suffix = com.example.Service.MaSinh.duoi(); // không trùng khi nhiều người đăng ký cùng lúc
            String soDienThoai = (req.getSoDienThoai() != null && !req.getSoDienThoai().isBlank()) ? req.getSoDienThoai().trim() : String.format("09%08d", java.util.concurrent.ThreadLocalRandom.current().nextInt(100000000));
            taiKhoan = TaiKhoan.builder()
                    .maTaiKhoan("TK-SOC-" + suffix)
                    .tenDangNhap(tenDangNhap)
                    .matKhau("SOCIAL_AUTH_" + UUID.randomUUID())
                    .email(email)
                    .soDienThoai(soDienThoai)
                    .loaiTaiKhoan("KhachHang")
                    .trangThai("HoatDong")
                    .build();
            taiKhoan = taiKhoanRepository.save(taiKhoan);

            KhachHang khachHang = KhachHang.builder()
                    .maKhachHang("KH-" + suffix)
                    .taiKhoan(taiKhoan)
                    .hoTen(req.getHoTen() != null ? req.getHoTen() : "Khách hàng " + req.getProvider())
                    .soDienThoai(soDienThoai)
                    .email(email)
                    .ngayDangKy(LocalDate.now())
                    .trangThai("HoatDong")
                    .build();
            khachHangRepository.save(khachHang);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("tenDangNhap", taiKhoan.getTenDangNhap());
        result.put("loaiTaiKhoan", taiKhoan.getLoaiTaiKhoan());
        result.put("role", "ROLE_KHACH_HANG");
        result.put("fullName", req.getHoTen());
        khachHangRepository.findByTaiKhoan(taiKhoan).ifPresent(kh -> result.put("khachHangId", kh.getId()));
        return result;
    }

    // ==========================================
    // UC-KH02: ĐĂNG KÝ LÀM CỘNG TÁC VIÊN
    // ==========================================
    public Map<String, Object> registerCollaborator(CollaboratorRegisterRequest req) {
        String soDienThoai = req.getSoDienThoai().trim();
        String email = (req.getEmail() != null && !req.getEmail().isBlank()) ? req.getEmail().trim() : (soDienThoai + "@ctv.local");
        String tenDangNhap = (req.getTenDangNhap() != null && !req.getTenDangNhap().isBlank()) ? req.getTenDangNhap().trim() : soDienThoai;

        // Số điện thoại đã là khách hàng: dùng chung tài khoản đó (cùng mật khẩu)
        TaiKhoan taiKhoanCoSan = timTaiKhoanDeDungChung(soDienThoai, req.getMatKhau(), "CongTacVien");
        if (taiKhoanCoSan == null && taiKhoanRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email này đã được đăng ký trên hệ thống.");
        }

        if (req.getNgaySinh() == null) {
            throw new IllegalArgumentException("Vui lòng nhập ngày sinh.");
        }
        if (req.getNgaySinh().plusYears(18).isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Cộng tác viên phải từ đủ 18 tuổi trở lên.");
        }
        if (req.getNgaySinh().isBefore(LocalDate.now().minusYears(100))) {
            throw new IllegalArgumentException("Ngày sinh không hợp lệ.");
        }

        // Địa chỉ nơi ở hiện tại (tỉnh, phường từ API địa giới) – khác với khu vực nhận việc
        String noiCuTru;
        List<KhuVuc> khuVucNoiO = List.of();
        if (req.getMaPhuong() != null) {
            DiaGioiService.DiaChiDaXacDinh x = diaGioiService.xacDinh(req.getSoNhaTenDuong(), req.getMaTinh(), req.getMaPhuong(), null);
            noiCuTru = x.diaChiDayDu();
            khuVucNoiO = diaGioiService.khuVucCuaPhuong(req.getMaPhuong());
        } else if (req.getNoiCuTru() != null && !req.getNoiCuTru().isBlank()) {
            noiCuTru = req.getNoiCuTru().trim();
        } else {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ nơi ở hiện tại (tỉnh, thành, phường, xã, số nhà).");
        }
        if (noiCuTru.length() > 200) {
            throw new IllegalArgumentException("Địa chỉ nơi ở quá dài (tối đa 200 ký tự).");
        }
        // Khu vực nhận việc không còn chọn tay: ban đầu là quận của nơi ở, sau đó theo GPS của app CTV
        if (khuVucNoiO.isEmpty() && (req.getPhuongXa() == null || req.getPhuongXa().isBlank())) {
            throw new IllegalArgumentException("Vui lòng chọn phường, xã nơi ở để xác định khu vực nhận việc ban đầu.");
        }

        String suffix = com.example.Service.MaSinh.duoi(); // không trùng khi nhiều người đăng ký cùng lúc
        String maTaiKhoan = "TK-CTV-" + suffix;
        String maCongTacVien = "CTV-" + suffix;

        // 1. Tạo tài khoản CTV ở trạng thái ChoDuyet với mật khẩu ứng viên tự đặt (web/app).
        //    Khi HCNS duyệt hồ sơ, tài khoản được kích hoạt (CapTaiKhoanCtvService).
        String rawPassword = req.getMatKhau() != null ? req.getMatKhau().trim() : "";
        //    Tài khoản dùng chung với vai trò khách hàng giữ nguyên trạng thái (khách vẫn đặt dịch vụ được);
        //    quyền làm CTV do trạng thái hồ sơ CongTacVien quyết định.
        TaiKhoan taiKhoan;
        if (taiKhoanCoSan != null) {
            taiKhoan = taiKhoanCoSan;
        } else {
            // Chỉ tài khoản mới phải theo quy tắc; dùng chung thì mật khẩu cũ đã được kiểm tra khớp
            QuyTacMatKhau.batBuoc(rawPassword, soDienThoai);
            taiKhoan = TaiKhoan.builder()
                    .maTaiKhoan(maTaiKhoan)
                    .tenDangNhap(tenDangNhap)
                    .matKhau(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                            .encode(rawPassword))
                    .email(email)
                    .soDienThoai(soDienThoai)
                    .loaiTaiKhoan("CongTacVien")
                    .trangThai("ChoDuyet")
                    .build();
            taiKhoan = taiKhoanRepository.save(taiKhoan);
        }

        // 2. Tạo hồ sơ Cộng tác viên
        CongTacVien ctv = CongTacVien.builder()
                .maCongTacVien(maCongTacVien)
                .taiKhoan(taiKhoan)
                .hoTen(req.getHoTen().trim())
                .ngaySinh(req.getNgaySinh())
                .gioiTinh(req.getGioiTinh() != null ? req.getGioiTinh() : "Khac")
                .noiCuTru(noiCuTru)
                .soDienThoai(soDienThoai)
                .diemDanhGia(BigDecimal.valueOf(5.0))
                .capDo("Moi")
                .trangThai("ChoDuyet")
                .ngayDangKy(LocalDate.now())
                .build();
        final CongTacVien savedCtv = congTacVienRepository.save(ctv);

        // 3. Liên kết Dịch vụ
        if (req.getDanhSachDichVuId() != null) {
            for (Integer dvId : req.getDanhSachDichVuId()) {
                dichVuRepository.findById(dvId).ifPresent(dv -> {
                    DichVuCTV dvCtv = DichVuCTV.builder()
                            .maDichVuCTV(maNgauNhien("DVCTV-"))
                            .congTacVien(savedCtv)
                            .dichVu(dv)
                            .trangThai("HoatDong")
                            .ngayBatDau(LocalDate.now())
                            .build();
                    dichVuCTVRepository.save(dvCtv);
                });
            }
        }

        // 4. Khu vực nhận việc ban đầu: quận chứa nơi ở (app cũ: tên quận, huyện gửi trong phuongXa).
        //    Khi CTV mở app, khu vực đổi theo quận đang đứng (GPS). danhSachKhuVucId gửi kèm bị bỏ qua.
        Set<Integer> khuVucDaGan = new HashSet<>();
        KhuVuc khuVucTheoDiaChi = !khuVucNoiO.isEmpty()
                ? khuVucNoiO.get(0) : timHoacTaoKhuVuc(req.getPhuongXa(), req.getTinhThanh());
        if (khuVucTheoDiaChi != null) {
            ganKhuVucChoCtv(savedCtv, khuVucTheoDiaChi, khuVucDaGan);
        }

        // 5. Lưu bằng cấp / chứng chỉ (URL MinIO)
        if (req.getDanhSachChungChi() != null) {
            for (CollaboratorRegisterRequest.ChungChiItem item : req.getDanhSachChungChi()) {
                ChungChiCTV cc = ChungChiCTV.builder()
                        .maChungChi(maNgauNhien("CC-"))
                        .congTacVien(savedCtv)
                        .loaiChungChi(item.getLoaiChungChi() != null ? item.getLoaiChungChi() : "ChungChi")
                        .tenChungChi(item.getTenChungChi())
                        .noiCap(item.getNoiCap())
                        .ngayCap(item.getNgayCap())
                        .duongDanFile(minioService.toStoredValue(item.getDuongDanFile())) // Lưu object key MinIO
                        .build();
                chungChiCTVRepository.save(cc);
            }
        }

        // 6. Lưu hồ sơ tài liệu (CCCD trước/sau, ảnh chân dung - URL MinIO)
        if (req.getDanhSachHoSo() != null) {
            for (CollaboratorRegisterRequest.HoSoItem item : req.getDanhSachHoSo()) {
                if (item.getDuongDanFile() == null || item.getDuongDanFile().isBlank()) {
                    continue; // cột DuongDanFile bắt buộc có giá trị
                }
                HoSoCTV hs = HoSoCTV.builder()
                        .maHoSo(maNgauNhien("HS-"))
                        .congTacVien(savedCtv)
                        .loaiTaiLieu(item.getLoaiTaiLieu() != null ? item.getLoaiTaiLieu() : "TaiLieuKhac")
                        .duongDanFile(minioService.toStoredValue(item.getDuongDanFile())) // Lưu object key MinIO
                        .ngayTai(LocalDateTime.now())
                        .build();
                hoSoCTVRepository.save(hs);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("congTacVienId", savedCtv.getId());
        result.put("maCongTacVien", savedCtv.getMaCongTacVien());
        result.put("hoTen", savedCtv.getHoTen());
        result.put("trangThai", savedCtv.getTrangThai());
        result.put("khuVuc", String.join(", ", khuVucRepository.findAllById(khuVucDaGan).stream()
                .map(KhuVuc::getTenKhuVuc).toList()));
        result.put("thoiGianXetDuyetDuKien", "1-3 ngày làm việc");
        result.put("message", "Hồ sơ ứng tuyển cộng tác viên đã được gửi thành công. Vui lòng chờ phòng HCNS xét duyệt, "
                + "kết quả sẽ được gửi qua tin nhắn tới số " + soDienThoai + ".");
        return result;
    }

    /** Gán một khu vực cho CTV, bỏ qua nếu đã gán (cặp congTacVienId - khuVucId là duy nhất). */
    private void ganKhuVucChoCtv(CongTacVien ctv, KhuVuc kv, Set<Integer> daGan) {
        if (!daGan.add(kv.getId())) {
            return;
        }
        khuVucCTVRepository.save(KhuVucCTV.builder()
                .maKhuVucCTV(maNgauNhien("KVCTV-"))
                .congTacVien(ctv)
                .khuVuc(kv)
                .trangThai("HoatDong")
                .build());
    }

    /**
     * Khu vực theo địa chỉ hiện tại của ứng viên: phải là một quận, huyện trong danh sách khu vực phục vụ
     * (TP.HCM). Không tạo khu vực mới để địa chỉ ứng viên luôn khớp với địa chỉ khách hàng.
     *
     * @return null nếu ứng viên (app cũ) không gửi quận, huyện
     */
    private KhuVuc timHoacTaoKhuVuc(String phuongXa, String tinhThanh) {
        if (phuongXa == null || phuongXa.isBlank()) {
            return null;
        }
        return khuVucService.timTheoTen(phuongXa)
                .orElseThrow(() -> new IllegalArgumentException("Quận, huyện \"" + phuongXa.trim()
                        + "\" không thuộc khu vực phục vụ. Hiện bTaskee nhận cộng tác viên tại "
                        + khuVucService.getTinhThanh() + "."));
    }

    /**
     * Sinh mã duy nhất cho các bản ghi tạo liên tiếp trong vòng lặp (hồ sơ, chứng chỉ, dịch vụ, khu vực).
     * Mã theo mili-giây bị trùng khi lưu nhiều bản ghi cùng lúc và vi phạm ràng buộc UNIQUE.
     */
    private static String maNgauNhien(String tienTo) {
        return tienTo + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    public Map<String, Object> getCollaboratorApplicationStatus(String soDienThoai) {
        CongTacVien ctv = congTacVienRepository.findAll().stream()
                .filter(c -> soDienThoai.trim().equals(c.getSoDienThoai()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ ứng tuyển với số điện thoại này."));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("congTacVienId", ctv.getId());
        result.put("maCongTacVien", ctv.getMaCongTacVien());
        result.put("hoTen", ctv.getHoTen());
        result.put("trangThai", ctv.getTrangThai());
        result.put("ngayDangKy", ctv.getNgayDangKy());
        return result;
    }

    // ==========================================
    // UC-KH03: XEM THÔNG TIN DỊCH VỤ, BẢNG GIÁ
    // ==========================================
    // Cache theo bộ lọc; không cache khi có từ khóa tìm kiếm tự do (#p3 = tuKhoa)
    @Cacheable(value = CacheNames.DICH_VU, condition = "#p3 == null or #p3.isEmpty()")
    public List<Map<String, Object>> getServices(Integer loaiDichVuId, Integer khuVucId, String loaiHinhDat, String tuKhoa, BigDecimal minPrice, BigDecimal maxPrice) {
        List<DichVu> allServices = dichVuRepository.findByTrangThai("HoatDong");

        List<Map<String, Object>> list = new ArrayList<>();
        for (DichVu dv : allServices) {
            if (loaiDichVuId != null && !loaiDichVuId.equals(dv.getLoaiDichVu().getId())) continue;
            if (loaiHinhDat != null && !loaiHinhDat.equalsIgnoreCase(dv.getLoaiHinhDat())) continue;

            // Giá hiện tại nằm ở DichVu.GiaHienTai; chỉ tra lịch sử bảng giá khi cột này chưa có giá
            BigDecimal donGia = dv.getGiaHienTai();
            if (donGia == null || donGia.signum() <= 0) {
                List<BangGiaDichVu> bangGias = bangGiaDichVuRepository.findByDichVu_IdAndTrangThai(dv.getId(), "DangApDung");
                donGia = bangGias.isEmpty() ? BigDecimal.ZERO : bangGias.get(0).getDonGia();
            }
            BigDecimal giaGoc = bangGiaService.giaGoc(dv.getId());
            Integer soBuoiGoi = "GoiThang".equalsIgnoreCase(dv.getLoaiHinhDat())
                    ? LichGoiThang.soBuoi(dv.getSoBuoi(), dv.getTenDichVu())
                    : null;

            if (minPrice != null && donGia.compareTo(minPrice) < 0) continue;
            if (maxPrice != null && donGia.compareTo(maxPrice) > 0) continue;

            if (tuKhoa != null && !tuKhoa.isBlank()) {
                if (!khopTuKhoaMoRong(dv, donGia, giaGoc, soBuoiGoi, tuKhoa)) continue;
            }

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", dv.getId());
            map.put("maDichVu", dv.getMaDichVu());
            map.put("tenDichVu", dv.getTenDichVu());
            map.put("loaiDichVuId", dv.getLoaiDichVu().getId());
            map.put("tenLoaiDichVu", dv.getLoaiDichVu().getTenLoaiDichVu());
            map.put("loaiHinhDat", dv.getLoaiHinhDat());
            map.put("donViTinh", dv.getDonViTinh());
            map.put("thoiGianThucHienPhut", dv.getThoiGianThucHien());
            map.put("moTaChiTiet", dv.getMoTaChiTiet());
            map.put("donGiaThamKhao", donGia);
            map.put("giaGoc", bangGiaService.giaGoc(dv.getId())); // giá trước đợt tăng/giảm tạm, null nếu không có
            map.put("hinhAnhUrl", minioService.getViewUrl(dv.getHinhAnh())); // ảnh HCNS đặt, null = dùng ảnh danh mục
            if ("GoiThang".equalsIgnoreCase(dv.getLoaiHinhDat())) {
                map.put("soBuoiGoi", LichGoiThang.soBuoi(dv.getSoBuoi(), dv.getTenDichVu()));
            }
            list.add(map);
        }
        return list;
    }

    /**
     * Tìm kiếm mở rộng: so khớp từ khóa (hỗ trợ cả đa từ khóa, có dấu/không dấu) với:
     * - Tên dịch vụ, Mã dịch vụ, Tên nhóm dịch vụ
     * - Nội dung mô tả chi tiết (diện tích, số phòng, công việc cụ thể...)
     * - Giá tiền (dạng số nguyên "216000", có dấu chấm/phẩy "216.000", viết tắt "216k", "1.4tr"...)
     * - Thời lượng ("2 gio", "2h", "120 phut"), số buổi ("8 buoi"), đơn vị tính, hình thức đặt.
     */
    private static boolean khopTuKhoaMoRong(DichVu dv, BigDecimal donGia, BigDecimal giaGoc, Integer soBuoiGoi, String tuKhoa) {
        String kwNorm = boDau(tuKhoa.trim());
        if (kwNorm.isEmpty()) return true;

        StringBuilder sb = new StringBuilder();
        sb.append(boDau(dv.getTenDichVu())).append(' ');
        sb.append(boDau(dv.getMaDichVu())).append(' ');
        sb.append(boDau(dv.getMoTaChiTiet())).append(' ');
        sb.append(boDau(dv.getDonViTinh())).append(' ');
        if (dv.getLoaiDichVu() != null) {
            sb.append(boDau(dv.getLoaiDichVu().getTenLoaiDichVu())).append(' ');
            sb.append(boDau(dv.getLoaiDichVu().getMoTa())).append(' ');
        }
        if ("GoiThang".equalsIgnoreCase(dv.getLoaiHinhDat())) {
            sb.append("goi thang dinh ky theo thang ");
        } else {
            sb.append("theo lan theo gio dat le ");
        }
        if (dv.getThoiGianThucHien() != null && dv.getThoiGianThucHien() > 0) {
            int phut = dv.getThoiGianThucHien();
            sb.append(phut).append(" phut ").append(phut).append("p ");
            if (phut % 60 == 0) {
                int gio = phut / 60;
                sb.append(gio).append(" gio ").append(gio).append("h ").append(gio).append("g ");
            } else {
                double gioLe = phut / 60.0;
                String gioStr = String.format(java.util.Locale.US, "%.1f", gioLe);
                sb.append(gioStr).append(" gio ").append(gioStr.replace('.', ',')).append(" gio ");
            }
        }
        if (soBuoiGoi != null && soBuoiGoi > 0) {
            sb.append(soBuoiGoi).append(" buoi ");
        }
        appendPriceTokens(sb, donGia);
        appendPriceTokens(sb, giaGoc);

        String fullText = sb.toString();
        if (fullText.contains(kwNorm)) return true;

        // Nếu người dùng nhập một mức giá (VD: "200k", "300.000đ", "1.5tr"), kiểm tra khớp giá trực tiếp
        BigDecimal parsedKwPrice = parseSearchPrice(kwNorm);
        if (parsedKwPrice != null && donGia != null && donGia.signum() > 0) {
            if (donGia.compareTo(parsedKwPrice) == 0) return true;
            if (giaGoc != null && giaGoc.compareTo(parsedKwPrice) == 0) return true;
        }

        // So khớp đa từ khóa (multi-token): mọi từ trong câu tìm kiếm đều xuất hiện trong dữ liệu dịch vụ
        String[] tokens = kwNorm.split("\\s+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (fullText.contains(token)) continue;
            // Hỗ trợ token là mức giá viết tắt (VD: "vệ sinh 216k")
            BigDecimal tokenPrice = parseSearchPrice(token);
            if (tokenPrice != null && donGia != null &&
                    (donGia.compareTo(tokenPrice) == 0 || (giaGoc != null && giaGoc.compareTo(tokenPrice) == 0))) {
                continue;
            }
            return false;
        }
        return true;
    }

    /** Đưa các biến thể viết giá (216000, 216.000, 216,000, 216k, 1.4tr...) vào chuỗi tìm kiếm */
    private static void appendPriceTokens(StringBuilder sb, BigDecimal price) {
        if (price == null || price.signum() <= 0) return;
        long v = price.longValue();
        sb.append(v).append(' ').append(v).append("d ").append(v).append("vnd ");
        String dotFormatted = String.format(java.util.Locale.US, "%,d", v).replace(',', '.');
        String commaFormatted = String.format(java.util.Locale.US, "%,d", v);
        sb.append(dotFormatted).append(' ').append(dotFormatted).append("d ");
        sb.append(commaFormatted).append(' ');
        if (v >= 1_000) {
            long k = v / 1_000;
            sb.append(k).append("k ").append(k).append(" nghin ").append(k).append(" ngan ");
        }
        if (v >= 1_000_000) {
            double tr = v / 1_000_000.0;
            if (v % 1_000_000 == 0) {
                long trInt = v / 1_000_000;
                sb.append(trInt).append("tr ").append(trInt).append(" trieu ");
            } else {
                String trStr = String.format(java.util.Locale.US, "%.2f", tr).replaceAll("0+$", "").replaceAll("\\.$", "");
                sb.append(trStr).append("tr ").append(trStr.replace('.', ',')).append("tr ");
                sb.append(trStr).append(" trieu ");
            }
        }
    }

    /** Chuyển chuỗi giá người dùng gõ (VD "216k", "216.000đ", "1.4tr", "216000") thành BigDecimal */
    private static BigDecimal parseSearchPrice(String token) {
        if (token == null) return null;
        String clean = token.trim().toLowerCase()
                .replace("vnd", "").replace("đ", "").replace("d", "").trim();
        if (clean.isEmpty()) return null;
        try {
            if (clean.endsWith("k")) {
                String numPart = clean.substring(0, clean.length() - 1).replace(',', '.').trim();
                return new BigDecimal(numPart).multiply(BigDecimal.valueOf(1_000)).setScale(0, java.math.RoundingMode.HALF_UP);
            }
            if (clean.endsWith("tr") || clean.endsWith("trieu")) {
                String numPart = clean.replace("trieu", "").replace("tr", "").replace(',', '.').trim();
                return new BigDecimal(numPart).multiply(BigDecimal.valueOf(1_000_000)).setScale(0, java.math.RoundingMode.HALF_UP);
            }
            // Dạng 216.000 hoặc 216000
            String digitsOnly = clean.replace(".", "").replace(",", "");
            if (digitsOnly.matches("\\d{4,9}")) {
                return new BigDecimal(digitsOnly);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /** Chữ thường, bỏ dấu tiếng Việt: "Dọn dẹp" → "don dep", để tìm "don dep" cũng ra. */
    static String boDau(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s.toLowerCase(), java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").replace('đ', 'd');
    }

    public Map<String, Object> getServiceDetail(Integer id) {
        DichVu dv = dichVuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại với ID: " + id));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", dv.getId());
        result.put("maDichVu", dv.getMaDichVu());
        result.put("tenDichVu", dv.getTenDichVu());
        result.put("loaiDichVuId", dv.getLoaiDichVu().getId());
        result.put("tenLoaiDichVu", dv.getLoaiDichVu().getTenLoaiDichVu());
        result.put("loaiHinhDat", dv.getLoaiHinhDat());
        result.put("donViTinh", dv.getDonViTinh());
        result.put("thoiGianThucHienPhut", dv.getThoiGianThucHien());
        result.put("moTaChiTiet", dv.getMoTaChiTiet());
        result.put("soBuoi", dv.getSoBuoi());
        if ("GoiThang".equalsIgnoreCase(dv.getLoaiHinhDat())) {
            result.put("soBuoiGoi", LichGoiThang.soBuoi(dv.getSoBuoi(), dv.getTenDichVu()));
        }
        result.put("soNguoiThucHien", dv.getSoNguoiThucHien());
        result.put("giaHienTai", dv.getGiaHienTai());
        result.put("giaGoc", bangGiaService.giaGoc(dv.getId()));

        // Lịch sử bảng giá
        // Chỉ bảng giá đang áp dụng lúc này (không gồm giá gốc đang bị đợt tạm thay, giá sắp áp dụng)
        List<BangGiaDichVu> bangGias = bangGiaService.giaApDung(dv.getId(), LocalDateTime.now()).map(List::of).orElse(List.of());
        List<Map<String, Object>> bangGiaMaps = new ArrayList<>();
        for (BangGiaDichVu bg : bangGias) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", bg.getId());
            m.put("maBangGia", bg.getMaBangGia());
            m.put("loaiHinhDat", bg.getLoaiHinhDat());
            m.put("donViTinh", bg.getDonViTinh());
            m.put("donGia", bg.getDonGia());
            m.put("giaCu", bg.getGiaCu());
            m.put("giaGoc", bangGiaService.giaGoc(dv.getId())); // giá trước đợt tăng/giảm tạm (gạch ngang), null nếu không có
            m.put("ngayKetThuc", bg.getNgayKetThuc());
            m.put("khuVucId", bg.getKhuVuc() != null ? bg.getKhuVuc().getId() : null);
            m.put("khuVuc", bg.getKhuVuc() != null ? bg.getKhuVuc().getTenKhuVuc() : null);
            bangGiaMaps.add(m);
        }
        result.put("bangGias", bangGiaMaps);

        // Đánh giá công khai (đã duyệt) kèm phản hồi mới nhất của bTaskee
        List<Map<String, Object>> danhGias = getReviewsByService(dv.getId());
        result.put("soLuongDanhGia", danhGias.size());
        double avgScore = danhGias.stream()
                .mapToDouble(d -> ((Number) d.get("diem")).doubleValue())
                .average().orElse(5.0);
        result.put("diemDanhGiaTrungBinh", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
        result.put("danhGias", danhGias.stream().limit(10).toList());
        // Lượt đặt theo mã dịch vụ (cùng cách tính với đánh giá), chưa có mã thì theo id
        result.put("soLuotDat", dv.getMaDichVu() != null && !dv.getMaDichVu().isBlank()
                ? chiTietDonDatRepository.countByDichVu_MaDichVu(dv.getMaDichVu())
                : chiTietDonDatRepository.countByDichVu_Id(dv.getId()));

        // Danh sách CTV thực hiện dịch vụ này
        List<DichVuCTV> dvCtvs = dichVuCTVRepository.findByDichVu_Id(dv.getId());
        List<Map<String, Object>> ctvList = new ArrayList<>();
        for (DichVuCTV dvCtv : dvCtvs) {
            CongTacVien ctv = dvCtv.getCongTacVien();
            if ("HoatDong".equalsIgnoreCase(ctv.getTrangThai())) {
                Map<String, Object> ctvMap = new LinkedHashMap<>();
                ctvMap.put("id", ctv.getId());
                ctvMap.put("hoTen", ctv.getHoTen());
                ctvMap.put("diemDanhGia", ctv.getDiemDanhGia());
                ctvMap.put("capDo", ctv.getCapDo());
                ctvList.add(ctvMap);
            }
        }
        result.put("congTacViens", ctvList);
        return result;
    }

    @Cacheable(value = CacheNames.LOAI_DICH_VU, key = "'tatCa'")
    public List<LoaiDichVu> getServiceTypes() {
        return loaiDichVuRepository.findAll();
    }

    /** Các quận, huyện TP.HCM đang phục vụ (cùng danh sách với web). */
    @Cacheable(value = CacheNames.KHU_VUC, key = "'tatCa'")
    public List<KhuVuc> getAreas() {
        return khuVucService.danhSach();
    }

    // ==========================================
    // UC-KH04: TÍNH GIÁ, KHUYẾN MẠI, ĐẶT DỊCH VỤ
    // ==========================================
    public PriceCalculationResult calculatePrice(CalculatePriceRequest req) {
        // Đơn nhiều dịch vụ (giỏ hàng): mỗi dòng một dịch vụ theo lần, giá gốc = tổng các dòng
        List<ChiTietDonDatRequest> dongs = req.getChiTiet() != null ? req.getChiTiet() : List.of();
        boolean nhieu = dongs.size() >= 2;
        Integer dichVuDau = nhieu ? dongs.get(0).getDichVuId() : req.getDichVuId();
        DichVu dv = dichVuRepository.findById(dichVuDau)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ ID: " + dichVuDau));

        String loaiHinh = nhieu ? "TheoLan" : (req.getLoaiHinhDat() != null && !req.getLoaiHinhDat().isBlank())
                ? req.getLoaiHinhDat() : dv.getLoaiHinhDat();

        List<DatNhieuDichVu.DongGia> chiTietGia = new ArrayList<>();
        if (nhieu) {
            for (ChiTietDonDatRequest dong : dongs) {
                DichVu d = dichVuRepository.findById(dong.getDichVuId())
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ ID: " + dong.getDichVuId()));
                chiTietGia.add(new DatNhieuDichVu.DongGia(d.getId(), d.getTenDichVu(), giaGocMotDichVu(d, req, false)));
            }
        } else {
            chiTietGia.add(new DatNhieuDichVu.DongGia(dv.getId(), dv.getTenDichVu(), giaGocMotDichVu(dv, req, true)));
        }
        BigDecimal chiPhiGoc = DatNhieuDichVu.tongGoc(chiTietGia);
        return apKhuyenMai(req, dv, loaiHinh, chiPhiGoc, chiTietGia);
    }

    /**
     * Giá gốc một dịch vụ: bảng giá hiệu lực lúc bắt đầu làm (có ngày giờ), rồi bangGiaId client gửi
     * (chỉ khi [dungBangGiaId]), rồi GiaHienTai trên DichVu (giá hôm nay, BangGiaService đồng bộ mỗi ngày).
     */
    private BigDecimal giaGocMotDichVu(DichVu dv, CalculatePriceRequest req, boolean dungBangGiaId) {
        BigDecimal chiPhiGoc;
        BangGiaDichVu theoNgay = req.getNgayThucHien() != null
                ? (req.getGioBatDau() != null
                        ? bangGiaService.giaApDung(dv.getId(), LocalDateTime.of(req.getNgayThucHien(), req.getGioBatDau()))
                        : bangGiaService.giaApDung(dv.getId(), req.getNgayThucHien())).orElse(null)
                : null;
        if (theoNgay != null && theoNgay.getDonGia() != null) {
            chiPhiGoc = theoNgay.getDonGia();
        } else if (dungBangGiaId && req.getBangGiaId() != null) {
            BangGiaDichVu bg = bangGiaDichVuRepository.findById(req.getBangGiaId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bảng giá ID: " + req.getBangGiaId()));
            chiPhiGoc = bg.getDonGia();
        } else if (dv.getGiaHienTai() != null && dv.getGiaHienTai().compareTo(BigDecimal.ZERO) > 0) {
            chiPhiGoc = dv.getGiaHienTai();
        } else {
            // Fallback: lấy giá từ bảng giá đang áp dụng
            List<BangGiaDichVu> bangGias = bangGiaDichVuRepository.findByDichVu_IdAndTrangThai(dv.getId(), "DangApDung");
            chiPhiGoc = bangGias.isEmpty() ? BigDecimal.valueOf(250000) : bangGias.get(0).getDonGia();
        }
        return chiPhiGoc;
    }

    /** Mã khuyến mãi xét điều kiện tối thiểu và giảm trên tổng [chiPhiGoc] của đơn. */
    private PriceCalculationResult apKhuyenMai(CalculatePriceRequest req, DichVu dv, String loaiHinh,
                                               BigDecimal chiPhiGoc, List<DatNhieuDichVu.DongGia> chiTietGia) {
        BigDecimal soTienGiam = BigDecimal.ZERO;
        String thongDiepKM = null;

        if (req.getCodeKhuyenMai() != null && !req.getCodeKhuyenMai().isBlank()) {
            MaKhuyenMai mkm = maKhuyenMaiRepository.findByCodeKhuyenMaiIgnoreCase(req.getCodeKhuyenMai().trim())
                    .orElse(null);

            String lyDo = mkm == null ? "Mã ưu đãi không tồn tại." : lyDoMaKhongDungDuoc(mkm, LocalDate.now());
            if (lyDo != null) {
                thongDiepKM = lyDo;
            } else {
                ChuongTrinhKhuyenMai ctkm = mkm.getChuongTrinhKhuyenMai();
                if (chiPhiGoc.compareTo(ctkm.getDieuKienToiThieu()) >= 0) {
                    soTienGiam = DatNhieuDichVu.soTienGiam(ctkm, chiPhiGoc);
                    thongDiepKM = "Áp dụng thành công mã " + mkm.getCodeKhuyenMai() + ": " + ctkm.getTenChuongTrinh();
                } else {
                    thongDiepKM = "Đơn hàng chưa đạt giá trị tối thiểu " + dinhDangTien(ctkm.getDieuKienToiThieu()) + " để dùng mã này.";
                }
            }
        }
        // Không giảm quá giá trị đơn
        if (soTienGiam.compareTo(chiPhiGoc) > 0) soTienGiam = chiPhiGoc;

        BigDecimal thanhTien = chiPhiGoc.subtract(soTienGiam);
        if (thanhTien.compareTo(BigDecimal.ZERO) < 0) thanhTien = BigDecimal.ZERO;

        return PriceCalculationResult.builder()
                .dichVuId(dv.getId())
                .tenDichVu(dv.getTenDichVu())
                .loaiHinhDat(loaiHinh)
                .chiPhiGoc(chiPhiGoc)
                .soTienGiam(soTienGiam)
                .thanhTien(thanhTien)
                .maKhuyenMai(req.getCodeKhuyenMai())
                .thongDiepKMDuocApDung(thongDiepKM)
                .chiTietGia(chiTietGia)
                .build();
    }

    /** null nếu mã dùng được hôm nay; ngược lại là lý do hiển thị cho khách. */
    private String lyDoMaKhongDungDuoc(MaKhuyenMai mkm, LocalDate homNay) {
        if (!"HoatDong".equalsIgnoreCase(mkm.getTrangThai())) return "Mã ưu đãi đã hết hiệu lực.";
        if (mkm.getSoLuotToiDa() != null && mkm.getSoLuotDaDung() != null
                && mkm.getSoLuotDaDung() >= mkm.getSoLuotToiDa()) return "Mã ưu đãi đã hết lượt sử dụng.";
        ChuongTrinhKhuyenMai ct = mkm.getChuongTrinhKhuyenMai();
        if (ct == null || !"DangHoatDong".equalsIgnoreCase(ct.getTrangThai())) return "Chương trình khuyến mãi không còn diễn ra.";
        if (ct.getNgayBatDau() != null && ct.getNgayBatDau().isAfter(homNay)) return "Chương trình khuyến mãi chưa bắt đầu.";
        if (ct.getNgayKetThuc() != null && ct.getNgayKetThuc().isBefore(homNay)) return "Chương trình khuyến mãi đã kết thúc.";
        return null;
    }

    private static String dinhDangTien(BigDecimal so) {
        if (so == null) return "0đ";
        return String.format("%,d", so.setScale(0, RoundingMode.HALF_UP).longValue()).replace(',', '.') + "đ";
    }

    /**
     * Bảng "Áp dụng khuyến mãi" ở trang đặt dịch vụ: mọi mã đang còn hiệu lực, tính sẵn số tiền giảm
     * cho dịch vụ (và ngày giờ) đang chọn. Mã dùng được xếp trước, giảm nhiều nhất lên đầu;
     * mã chưa đủ giá trị đơn tối thiểu vẫn trả về kèm lý do để khách biết.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUsablePromotions(CalculatePriceRequest req) {
        boolean coChiTiet = req.getChiTiet() != null && !req.getChiTiet().isEmpty();
        if (req.getDichVuId() == null && !coChiTiet) throw new IllegalArgumentException("Vui lòng chọn dịch vụ trước.");
        CalculatePriceRequest khongMa = new CalculatePriceRequest();
        khongMa.setDichVuId(req.getDichVuId());
        khongMa.setChiTiet(req.getChiTiet());
        khongMa.setLoaiHinhDat(req.getLoaiHinhDat());
        khongMa.setBangGiaId(req.getBangGiaId());
        khongMa.setNgayThucHien(req.getNgayThucHien());
        khongMa.setGioBatDau(req.getGioBatDau());
        BigDecimal chiPhiGoc = calculatePrice(khongMa).getChiPhiGoc();

        LocalDate homNay = LocalDate.now();
        List<Map<String, Object>> ketQua = new ArrayList<>();
        for (MaKhuyenMai mkm : maKhuyenMaiRepository.findAll()) {
            if (lyDoMaKhongDungDuoc(mkm, homNay) != null) continue;
            ChuongTrinhKhuyenMai ct = mkm.getChuongTrinhKhuyenMai();
            BigDecimal toiThieu = ct.getDieuKienToiThieu() != null ? ct.getDieuKienToiThieu() : BigDecimal.ZERO;
            boolean duDieuKien = chiPhiGoc.compareTo(toiThieu) >= 0;
            BigDecimal giam = duDieuKien ? DatNhieuDichVu.soTienGiam(ct, chiPhiGoc) : BigDecimal.ZERO;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codeKhuyenMai", mkm.getCodeKhuyenMai());
            m.put("tenChuongTrinh", ct.getTenChuongTrinh());
            m.put("moTa", KhuyenMaiHienThi.moTaChoKhach(ct.getMoTa()));
            m.put("loaiGiam", ct.getLoaiGiam());
            m.put("giaTriGiam", ct.getGiaTriGiam());
            m.put("soTienGiamToiDa", ct.getSoTienGiamToiDa());
            m.put("dieuKienToiThieu", toiThieu);
            m.put("ngayKetThuc", ct.getNgayKetThuc());
            m.put("soLuotConLai", mkm.getSoLuotToiDa() != null && mkm.getSoLuotDaDung() != null
                    ? mkm.getSoLuotToiDa() - mkm.getSoLuotDaDung() : null);
            m.put("dungDuoc", duDieuKien && giam.signum() > 0);
            m.put("soTienGiam", giam);
            m.put("lyDo", duDieuKien ? null
                    : "Cần đơn từ " + dinhDangTien(toiThieu) + " (còn thiếu " + dinhDangTien(toiThieu.subtract(chiPhiGoc)) + ")");
            ketQua.add(m);
        }
        ketQua.sort(Comparator.comparing((Map<String, Object> m) -> !(Boolean) m.get("dungDuoc"))
                .thenComparing(m -> (BigDecimal) m.get("soTienGiam"), Comparator.reverseOrder())
                .thenComparing(m -> (BigDecimal) m.get("dieuKienToiThieu")));
        return ketQua;
    }

    /** Chương trình khuyến mãi đang chạy kèm mã còn dùng được: banner "Ưu đãi" trên app. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActivePromotions() {
        LocalDate homNay = LocalDate.now();
        Map<Integer, List<MaKhuyenMai>> maTheoChuongTrinh = new HashMap<>();
        for (MaKhuyenMai mkm : maKhuyenMaiRepository.findAll()) {
            if (mkm.getChuongTrinhKhuyenMai() == null || !"HoatDong".equalsIgnoreCase(mkm.getTrangThai())) continue;
            if (mkm.getSoLuotToiDa() != null && mkm.getSoLuotDaDung() != null
                    && mkm.getSoLuotDaDung() >= mkm.getSoLuotToiDa()) continue;
            maTheoChuongTrinh.computeIfAbsent(mkm.getChuongTrinhKhuyenMai().getId(), k -> new ArrayList<>()).add(mkm);
        }
        List<Map<String, Object>> ketQua = new ArrayList<>();
        for (ChuongTrinhKhuyenMai ct : chuongTrinhKhuyenMaiRepository.findAll()) {
            if (!"DangHoatDong".equalsIgnoreCase(ct.getTrangThai())) continue;
            if (ct.getNgayBatDau() != null && ct.getNgayBatDau().isAfter(homNay)) continue;
            if (ct.getNgayKetThuc() != null && ct.getNgayKetThuc().isBefore(homNay)) continue;
            List<MaKhuyenMai> ma = maTheoChuongTrinh.getOrDefault(ct.getId(), List.of());
            if (ma.isEmpty()) continue; // không còn mã nào để khách nhập
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ct.getId());
            m.put("tenChuongTrinh", ct.getTenChuongTrinh());
            m.put("moTa", KhuyenMaiHienThi.moTaChoKhach(ct.getMoTa()));
            m.put("loaiGiam", ct.getLoaiGiam());
            m.put("giaTriGiam", ct.getGiaTriGiam());
            m.put("soTienGiamToiDa", ct.getSoTienGiamToiDa());
            m.put("dieuKienToiThieu", ct.getDieuKienToiThieu());
            m.put("ngayKetThuc", ct.getNgayKetThuc());
            m.put("codeKhuyenMai", ma.get(0).getCodeKhuyenMai());
            ketQua.add(m);
        }
        ketQua.sort(Comparator.comparing(m -> (LocalDate) m.get("ngayKetThuc"),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return ketQua;
    }

    public Map<String, Object> validatePromotion(ValidatePromotionRequest req) {
        MaKhuyenMai mkm = maKhuyenMaiRepository.findByCodeKhuyenMaiIgnoreCase(req.getCodeKhuyenMai().trim())
                .orElseThrow(() -> new IllegalArgumentException("Mã khuyến mãi không tồn tại hoặc không hợp lệ."));

        if (!"HoatDong".equalsIgnoreCase(mkm.getTrangThai()) || mkm.getSoLuotDaDung() >= mkm.getSoLuotToiDa()) {
            throw new IllegalArgumentException("Mã khuyến mãi đã hết lượt sử dụng hoặc không còn hiệu lực.");
        }

        ChuongTrinhKhuyenMai ct = mkm.getChuongTrinhKhuyenMai();
        if (ct == null || !"DangHoatDong".equalsIgnoreCase(ct.getTrangThai())) {
            throw new IllegalArgumentException("Chương trình khuyến mãi này đã kết thúc.");
        }

        if (req.getTongTienDonHang().compareTo(ct.getDieuKienToiThieu()) < 0) {
            throw new IllegalArgumentException("Đơn hàng tối thiểu phải từ " + ct.getDieuKienToiThieu() + " đ để áp dụng mã này.");
        }

        BigDecimal soTienGiam = BigDecimal.ZERO;
        if ("PhanTram".equalsIgnoreCase(ct.getLoaiGiam())) {
            soTienGiam = req.getTongTienDonHang().multiply(ct.getGiaTriGiam()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            if (ct.getSoTienGiamToiDa() != null && soTienGiam.compareTo(ct.getSoTienGiamToiDa()) > 0) {
                soTienGiam = ct.getSoTienGiamToiDa();
            }
        } else {
            soTienGiam = ct.getGiaTriGiam();
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("valid", true);
        res.put("codeKhuyenMai", mkm.getCodeKhuyenMai());
        res.put("tenChuongTrinh", ct.getTenChuongTrinh());
        res.put("soTienGiam", soTienGiam);
        return res;
    }

    public Map<String, Object> createBooking(BookingCreateRequest req) {
        KhachHang khachHang = null;
        if (req.getKhachHangId() != null) {
            khachHang = khachHangRepository.findById(req.getKhachHangId()).orElse(null);
        }
        if (khachHang == null && req.getTaiKhoanId() != null) {
            khachHang = khachHangRepository.findByTaiKhoan_Id(req.getTaiKhoanId()).orElse(null);
        }
        if (khachHang == null) {
            throw new IllegalArgumentException("Không xác định được thông tin Khách hàng để tạo đơn đặt.");
        }

        // Đơn nhiều dịch vụ (giỏ hàng): một lịch, một địa chỉ, một hóa đơn; mỗi dịch vụ một dòng chi tiết
        List<ChiTietDonDatRequest> dongs = req.getDanhSachDichVu();
        boolean nhieu = dongs.size() >= 2;
        List<DichVu> dsDichVu = new ArrayList<>();
        for (ChiTietDonDatRequest dong : dongs) {
            dsDichVu.add(dichVuRepository.findById(dong.getDichVuId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ ID: " + dong.getDichVuId())));
        }
        if (dsDichVu.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn dịch vụ.");
        if (nhieu) {
            String loiDs = DatNhieuDichVu.loiDanhSach(dsDichVu);
            if (loiDs != null) throw new IllegalArgumentException(loiDs);
        }
        DichVu dichVu = dsDichVu.get(0);

        // Địa chỉ thực hiện
        DiaChiKhachHang diaChi = null;
        if (req.getDiaChiId() != null) {
            // Chỉ nhận địa chỉ còn dùng của chính khách hàng đặt đơn
            diaChi = customerAddressService.timDiaChi(khachHang.getId(), req.getDiaChiId());
        }
        if (diaChi == null && req.getMaPhuong() != null) {
            // Địa chỉ mới nhập lúc đặt: lưu vào sổ địa chỉ (tỉnh, phường → khu vực)
            com.example.DTO.request.AddressRequest dcMoi = new com.example.DTO.request.AddressRequest();
            dcMoi.setSoNhaTenDuong(req.getSoNhaTenDuong() != null ? req.getSoNhaTenDuong() : req.getDiaChiChiTiet());
            dcMoi.setMaTinh(req.getMaTinh());
            dcMoi.setMaQuan(req.getMaQuan());
            dcMoi.setMaPhuong(req.getMaPhuong());
            dcMoi.setKhuVucId(req.getKhuVucId());
            Integer idMoi = (Integer) customerAddressService.them(khachHang.getId(), dcMoi).get("id");
            diaChi = customerAddressService.timDiaChi(khachHang.getId(), idMoi);
        }
        if (diaChi == null && req.getDiaChiChiTiet() != null && !req.getDiaChiChiTiet().isBlank()) {
            if (req.getKhuVucId() == null) {
                throw new IllegalArgumentException("Vui lòng chọn tỉnh, thành và phường, xã của địa chỉ thực hiện.");
            }
            KhuVuc kv = khuVucService.layDangPhucVu(req.getKhuVucId());
            diaChi = DiaChiKhachHang.builder()
                    .maDiaChi(com.example.Service.MaSinh.tao("DC-"))
                    .khachHang(khachHang)
                    .khuVuc(kv)
                    .diaChiChiTiet(req.getDiaChiChiTiet().trim())
                    .laMacDinh(false)
                    .trangThai("HoatDong")
                    .build();
            diaChi = diaChiKhachHangRepository.save(diaChi);
        }
        if (diaChi == null) {
            diaChi = diaChiKhachHangRepository.findFirstByKhachHang_IdAndLaMacDinhTrue(khachHang.getId())
                    .filter(dc -> "HoatDong".equals(dc.getTrangThai()))
                    .orElse(null);
        }
        if (diaChi == null) {
            List<DiaChiKhachHang> dcs = diaChiKhachHangRepository.findByKhachHang_IdAndTrangThai(khachHang.getId(), "HoatDong");
            if (!dcs.isEmpty()) diaChi = dcs.get(0);
            else throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ thực hiện dịch vụ.");
        }
        // Cộng tác viên nhận việc theo khu vực (quận, huyện): địa chỉ phải nằm trong khu vực phục vụ
        if (diaChi.getKhuVuc() == null) {
            throw new IllegalArgumentException("Địa chỉ \"" + diaChi.getDiaChiChiTiet()
                    + "\" nằm ngoài khu vực phục vụ (các quận, huyện TP. Hồ Chí Minh). Vui lòng chọn địa chỉ khác.");
        }

        // Tính giá (không còn goiDichVuId)
        CalculatePriceRequest calcReq = new CalculatePriceRequest();
        calcReq.setDichVuId(dichVu.getId());
        if (nhieu) calcReq.setChiTiet(dongs);
        calcReq.setLoaiHinhDat(req.getLoaiHinhDat());
        calcReq.setBangGiaId(req.getBangGiaId());
        calcReq.setCodeKhuyenMai(req.getCodeKhuyenMai());
        calcReq.setNgayThucHien(req.getNgayThucHien());
        calcReq.setGioBatDau(req.getGioBatDau());
        PriceCalculationResult priceResult = calculatePrice(calcReq);

        // Khuyến mãi nếu có
        MaKhuyenMai mkm = null;
        if (req.getCodeKhuyenMai() != null && !req.getCodeKhuyenMai().isBlank()) {
            mkm = maKhuyenMaiRepository.findByCodeKhuyenMaiIgnoreCase(req.getCodeKhuyenMai().trim()).orElse(null);
        }
        // Mã không giảm được (hết hạn, chưa đủ giá trị tối thiểu...) thì không gắn vào đơn
        if (mkm != null && priceResult.getSoTienGiam().signum() <= 0) mkm = null;

        // Bảng giá snapshot tại thời điểm đặt
        // Bảng giá có hiệu lực lúc bắt đầu làm; không có ngày thì dùng bangGiaId client gửi
        BangGiaDichVu bg = req.getNgayThucHien() == null && req.getBangGiaId() != null
                ? bangGiaDichVuRepository.findById(req.getBangGiaId()).orElse(null)
                : (req.getNgayThucHien() != null && req.getGioBatDau() != null
                        ? bangGiaService.giaApDung(dichVu.getId(), LocalDateTime.of(req.getNgayThucHien(), req.getGioBatDau()))
                        : bangGiaService.giaApDung(dichVu.getId(), req.getNgayThucHien())).orElse(null); // bảng giá lúc bắt đầu làm

        // Gói tháng: các thứ trong tuần phải đủ số buổi của gói trong vòng 1 tháng kể từ ngày bắt đầu.
        // Ngày thực hiện của đơn = buổi đầu tiên; thứ được chuẩn hóa ("3,5,7,CN") để tính lại lịch khi xem.
        LocalDate ngayThucHien = req.getNgayThucHien();
        if (ngayThucHien != null && ngayThucHien.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày thực hiện không được ở quá khứ.");
        }
        String thuTrongTuan = req.getNgayThucHienTrongTuan();
        if ("GoiThang".equalsIgnoreCase(priceResult.getLoaiHinhDat())) {
            if (ngayThucHien == null) {
                throw new IllegalArgumentException("Vui lòng chọn ngày bắt đầu gói.");
            }
            List<LocalDate> lich = LichGoiThang.tinhLichDu(ngayThucHien, thuTrongTuan,
                    LichGoiThang.soBuoi(dichVu.getSoBuoi(), dichVu.getTenDichVu()));
            ngayThucHien = lich.get(0);
            thuTrongTuan = LichGoiThang.vietThu(LichGoiThang.docThu(thuTrongTuan));
        }
        if ("GoiThang".equalsIgnoreCase(priceResult.getLoaiHinhDat())
                && !"GoiThang".equalsIgnoreCase(dichVu.getLoaiHinhDat())) {
            throw new IllegalArgumentException("Dịch vụ này không có gói tháng, vui lòng chọn gói tháng trong danh sách.");
        }
        // Buổi đầu tiên (gói tháng: ngày đầu khớp thứ đã chọn): theo lần trong vòng 1 tháng, gói bắt đầu
        // trong vòng 3 tháng, sau hiện tại ít nhất 1 giờ – cùng quy tắc với website
        String loiNgay = RangBuocNgayDat.loiNgayDat(priceResult.getLoaiHinhDat(), ngayThucHien, req.getGioBatDau(),
                LocalDateTime.now());
        if (loiNgay != null) throw new IllegalArgumentException(loiNgay);

        LocalTime gioKetThuc = req.getGioKetThuc();
        if (nhieu) {
            // Các dịch vụ làm nối tiếp nhau: kết thúc = bắt đầu + tổng thời lượng, phải xong trước 20:00
            int tongPhut = DatNhieuDichVu.tongPhut(dsDichVu);
            String loiGio = DatNhieuDichVu.loiGioKetThuc(req.getGioBatDau(), tongPhut);
            if (loiGio != null) throw new IllegalArgumentException(loiGio);
            gioKetThuc = DatNhieuDichVu.gioKetThuc(req.getGioBatDau(), tongPhut);
        } else if (gioKetThuc == null && dichVu.getThoiGianThucHien() != null) {
            gioKetThuc = req.getGioBatDau().plusMinutes(dichVu.getThoiGianThucHien());
        } else if (gioKetThuc == null) {
            gioKetThuc = req.getGioBatDau().plusHours(2);
        }

        String suffix = com.example.Service.MaSinh.duoi(); // không trùng khi nhiều người đăng ký cùng lúc
        String maDonDat = "DD-" + suffix;

        DonDatDichVu donDat = DonDatDichVu.builder()
                .maDonDat(maDonDat)
                .khachHang(khachHang)
                .diaChi(diaChi)
                .khuyenMai(mkm)
                .loaiHinhDat(priceResult.getLoaiHinhDat())
                .ngayThucHien(ngayThucHien)
                .gioBatDau(req.getGioBatDau())
                .gioKetThuc(gioKetThuc)
                .yeuCauDacBiet(req.getYeuCauDacBiet())
                .chiPhiGoc(priceResult.getChiPhiGoc())
                .soTienGiam(priceResult.getSoTienGiam())
                .thanhTien(priceResult.getThanhTien())
                .trangThai("ChoDuyet")
                .ngayTao(LocalDateTime.now())
                .ghiChu(req.getGhiChu())
                .build();
        donDat = donDatDichVuRepository.save(donDat);

        // Tạo ChiTietDonDat: mỗi dịch vụ một dòng, giá và bảng giá của chính dịch vụ đó
        if (nhieu) {
            for (int i = 0; i < dsDichVu.size(); i++) {
                DichVu dvDong = dsDichVu.get(i);
                BigDecimal giaDong = priceResult.getChiTietGia().get(i).donGia();
                BangGiaDichVu bgDong = bangGiaService.giaApDung(dvDong.getId(),
                        LocalDateTime.of(ngayThucHien, req.getGioBatDau())).orElse(null);
                chiTietDonDatRepository.save(ChiTietDonDat.builder()
                        .donDat(donDat)
                        .dichVu(dvDong)
                        .bangGia(bgDong)
                        .soLuong(1)
                        .donGia(giaDong)
                        .thanhTien(giaDong)
                        .ghiChu(dongs.get(i).getGhiChu())
                        .build());
            }
        } else {
            ChiTietDonDat chiTiet = ChiTietDonDat.builder()
                    .donDat(donDat)
                    .dichVu(dichVu)
                    .bangGia(bg)
                    .soLuong(1)
                    .donGia(priceResult.getChiPhiGoc())
                    .thanhTien(priceResult.getChiPhiGoc())
                    .ngayThucHienTrongTuan(thuTrongTuan)
                    .ghiChu(req.getGhiChu())
                    .build();
            chiTietDonDatRepository.save(chiTiet);
        }

        // Lưu lịch sử trạng thái đơn
        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(donDat)
                .trangThaiCu(null)
                .trangThaiMoi("ChoDuyet")
                .nguoiThucHien("KhachHang: " + khachHang.getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("Khách hàng tạo đơn đặt dịch vụ qua Mobile App")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Ghi nhận sử dụng khuyến mại
        if (mkm != null && priceResult.getSoTienGiam().compareTo(BigDecimal.ZERO) > 0) {
            // Cập nhật nguyên tử trong CSDL: nếu người khác vừa dùng lượt cuối thì đơn này bị từ chối
            // và toàn bộ giao dịch tạo đơn được hoàn tác (rollback).
            Integer khuyenMaiId = mkm.getId();
            if (maKhuyenMaiRepository.ghiNhanLuotDung(khuyenMaiId) == 0) {
                throw new IllegalArgumentException("Mã khuyến mãi vừa hết lượt sử dụng. Vui lòng bỏ mã hoặc chọn mã khác.");
            }
            mkm = maKhuyenMaiRepository.findById(khuyenMaiId).orElse(mkm);

            LichSuSuDungKhuyenMai lsKm = LichSuSuDungKhuyenMai.builder()
                    .khuyenMai(mkm)
                    .khachHang(khachHang)
                    .donDat(donDat)
                    .ngaySuDung(LocalDateTime.now())
                    .soTienDuocGiam(priceResult.getSoTienGiam())
                    .build();
            lichSuSuDungKhuyenMaiRepository.save(lsKm);
        }

        // Tự động sinh Hóa đơn ở trạng thái ChuaThanhToan
        HoaDon hoaDon = HoaDon.builder()
                .maHoaDon("HD-" + suffix)
                .donDat(donDat)
                .khachHang(khachHang)
                .ngayLap(LocalDateTime.now())
                .hinhThucThanhToan(HanNhanDonService.chuanHoaHinhThuc(req.getHinhThucThanhToan()))
                .tongTienHang(priceResult.getChiPhiGoc())
                .soTienGiam(priceResult.getSoTienGiam())
                .tongThanhToan(priceResult.getThanhTien())
                .trangThaiThanhToan("ChuaThanhToan")
                .build();
        hoaDon = hoaDonRepository.save(hoaDon);

        // Chi tiết hóa đơn
        ChiTietHoaDon cthd = ChiTietHoaDon.builder()
                .hoaDon(hoaDon)
                .tenDichVu(dichVu.getTenDichVu())
                .yeuCauDacBiet(req.getYeuCauDacBiet())
                .donViTinh(dichVu.getDonViTinh())
                .soLuong(BigDecimal.ONE)
                .donGia(priceResult.getChiPhiGoc())
                .thanhTien(priceResult.getChiPhiGoc())
                .loaiDong("DichVuChinh")
                .build();
        chiTietHoaDonRepository.save(cthd);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("donDatId", donDat.getId());
        result.put("maDonDat", donDat.getMaDonDat());
        result.put("hoaDonId", hoaDon.getId());
        result.put("hinhThucThanhToan", hoaDon.getHinhThucThanhToan());
        result.put("maHoaDon", hoaDon.getMaHoaDon());
        result.put("tongThanhToan", donDat.getThanhTien());
        result.put("trangThai", donDat.getTrangThai());
        result.put("message", "Đặt dịch vụ thành công! Đơn hàng đã được chuyển đến bộ phận điều phối.");
        return result;
    }

    // ==========================================
    // UC-KH05: THEO DÕI ĐƠN & LỊCH LÀM VIỆC CỦA KH
    // ==========================================
    public List<Map<String, Object>> getCustomerBookings(Integer khachHangId, String trangThai) {
        List<DonDatDichVu> list;
        if (trangThai != null && !trangThai.isBlank()) {
            list = donDatDichVuRepository.findByKhachHang_IdAndTrangThaiOrderByNgayTaoDesc(khachHangId, trangThai);
        } else {
            list = donDatDichVuRepository.findByKhachHang_IdOrderByNgayTaoDesc(khachHangId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (DonDatDichVu d : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", d.getId());
            item.put("maDonDat", d.getMaDonDat());
            // Tên dịch vụ của đơn (nhiều dịch vụ: "A + B")
            String tenDichVu = DatNhieuDichVu.tenDichVuCuaDon(d, "");
            item.put("tenDichVu", tenDichVu);
            item.put("ngayThucHien", d.getNgayThucHien());
            item.put("gioBatDau", d.getGioBatDau());
            item.put("gioKetThuc", d.getGioKetThuc());
            item.put("thanhTien", d.getThanhTien());
            item.put("trangThai", d.getTrangThai());
            // Đơn chuyển khoản chưa thanh toán: app hiện "Chờ thanh toán" thay vì "Đang tìm CTV"
            item.put("soGiayConLaiThanhToan", "ChoDuyet".equals(d.getTrangThai())
                    ? HanNhanDonService.soGiayConLai(hanNhanDonService.hanThanhToanNeuDangCho(d.getId()), LocalDateTime.now())
                    : null);
            item.put("diaChi", d.getDiaChi() != null ? d.getDiaChi().getDiaChiChiTiet() : "");

            // Thông tin CTV phụ trách nếu đã phân công
            phanCongCTVRepository.findByDonDat_Id(d.getId()).ifPresent(pc -> {
                CongTacVien ctv = pc.getCongTacVien();
                item.put("congTacVienTen", ctv.getHoTen());
                item.put("congTacVienPhone", ctv.getSoDienThoai());
                item.put("congTacVienDiem", ctv.getDiemDanhGia());
                item.put("trangThaiPhanCong", pc.getTrangThai());
            });

            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getBookingDetail(Integer donDatId) {
        DonDatDichVu d = donDatDichVuRepository.findById(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + donDatId));

        Map<String, Object> result = new LinkedHashMap<>();
        // Đồng hồ cho app: hạn chuyển khoản (đơn chờ thanh toán) và hạn có người nhận (đơn đã phát hành)
        LocalDateTime bayGio = LocalDateTime.now();
        result.put("soGiayConLaiThanhToan",
                HanNhanDonService.soGiayConLai(hanNhanDonService.hanThanhToanNeuDangCho(donDatId), bayGio));
        result.put("soGiayConLaiNhan",
                HanNhanDonService.soGiayConLai(hanNhanDonService.hanNhanNeuDangCho(donDatId), bayGio));
        result.put("soPhutNhanDon", hanNhanDonService.soPhut());
        result.put("soPhutThanhToan", hanNhanDonService.soPhutThanhToan());
        result.put("id", d.getId());
        result.put("maDonDat", d.getMaDonDat());
        // Lấy danh sách dịch vụ trong đơn
        String tenDichVu = DatNhieuDichVu.tenDichVuCuaDon(d, "");
        result.put("tenDichVu", tenDichVu);
        result.put("danhSachDichVu", d.getChiTietList().stream()
                .map(ct -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("dichVuId", ct.getDichVu().getId());
                    m.put("tenDichVu", ct.getDichVu().getTenDichVu());
                    m.put("soLuong", ct.getSoLuong());
                    m.put("donGia", ct.getDonGia());
                    m.put("thanhTien", ct.getThanhTien());
                    m.put("ngayThucHienTrongTuan", ct.getNgayThucHienTrongTuan());
                    return m;
                }).toList());
        result.put("loaiHinhDat", d.getLoaiHinhDat());
        result.put("ngayThucHien", d.getNgayThucHien());
        if ("GoiThang".equalsIgnoreCase(d.getLoaiHinhDat()) && d.getChiTietList() != null && !d.getChiTietList().isEmpty()) {
            ChiTietDonDat ct = d.getChiTietList().get(0);
            String thu = ct.getNgayThucHienTrongTuan();
            int soBuoiGoi = LichGoiThang.soBuoi(ct.getDichVu().getSoBuoi(), ct.getDichVu().getTenDichVu());
            result.put("thuTrongTuan", thu);
            result.put("thuTrongTuanText", LichGoiThang.nhanThu(thu));
            result.put("soBuoiGoi", soBuoiGoi);
            result.put("lichBuoi", LichGoiThang.tinhLich(d.getNgayThucHien(), LichGoiThang.docThu(thu), soBuoiGoi));
        }
        result.put("gioBatDau", d.getGioBatDau());
        result.put("gioKetThuc", d.getGioKetThuc());
        result.put("yeuCauDacBiet", d.getYeuCauDacBiet());
        result.put("chiPhiGoc", d.getChiPhiGoc());
        result.put("soTienGiam", d.getSoTienGiam());
        result.put("thanhTien", d.getThanhTien());
        result.put("trangThai", d.getTrangThai());
        result.put("ngayTao", d.getNgayTao());
        result.put("diaChi", d.getDiaChi() != null ? d.getDiaChi().getDiaChiChiTiet() : "");

        // CTV phụ trách
        phanCongCTVRepository.findByDonDat_Id(d.getId()).ifPresent(pc -> {
            CongTacVien ctv = pc.getCongTacVien();
            Map<String, Object> ctvMap = new LinkedHashMap<>();
            ctvMap.put("id", ctv.getId());
            ctvMap.put("hoTen", ctv.getHoTen());
            ctvMap.put("soDienThoai", ctv.getSoDienThoai());
            ctvMap.put("diemDanhGia", ctv.getDiemDanhGia());
            ctvMap.put("trangThaiPhanCong", pc.getTrangThai());
            // Vị trí GPS gần nhất của CTV (lưu trên Redis, không lưu CSDL) khi đơn đã được nhận
            if (!"TuChoi".equals(pc.getTrangThai())) {
                viTriCtvStore.get(ctv.getId()).ifPresent(vt -> {
                    ctvMap.put("viDo", vt.viDo());
                    ctvMap.put("kinhDo", vt.kinhDo());
                    ctvMap.put("thoiGianCapNhatViTri", vt.thoiGian());
                });
            }
            result.put("congTacVien", ctvMap);
        });

        // Hóa đơn
        hoaDonRepository.findByDonDat_Id(d.getId()).ifPresent(hd -> {
            Map<String, Object> hdMap = new LinkedHashMap<>();
            hdMap.put("id", hd.getId());
            hdMap.put("maHoaDon", hd.getMaHoaDon());
            hdMap.put("trangThaiThanhToan", hd.getTrangThaiThanhToan());
            hdMap.put("hinhThucThanhToan", hd.getHinhThucThanhToan());
            hdMap.put("tongThanhToan", hd.getTongThanhToan());
            result.put("hoaDon", hdMap);
        });

        // Đánh giá nếu có (kèm trạng thái duyệt, hạn sửa và trao đổi với bTaskee)
        Map<String, Object> danhGia = danhGiaService.cuaDon(d.getId());
        if (danhGia != null) result.put("danhGia", danhGia);

        // Hình ảnh tài sản Trước & Sau khi thực hiện dịch vụ
        if (collaboratorApiService != null) {
            result.putAll(collaboratorApiService.layThongTinTaiSanDon(d.getId()));
        }

        return result;
    }

    public Map<String, Object> cancelBooking(Integer donDatId, Integer khachHangId, String lyDo) {
        DonDatDichVu d = donDatDichVuRepository.findByIdForUpdate(donDatId) // khóa đơn: tránh vừa hủy vừa được phân công
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + donDatId));

        if (khachHangId != null && (d.getKhachHang() == null || !khachHangId.equals(d.getKhachHang().getId()))) {
            throw new IllegalArgumentException("Không tìm thấy đơn.");
        }
        // Hủy được khi đơn chưa bắt đầu làm (chờ duyệt, hoặc đã có cộng tác viên nhận) và chưa tới giờ bắt đầu
        if (!"ChoDuyet".equalsIgnoreCase(d.getTrangThai()) && !"DaXacNhan".equalsIgnoreCase(d.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ hủy được đơn chưa bắt đầu thực hiện.");
        }
        if (d.getNgayThucHien() != null && d.getGioBatDau() != null
                && !LocalDateTime.of(d.getNgayThucHien(), d.getGioBatDau()).isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Đơn đã tới giờ bắt đầu, vui lòng liên hệ CSKH để hủy.");
        }

        String trangThaiCu = d.getTrangThai();
        d.setTrangThai("DaHuy");
        donDatDichVuRepository.save(d);

        // Gỡ lịch làm việc chưa làm và lời mời đang chờ; cộng tác viên đã nhận đơn được báo qua ThongBaoSuKienService
        for (LichLamViec l : lichLamViecRepository.findByPhanCong_DonDat_Id(d.getId())) {
            if ("SapToi".equals(l.getTrangThai())) {
                l.setTrangThai("Huy");
                lichLamViecRepository.save(l);
            }
        }
        for (PhanCongCTV pc : phanCongCTVRepository.findAllByDonDat_Id(d.getId())) {
            if ("ChoPhanCong".equals(pc.getTrangThai())) {
                pc.setTrangThai("TuChoi");
                pc.setLyDoTuChoi("Khách hàng đã hủy đơn");
                phanCongCTVRepository.save(pc);
            }
        }

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(d)
                .trangThaiCu(trangThaiCu)
                .trangThaiMoi("DaHuy")
                .nguoiThucHien("KhachHang ID: " + khachHangId)
                .thoiGian(LocalDateTime.now())
                .ghiChu("Hủy đơn qua Mobile App. Lý do: " + (lyDo != null ? lyDo : "Không có"))
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("donDatId", d.getId());
        res.put("trangThai", d.getTrangThai());
        res.put("message", "Hủy đơn đặt dịch vụ thành công.");
        return res;
    }

    // ==========================================
    // UC-KH06: THANH TOÁN & BIÊN LAI
    // ==========================================
    public Map<String, Object> getInvoiceInfo(Integer donDatId) {
        DonDatDichVu donDat = donDatDichVuRepository.findById(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + donDatId));

        HoaDon hd = hoaDonRepository.findByDonDat_Id(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Chưa có hóa đơn cho đơn đặt này."));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("hoaDonId", hd.getId());
        res.put("maHoaDon", hd.getMaHoaDon());
        res.put("donDatId", donDat.getId());
        res.put("maDonDat", donDat.getMaDonDat());
        res.put("khachHangTen", hd.getKhachHang().getHoTen());
        res.put("tongTienHang", hd.getTongTienHang());
        res.put("soTienGiam", hd.getSoTienGiam());
        res.put("tongThanhToan", hd.getTongThanhToan());
        res.put("trangThaiThanhToan", hd.getTrangThaiThanhToan());
        res.put("hinhThucThanhToan", hd.getHinhThucThanhToan());

        // Tạo dữ liệu mã QR VietQR mẫu liên kết tài khoản ngân hàng SePay
        String bankCode = (sepayBankCode != null && !sepayBankCode.isBlank()) ? sepayBankCode : "MB";
        String accountNo = (sepayAccountNo != null && !sepayAccountNo.isBlank()) ? sepayAccountNo : "0336676173";
        String accountName = (sepayAccountName != null && !sepayAccountName.isBlank()) ? sepayAccountName : "NGUYEN VUONG LE";
        String prefix = (sepayTransferPrefix != null && !sepayTransferPrefix.isBlank()) ? sepayTransferPrefix : "BTK";
        String content = prefix + donDat.getId(); // Ví dụ: BTK30
        
        String encodedAccountName = "";
        String encodedContent = "";
        try {
            encodedAccountName = java.net.URLEncoder.encode(accountName, java.nio.charset.StandardCharsets.UTF_8.toString());
            encodedContent = java.net.URLEncoder.encode(content, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (Exception ignored) {
            encodedAccountName = accountName.replace(" ", "%20");
            encodedContent = content;
        }

        String qrQuickLink = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankCode, accountNo, hd.getTongThanhToan().toPlainString(), encodedContent, encodedAccountName);

        Map<String, Object> bankInfo = new LinkedHashMap<>();
        bankInfo.put("nganHang", "MBBank (Ngân hàng Quân Đội)");
        bankInfo.put("maNganHang", bankCode);
        bankInfo.put("soTaiKhoan", accountNo);
        bankInfo.put("chuTaiKhoan", accountName);
        bankInfo.put("noiDungChuyenKhoan", content);
        bankInfo.put("soTien", hd.getTongThanhToan());
        bankInfo.put("qrCodeUrl", qrQuickLink);

        res.put("thongTinNganHang", bankInfo);
        return res;
    }

    /**
     * Điểm vào duy nhất để ghi nhận một khoản chuyển khoản của khách: nút "Tôi đã chuyển khoản" trên website
     * hôm nay, webhook VietQR sau này. Khoá đơn để không đụng độ với tác vụ tự hủy đơn quá hạn thanh toán.
     *
     * @param nguon ghi vào biên lai (người thu tiền), VD "Khách xác nhận trên website", "VietQR"
     */
    public Map<String, Object> xacNhanThanhToanChuyenKhoan(Integer donDatId, String nguon) {
        donDatDichVuRepository.findByIdForUpdate(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + donDatId));
        PaymentReceiptRequest req = new PaymentReceiptRequest();
        req.setDonDatId(donDatId);
        req.setHinhThucThanhToan("ChuyenKhoan");
        req.setNguoiThuTien(nguon);
        return createReceipt(req);
    }

    public Map<String, Object> createReceipt(PaymentReceiptRequest req) {
        HoaDon hd = null;
        // Khóa hóa đơn trong lúc ghi nhận thanh toán: hai yêu cầu thanh toán cùng lúc sẽ lần lượt xử lý,
        // yêu cầu sau thấy hóa đơn đã thanh toán và bị từ chối (không tạo biên lai thứ hai).
        if (req.getHoaDonId() != null) {
            hd = hoaDonRepository.findByIdForUpdate(req.getHoaDonId()).orElse(null);
        }
        if (hd == null && req.getDonDatId() != null) {
            hd = hoaDonRepository.findByDonDatIdForUpdate(req.getDonDatId()).orElse(null);
        }
        if (hd == null) {
            throw new IllegalArgumentException("Không tìm thấy hóa đơn cần thanh toán.");
        }
        if ("DaThanhToan".equalsIgnoreCase(hd.getTrangThaiThanhToan())) {
            throw new IllegalArgumentException("Hóa đơn này đã được thanh toán.");
        }
        DonDatDichVu donCuaHd = hd.getDonDat();
        if (donCuaHd != null && "DaHuy".equalsIgnoreCase(donCuaHd.getTrangThai())) {
            throw new IllegalArgumentException("Đơn này đã bị hủy, không thanh toán được nữa.");
        }
        boolean choPhatHanh = donCuaHd != null && "ChoDuyet".equalsIgnoreCase(donCuaHd.getTrangThai())
                && "ChuyenKhoan".equalsIgnoreCase(req.getHinhThucThanhToan() != null
                        ? req.getHinhThucThanhToan() : hd.getHinhThucThanhToan());

        hd.setTrangThaiThanhToan("DaThanhToan");
        hd.setNgayThanhToan(LocalDateTime.now());
        if (req.getHinhThucThanhToan() != null) {
            hd.setHinhThucThanhToan(req.getHinhThucThanhToan());
        }
        hoaDonRepository.save(hd);

        // Lưu biên lai
        BigDecimal soTien = req.getSoTienNhan() != null ? req.getSoTienNhan() : hd.getTongThanhToan();
        String nguoiNop = req.getNguoiNopTien() != null ? req.getNguoiNopTien() : hd.getKhachHang().getHoTen();
        String nguoiThu = req.getNguoiThuTien() != null ? req.getNguoiThuTien() : "Hệ thống thanh toán tự động";

        BienLai bienLai = BienLai.builder()
                .maBienLai(com.example.Service.MaSinh.tao("BL-"))
                .hoaDon(hd)
                .ngayGioThuTien(LocalDateTime.now())
                .soTienNhan(soTien)
                .hinhThucThanhToan(hd.getHinhThucThanhToan())
                .nguoiNopTien(nguoiNop)
                .nguoiThuTien(nguoiThu)
                .build();
        bienLai = bienLaiRepository.save(bienLai);

        // Đơn chuyển khoản chờ thanh toán: từ giờ mới phát hành cho cộng tác viên (báo CTV, CSKH)
        if (choPhatHanh) com.example.SuKien.BoPhatSuKien.phat(new com.example.SuKien.SuKienDon.DaPhatHanh(donCuaHd.getId()));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("bienLaiId", bienLai.getId());
        res.put("maBienLai", bienLai.getMaBienLai());
        res.put("soTienNhan", bienLai.getSoTienNhan());
        res.put("ngayGioThuTien", bienLai.getNgayGioThuTien());
        res.put("trangThaiThanhToan", "DaThanhToan");
        res.put("message", "Thanh toán thành công. Biên lai điện tử đã được ghi nhận.");
        return res;
    }

    // ==========================================
    // UC-KH07: ĐÁNH GIÁ VÀ PHẢN HỒI DỊCH VỤ
    // ==========================================
    public Map<String, Object> createReview(ReviewCreateRequest req) {
        Map<String, Object> res = new LinkedHashMap<>(danhGiaService.danhGia(req.getDonDatId(), req.getKhachHangId(),
                req.getDiemChatLuong(), req.getDiemThaiDo(), req.getNhanXet()));
        res.put("danhGiaId", res.get("id"));
        res.put("message", "Gửi đánh giá thành công! Cảm ơn bạn đã phản hồi về dịch vụ.");
        return res;
    }

    /** Đánh giá công khai của một dịch vụ (chỉ đánh giá đã duyệt). */
    public List<Map<String, Object>> getReviewsByService(Integer dichVuId) {
        return danhGiaService.congKhaiTheoDichVu(dichVuId);
    }

    // ==========================================
    // UC-KH08: GỬI KHIẾU NẠI & TRA CỨU
    // ==========================================
    public Map<String, Object> createComplaint(ComplaintCreateRequest req) {
        DonDatDichVu donDat = donDatDichVuRepository.findById(req.getDonDatId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + req.getDonDatId()));

        KhieuNai khieuNai = KhieuNai.builder()
                .maKhieuNai(com.example.Service.MaSinh.tao("KN-"))
                .donDat(donDat)
                .khachHang(donDat.getKhachHang())
                .loaiVanDe(req.getLoaiVanDe())
                .noiDung(req.getNoiDung())
                .trangThai("Moi")
                .ngayGui(LocalDateTime.now())
                .build();
        khieuNai = khieuNaiRepository.save(khieuNai);

        // Lưu danh sách file ảnh/bằng chứng từ MinIO
        if (req.getDanhSachFileDinhKem() != null) {
            for (String fileUrl : req.getDanhSachFileDinhKem()) {
                if (fileUrl != null && !fileUrl.isBlank()) {
                    TaiLieuKhieuNai tl = TaiLieuKhieuNai.builder()
                            .khieuNai(khieuNai)
                            .loaiFile(minioService.phanLoaiFile(fileUrl)) // HinhAnh | Video | TaiLieuKhac
                            .duongDanFile(minioService.toStoredValue(fileUrl)) // Lưu object key MinIO
                            .ngayTai(LocalDateTime.now())
                            .build();
                    taiLieuKhieuNaiRepository.save(tl);
                }
            }
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("khieuNaiId", khieuNai.getId());
        res.put("maKhieuNai", khieuNai.getMaKhieuNai());
        res.put("trangThai", khieuNai.getTrangThai());
        res.put("message", "Gửi khiếu nại thành công! Bộ phận CSKH sẽ liên hệ với bạn trong thời gian sớm nhất.");
        return res;
    }

    public List<Map<String, Object>> getCustomerComplaints(Integer khachHangId) {
        List<KhieuNai> list = khieuNaiRepository.findByKhachHang_IdOrderByNgayGuiDesc(khachHangId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (KhieuNai kn : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", kn.getId());
            m.put("maKhieuNai", kn.getMaKhieuNai());
            m.put("maDonDat", kn.getDonDat().getMaDonDat());
            m.put("loaiVanDe", kn.getLoaiVanDe());
            m.put("noiDung", kn.getNoiDung());
            m.put("trangThai", kn.getTrangThai());
            m.put("ngayGui", kn.getNgayGui());
            m.put("taiLieuDinhKem", taiLieuKhieuNaiRepository.findByKhieuNai_Id(kn.getId()));
            result.add(m);
        }
        return result;
    }

    // ==========================================
    // UC-KH09: NHẬN THÔNG BÁO CỦA KHÁCH HÀNG
    // ==========================================
    public List<Map<String, Object>> getCustomerNotifications(Integer taiKhoanId) {
        // Resolve TaiKhoan linh hoạt (hỗ trợ truyền taiKhoanId hoặc khachHangId)
        TaiKhoan tk = resolveTaiKhoan(taiKhoanId);
        if (tk == null) {
            return Collections.emptyList();
        }
        Integer actualTaiKhoanId = tk.getId();

        // 1. Tự động đồng bộ các thông báo chung từ Marketing, Ban Giám Đốc, CSKH (nhomNhan = KhachHang hoặc TatCa)
        List<ThongBao> broadcastList = thongBaoRepository.findAll().stream()
                .filter(tb -> "DaGui".equalsIgnoreCase(tb.getTrangThai()) &&
                        ("KhachHang".equalsIgnoreCase(tb.getNhomNhan()) || "TatCa".equalsIgnoreCase(tb.getNhomNhan())))
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
        if (!newlyCreated.isEmpty()) {
            thongBaoNguoiDungRepository.saveAll(newlyCreated);
            existingList = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(actualTaiKhoanId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ThongBaoNguoiDung tbnd : existingList) {
            ThongBao tb = tbnd.getThongBao();
            if (tb == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tbnd.getId());
            m.put("thongBaoId", tb.getId());
            m.put("tieuDe", tb.getTieuDe());
            m.put("noiDung", tb.getNoiDung());
            m.put("nguoiGui", tb.getNguoiGui() != null ? tb.getNguoiGui() : "Hệ thống");
            m.put("thoiGianGui", tb.getThoiGianGui());
            m.put("daDoc", tbnd.getDaDoc());
            m.put("thoiGianDoc", tbnd.getThoiGianDoc());

            // Phân loại thông báo cho Mobile hiển thị icon và màu sắc
            String text = ((tb.getTieuDe() != null ? tb.getTieuDe() : "") + " "
                    + (tb.getNoiDung() != null ? tb.getNoiDung() : "")).toLowerCase();
            String loai = "heThong";
            if (text.contains("khuyến mãi") || text.contains("ưu đãi") || text.contains("coupon") || text.contains("giảm giá") || text.contains("tặng bạn")) {
                loai = "khuyenMai";
            } else if (text.contains("hoàn thành")) {
                loai = "hoanThanh";
            } else if (text.contains("tiếp nhận") || text.contains("đã có cộng tác viên") || text.contains("xác nhận")) {
                loai = "tiepNhan";
            } else if (text.contains("lịch") || text.contains("nhắc lịch")) {
                loai = "nhacLich";
            }
            m.put("loai", loai);

            result.add(m);
        }
        return result;
    }

    public void markNotificationRead(Integer notificationId, Integer taiKhoanId) {
        TaiKhoan tk = resolveTaiKhoan(taiKhoanId);
        final Integer actualId = tk != null ? tk.getId() : taiKhoanId;
        thongBaoNguoiDungRepository.findById(notificationId).ifPresent(tbnd -> {
            if (tbnd.getTaiKhoan() != null && tbnd.getTaiKhoan().getId().equals(actualId)) {
                tbnd.setDaDoc(true);
                tbnd.setThoiGianDoc(LocalDateTime.now());
                thongBaoNguoiDungRepository.save(tbnd);
            }
        });
    }

    public void markAllNotificationsRead(Integer taiKhoanId) {
        TaiKhoan tk = resolveTaiKhoan(taiKhoanId);
        final Integer actualId = tk != null ? tk.getId() : taiKhoanId;
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(actualId);
        for (ThongBaoNguoiDung tb : list) {
            tb.setDaDoc(true);
            tb.setThoiGianDoc(LocalDateTime.now());
        }
        thongBaoNguoiDungRepository.saveAll(list);
    }

    private TaiKhoan resolveTaiKhoan(Integer id) {
        if (id == null) return null;
        TaiKhoan tk = taiKhoanRepository.findById(id).orElse(null);
        if (tk == null) {
            KhachHang kh = khachHangRepository.findById(id).orElse(null);
            if (kh != null) {
                tk = kh.getTaiKhoan();
            }
        }
        return tk;
    }
}
