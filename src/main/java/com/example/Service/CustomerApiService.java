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
    private final LichSuSuDungKhuyenMaiRepository lichSuSuDungKhuyenMaiRepository;
    private final LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final BienLaiRepository bienLaiRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final KhieuNaiRepository khieuNaiRepository;
    private final TaiLieuKhieuNaiRepository taiLieuKhieuNaiRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final PhanCongCTVRepository phanCongCTVRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final AuthService authService;

    /** Chuẩn hóa đường dẫn file MinIO (lưu object key vào CSDL). */
    @Autowired
    private MinioService minioService;

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
            PhanCongCTVRepository phanCongCTVRepository,
            LichLamViecRepository lichLamViecRepository,
            AuthService authService) {
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
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.authService = authService;
    }

    // ==========================================
    // UC-KH01: ĐĂNG KÝ, XÁC THỰC OTP, ĐĂNG NHẬP
    // ==========================================
    public Map<String, Object> registerCustomer(CustomerRegisterRequest req) {
        String soDienThoai = req.getSoDienThoai().trim();
        String email = (req.getEmail() != null && !req.getEmail().isBlank()) ? req.getEmail().trim() : (soDienThoai + "@customer.local");
        String tenDangNhap = (req.getTenDangNhap() != null && !req.getTenDangNhap().isBlank()) ? req.getTenDangNhap().trim() : soDienThoai;

        if (taiKhoanRepository.findBySoDienThoai(soDienThoai).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký trên hệ thống.");
        }
        if (taiKhoanRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email này đã được đăng ký trên hệ thống.");
        }
        if (taiKhoanRepository.findByTenDangNhap(tenDangNhap).isPresent()) {
            throw new IllegalArgumentException("Tên đăng nhập đã tồn tại, vui lòng chọn tên khác.");
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

        // 1. Tạo tài khoản (ở trạng thái ChoDuyet cho đến khi xác thực OTP)
        TaiKhoan taiKhoan = TaiKhoan.builder()
                .maTaiKhoan(maTaiKhoan)
                .tenDangNhap(tenDangNhap)
                .matKhau(req.getMatKhau().trim())
                .email(email)
                .soDienThoai(soDienThoai)
                .loaiTaiKhoan("KhachHang")
                .trangThai("ChoDuyet")
                .build();
        taiKhoan = taiKhoanRepository.save(taiKhoan);

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

        // 3. Tạo địa chỉ nếu có
        if (req.getDiaChiChiTiet() != null && !req.getDiaChiChiTiet().isBlank()) {
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

        // 4. Tạo mã OTP xác thực
        String otpCode = String.format("%06d", new Random().nextInt(1000000));
        OTPXacThuc otp = OTPXacThuc.builder()
                .taiKhoan(taiKhoan)
                .mucDich("DangKy")
                .maCode(otpCode)
                .thoiGianTao(LocalDateTime.now())
                .thoiGianHetHan(LocalDateTime.now().plusMinutes(5))
                .daSuDung(false)
                .build();
        otpXacThucRepository.save(otp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("khachHangId", khachHang.getId());
        result.put("soDienThoai", soDienThoai);
        result.put("email", email);
        result.put("otpCode", otpCode); // Phục vụ môi trường test/demo mobile
        result.put("thoiGianHetHanPhut", 5);
        result.put("message", "Đăng ký thành công! Vui lòng nhập mã OTP để kích hoạt tài khoản.");
        return result;
    }

    public Map<String, Object> verifyOtp(OtpVerifyRequest req) {
        String identifier = req.getIdentifier().trim();
        String mucDich = (req.getMucDich() != null && !req.getMucDich().isBlank()) ? req.getMucDich().trim() : "DangKy";

        TaiKhoan taiKhoan = taiKhoanRepository.findBySoDienThoai(identifier)
                .or(() -> taiKhoanRepository.findByEmail(identifier))
                .or(() -> taiKhoanRepository.findByTenDangNhap(identifier))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với thông tin đã cung cấp."));

        OTPXacThuc otp = otpXacThucRepository.findTopByTaiKhoanAndMucDichAndDaSuDungFalseOrderByThoiGianTaoDesc(taiKhoan, mucDich)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã OTP hợp lệ hoặc mã đã được sử dụng."));

        if (otp.getThoiGianHetHan().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu gửi lại mã mới.");
        }

        if (!otp.getMaCode().equals(req.getMaCode().trim())) {
            throw new IllegalArgumentException("Mã OTP không chính xác.");
        }

        otp.setDaSuDung(true);
        otpXacThucRepository.save(otp);

        taiKhoan.setTrangThai("HoatDong");
        taiKhoanRepository.save(taiKhoan);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("verified", true);
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("tenDangNhap", taiKhoan.getTenDangNhap());
        result.put("trangThai", taiKhoan.getTrangThai());
        result.put("message", "Xác thực OTP thành công. Tài khoản đã được kích hoạt.");
        return result;
    }

    public Map<String, Object> sendOtp(OtpSendRequest req) {
        String identifier = req.getIdentifier().trim();
        String mucDich = (req.getMucDich() != null && !req.getMucDich().isBlank()) ? req.getMucDich().trim() : "DangKy";

        TaiKhoan taiKhoan = taiKhoanRepository.findBySoDienThoai(identifier)
                .or(() -> taiKhoanRepository.findByEmail(identifier))
                .or(() -> taiKhoanRepository.findByTenDangNhap(identifier))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản với thông tin đã cung cấp."));

        String otpCode = String.format("%06d", new Random().nextInt(1000000));
        OTPXacThuc otp = OTPXacThuc.builder()
                .taiKhoan(taiKhoan)
                .mucDich(mucDich)
                .maCode(otpCode)
                .thoiGianTao(LocalDateTime.now())
                .thoiGianHetHan(LocalDateTime.now().plusMinutes(5))
                .daSuDung(false)
                .build();
        otpXacThucRepository.save(otp);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taiKhoanId", taiKhoan.getId());
        result.put("otpCode", otpCode);
        result.put("thoiGianHetHanPhut", 5);
        result.put("message", "Mã OTP mới đã được gửi thành công.");
        return result;
    }

    public Map<String, Object> login(LoginRequest req) {
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

        if (taiKhoanRepository.findBySoDienThoai(soDienThoai).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại này đã được đăng ký trên hệ thống.");
        }
        if (taiKhoanRepository.findByEmail(email).isPresent()) {
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

        String suffix = com.example.Service.MaSinh.duoi(); // không trùng khi nhiều người đăng ký cùng lúc
        String maTaiKhoan = "TK-CTV-" + suffix;
        String maCongTacVien = "CTV-" + suffix;

        // 1. Tạo tài khoản CTV ở trạng thái ChoDuyet
        TaiKhoan taiKhoan = TaiKhoan.builder()
                .maTaiKhoan(maTaiKhoan)
                .tenDangNhap(tenDangNhap)
                .matKhau(req.getMatKhau().trim())
                .email(email)
                .soDienThoai(soDienThoai)
                .loaiTaiKhoan("CongTacVien")
                .trangThai("ChoDuyet")
                .build();
        taiKhoan = taiKhoanRepository.save(taiKhoan);

        // 2. Tạo hồ sơ Cộng tác viên
        CongTacVien ctv = CongTacVien.builder()
                .maCongTacVien(maCongTacVien)
                .taiKhoan(taiKhoan)
                .hoTen(req.getHoTen().trim())
                .ngaySinh(req.getNgaySinh())
                .gioiTinh(req.getGioiTinh() != null ? req.getGioiTinh() : "Khac")
                .noiCuTru(req.getNoiCuTru().trim())
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

        // 4. Liên kết Khu vực
        //    - Ưu tiên khu vực suy ra từ địa chỉ cư trú (tỉnh/thành + phường/xã) mà ứng viên cung cấp
        //    - Vẫn nhận danhSachKhuVucId nếu client (ứng dụng di động) gửi kèm
        Set<Integer> khuVucDaGan = new HashSet<>();
        if (req.getDanhSachKhuVucId() != null) {
            for (Integer kvId : req.getDanhSachKhuVucId()) {
                khuVucRepository.findById(kvId).ifPresent(kv -> ganKhuVucChoCtv(savedCtv, kv, khuVucDaGan));
            }
        }
        KhuVuc khuVucTheoDiaChi = timHoacTaoKhuVuc(req.getPhuongXa(), req.getTinhThanh());
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
        result.put("khuVuc", khuVucTheoDiaChi != null
                ? khuVucTheoDiaChi.getTenKhuVuc() + ", " + khuVucTheoDiaChi.getTinhThanh() : null);
        result.put("thoiGianXetDuyetDuKien", "1-3 ngày làm việc");
        result.put("message", "Hồ sơ ứng tuyển cộng tác viên đã được gửi thành công. Vui lòng chờ phòng HCNS xét duyệt.");
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
     * Xác định khu vực từ địa chỉ: tìm khu vực trùng phường/xã + tỉnh/thành, chưa có thì tạo mới
     * (cùng cách CSKH đang làm khi thêm địa chỉ khách hàng).
     *
     * @return null nếu không có tỉnh/thành
     */
    private KhuVuc timHoacTaoKhuVuc(String phuongXa, String tinhThanh) {
        if (tinhThanh == null || tinhThanh.isBlank()) {
            return null;
        }
        String tinh = tinhThanh.trim();
        String phuong = phuongXa != null ? phuongXa.trim() : "";

        if (!phuong.isEmpty()) {
            Optional<KhuVuc> coSan = khuVucRepository.findFirstByQuanHuyenAndTinhThanh(phuong, tinh)
                    .or(() -> khuVucRepository.findFirstByTenKhuVucAndTinhThanh(phuong, tinh));
            if (coSan.isPresent()) {
                return coSan.get();
            }
        } else {
            List<KhuVuc> trongTinh = khuVucRepository.findByTinhThanh(tinh);
            if (!trongTinh.isEmpty()) {
                return trongTinh.get(0);
            }
        }

        return khuVucRepository.save(KhuVuc.builder()
                .maKhuVuc(maNgauNhien("KV-"))
                .tenKhuVuc(!phuong.isEmpty() ? phuong : tinh)
                .quanHuyen(!phuong.isEmpty() ? phuong : "Chưa xác định")
                .tinhThanh(tinh)
                .trangThai("HoatDong")
                .build());
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
            if (tuKhoa != null && !tuKhoa.isBlank()) {
                String kw = tuKhoa.toLowerCase();
                boolean matchName = dv.getTenDichVu() != null && dv.getTenDichVu().toLowerCase().contains(kw);
                boolean matchDesc = dv.getMoTaChiTiet() != null && dv.getMoTaChiTiet().toLowerCase().contains(kw);
                if (!matchName && !matchDesc) continue;
            }

            // Giá hiện tại nằm ở DichVu.GiaHienTai; chỉ tra lịch sử bảng giá khi cột này chưa có giá
            BigDecimal donGia = dv.getGiaHienTai();
            if (donGia == null || donGia.signum() <= 0) {
                List<BangGiaDichVu> bangGias = bangGiaDichVuRepository.findByDichVu_IdAndTrangThai(dv.getId(), "DangApDung");
                donGia = bangGias.isEmpty() ? BigDecimal.ZERO : bangGias.get(0).getDonGia();
            }

            if (minPrice != null && donGia.compareTo(minPrice) < 0) continue;
            if (maxPrice != null && donGia.compareTo(maxPrice) > 0) continue;

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
            list.add(map);
        }
        return list;
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
        result.put("soNguoiThucHien", dv.getSoNguoiThucHien());
        result.put("giaHienTai", dv.getGiaHienTai());

        // Lịch sử bảng giá
        List<BangGiaDichVu> bangGias = bangGiaDichVuRepository.findByDichVu_IdAndTrangThai(dv.getId(), "DangApDung");
        result.put("bangGias", bangGias);

        // Đánh giá từ khách hàng (tìm qua chiTietDonDat → donDat)
        List<DanhGia> danhGias = danhGiaRepository.findByCongTacVien_Id(dv.getId());
        result.put("soLuongDanhGia", danhGias.size());
        double avgScore = danhGias.stream()
                .mapToInt(d -> (d.getDiemChatLuong() + d.getDiemThaiDo()) / 2)
                .average().orElse(5.0);
        result.put("diemDanhGiaTrungBinh", BigDecimal.valueOf(avgScore).setScale(1, RoundingMode.HALF_UP));
        result.put("danhGias", danhGias);

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

    @Cacheable(value = CacheNames.KHU_VUC, key = "'tatCa'")
    public List<KhuVuc> getAreas() {
        return khuVucRepository.findAll();
    }

    // ==========================================
    // UC-KH04: TÍNH GIÁ, KHUYẾN MẠI, ĐẶT DỊCH VỤ
    // ==========================================
    public PriceCalculationResult calculatePrice(CalculatePriceRequest req) {
        DichVu dv = dichVuRepository.findById(req.getDichVuId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ ID: " + req.getDichVuId()));

        String loaiHinh = (req.getLoaiHinhDat() != null && !req.getLoaiHinhDat().isBlank())
                ? req.getLoaiHinhDat() : dv.getLoaiHinhDat();

        // Tính giá gốc: ưu tiên bangGiaId được truyền vào, rồi đến GiaHienTai trên DichVu
        BigDecimal chiPhiGoc;
        if (req.getBangGiaId() != null) {
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

        BigDecimal soTienGiam = BigDecimal.ZERO;
        String thongDiepKM = null;

        if (req.getCodeKhuyenMai() != null && !req.getCodeKhuyenMai().isBlank()) {
            MaKhuyenMai mkm = maKhuyenMaiRepository.findByCodeKhuyenMaiIgnoreCase(req.getCodeKhuyenMai().trim())
                    .orElse(null);

            if (mkm != null && "HoatDong".equalsIgnoreCase(mkm.getTrangThai())) {
                ChuongTrinhKhuyenMai ctkm = mkm.getChuongTrinhKhuyenMai();
                if (ctkm != null && "DangHoatDong".equalsIgnoreCase(ctkm.getTrangThai())) {
                    if (chiPhiGoc.compareTo(ctkm.getDieuKienToiThieu()) >= 0) {
                        if ("PhanTram".equalsIgnoreCase(ctkm.getLoaiGiam())) {
                            BigDecimal giam = chiPhiGoc.multiply(ctkm.getGiaTriGiam()).divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                            if (ctkm.getSoTienGiamToiDa() != null && giam.compareTo(ctkm.getSoTienGiamToiDa()) > 0) {
                                giam = ctkm.getSoTienGiamToiDa();
                            }
                            soTienGiam = giam;
                        } else {
                            soTienGiam = ctkm.getGiaTriGiam();
                        }
                        thongDiepKM = "Áp dụng thành công mã " + mkm.getCodeKhuyenMai() + ": " + ctkm.getTenChuongTrinh();
                    } else {
                        thongDiepKM = "Đơn hàng chưa đạt giá trị tối thiểu " + ctkm.getDieuKienToiThieu() + " đ để dùng mã này.";
                    }
                }
            }
        }

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
                .build();
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

        DichVu dichVu = dichVuRepository.findById(req.getDichVuId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ ID: " + req.getDichVuId()));

        // Địa chỉ thực hiện
        DiaChiKhachHang diaChi = null;
        if (req.getDiaChiId() != null) {
            diaChi = diaChiKhachHangRepository.findById(req.getDiaChiId()).orElse(null);
        }
        if (diaChi == null && req.getDiaChiChiTiet() != null && !req.getDiaChiChiTiet().isBlank()) {
            KhuVuc kv = req.getKhuVucId() != null ? khuVucRepository.findById(req.getKhuVucId()).orElse(null) : null;
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
            List<DiaChiKhachHang> dcs = diaChiKhachHangRepository.findByKhachHang_Id(khachHang.getId());
            if (!dcs.isEmpty()) diaChi = dcs.get(0);
            else throw new IllegalArgumentException("Vui lòng cung cấp địa chỉ thực hiện dịch vụ.");
        }

        // Tính giá (không còn goiDichVuId)
        CalculatePriceRequest calcReq = new CalculatePriceRequest();
        calcReq.setDichVuId(dichVu.getId());
        calcReq.setLoaiHinhDat(req.getLoaiHinhDat());
        calcReq.setBangGiaId(req.getBangGiaId());
        calcReq.setCodeKhuyenMai(req.getCodeKhuyenMai());
        PriceCalculationResult priceResult = calculatePrice(calcReq);

        // Khuyến mãi nếu có
        MaKhuyenMai mkm = null;
        if (req.getCodeKhuyenMai() != null && !req.getCodeKhuyenMai().isBlank()) {
            mkm = maKhuyenMaiRepository.findByCodeKhuyenMaiIgnoreCase(req.getCodeKhuyenMai().trim()).orElse(null);
        }

        // Bảng giá snapshot tại thời điểm đặt
        BangGiaDichVu bg = req.getBangGiaId() != null
                ? bangGiaDichVuRepository.findById(req.getBangGiaId()).orElse(null)
                : null;

        LocalTime gioKetThuc = req.getGioKetThuc();
        if (gioKetThuc == null && dichVu.getThoiGianThucHien() != null) {
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
                .ngayThucHien(req.getNgayThucHien())
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

        // Tạo ChiTietDonDat cho dịch vụ được đặt
        ChiTietDonDat chiTiet = ChiTietDonDat.builder()
                .donDat(donDat)
                .dichVu(dichVu)
                .bangGia(bg)
                .soLuong(1)
                .donGia(priceResult.getChiPhiGoc())
                .thanhTien(priceResult.getChiPhiGoc())
                .ngayThucHienTrongTuan(req.getNgayThucHienTrongTuan())
                .ghiChu(req.getGhiChu())
                .build();
        chiTietDonDatRepository.save(chiTiet);

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
                .hinhThucThanhToan("ChuyenKhoan")
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
            // Lấy tên dịch vụ đầu tiên trong chiTietList
            String tenDichVu = d.getChiTietList() != null && !d.getChiTietList().isEmpty()
                    ? d.getChiTietList().get(0).getDichVu().getTenDichVu() : "";
            item.put("tenDichVu", tenDichVu);
            item.put("ngayThucHien", d.getNgayThucHien());
            item.put("gioBatDau", d.getGioBatDau());
            item.put("gioKetThuc", d.getGioKetThuc());
            item.put("thanhTien", d.getThanhTien());
            item.put("trangThai", d.getTrangThai());
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
        result.put("id", d.getId());
        result.put("maDonDat", d.getMaDonDat());
        // Lấy danh sách dịch vụ trong đơn
        String tenDichVu = d.getChiTietList() != null && !d.getChiTietList().isEmpty()
                ? d.getChiTietList().get(0).getDichVu().getTenDichVu() : "";
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

        // Đánh giá nếu có
        danhGiaRepository.findByDonDat_Id(d.getId()).ifPresent(dg -> result.put("danhGia", dg));

        return result;
    }

    public Map<String, Object> cancelBooking(Integer donDatId, Integer khachHangId, String lyDo) {
        DonDatDichVu d = donDatDichVuRepository.findByIdForUpdate(donDatId) // khóa đơn: tránh vừa hủy vừa được phân công
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + donDatId));

        if (!"ChoDuyet".equalsIgnoreCase(d.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn khi đang ở trạng thái 'Chờ xác nhận'.");
        }

        String trangThaiCu = d.getTrangThai();
        d.setTrangThai("DaHuy");
        donDatDichVuRepository.save(d);

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

        // Tạo dữ liệu mã QR VietQR mẫu liên kết tài khoản ngân hàng công ty
        String bankCode = "MB"; // Ngân hàng MBBank
        String accountNo = "0988888888"; // STK công ty
        String accountName = "CONG TY DICH VU GIUP VIEC NEATIFY";
        String content = "THANHTOAN " + hd.getMaHoaDon();
        String qrQuickLink = String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                bankCode, accountNo, hd.getTongThanhToan().toPlainString(), content, accountName);

        Map<String, Object> bankInfo = new LinkedHashMap<>();
        bankInfo.put("nganHang", "MBBank (Ngân hàng Quân Đội)");
        bankInfo.put("soTaiKhoan", accountNo);
        bankInfo.put("chuTaiKhoan", accountName);
        bankInfo.put("noiDungChuyenKhoan", content);
        bankInfo.put("soTien", hd.getTongThanhToan());
        bankInfo.put("qrCodeUrl", qrQuickLink);

        res.put("thongTinNganHang", bankInfo);
        return res;
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
        DonDatDichVu donDat = donDatDichVuRepository.findById(req.getDonDatId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn đặt ID: " + req.getDonDatId()));

        if (!"HoanThanh".equalsIgnoreCase(donDat.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá khi đơn dịch vụ đã ở trạng thái 'Hoàn thành'.");
        }

        if (danhGiaRepository.findByDonDat_Id(donDat.getId()).isPresent()) {
            throw new IllegalArgumentException("Đơn dịch vụ này đã được đánh giá trước đó.");
        }

        // Tìm CTV phụ trách đơn hàng
        PhanCongCTV pc = phanCongCTVRepository.findByDonDat_Id(donDat.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên được phân công cho đơn này."));

        CongTacVien ctv = pc.getCongTacVien();

        DanhGia danhGia = DanhGia.builder()
                .maDanhGia(com.example.Service.MaSinh.tao("DG-"))
                .donDat(donDat)
                .khachHang(donDat.getKhachHang())
                .congTacVien(ctv)
                .diemChatLuong(req.getDiemChatLuong())
                .diemThaiDo(req.getDiemThaiDo())
                .nhanXet(req.getNhanXet())
                .ngayDanhGia(LocalDateTime.now())
                .trangThai("DaDuyet")
                .build();
        danhGia = danhGiaRepository.save(danhGia);

        // Cập nhật lại điểm trung bình cho CTV
        List<DanhGia> ctvReviews = danhGiaRepository.findByCongTacVien_Id(ctv.getId());
        double avg = ctvReviews.stream()
                .mapToDouble(d -> (d.getDiemChatLuong() + d.getDiemThaiDo()) / 2.0)
                .average().orElse(5.0);
        ctv.setDiemDanhGia(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        congTacVienRepository.save(ctv);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("danhGiaId", danhGia.getId());
        res.put("maDanhGia", danhGia.getMaDanhGia());
        res.put("diemDanhGiaMoiCuaCTV", ctv.getDiemDanhGia());
        res.put("message", "Gửi đánh giá thành công! Cảm ơn bạn đã phản hồi về dịch vụ.");
        return res;
    }

    public List<DanhGia> getReviewsByService(Integer dichVuId) {
        // Lọc đánh giá theo dịch vụ qua chiTietDonDat
        return danhGiaRepository.findAll().stream()
                .filter(dg -> dg.getDonDat() != null
                        && dg.getDonDat().getChiTietList() != null
                        && dg.getDonDat().getChiTietList().stream()
                            .anyMatch(ct -> dichVuId.equals(ct.getDichVu().getId())))
                .toList();
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
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(taiKhoanId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ThongBaoNguoiDung tbnd : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tbnd.getId());
            m.put("thongBaoId", tbnd.getThongBao().getId());
            m.put("tieuDe", tbnd.getThongBao().getTieuDe());
            m.put("noiDung", tbnd.getThongBao().getNoiDung());
            m.put("thoiGianGui", tbnd.getThongBao().getThoiGianGui());
            m.put("daDoc", tbnd.getDaDoc());
            m.put("thoiGianDoc", tbnd.getThoiGianDoc());
            result.add(m);
        }
        return result;
    }

    public void markNotificationRead(Integer notificationId, Integer taiKhoanId) {
        thongBaoNguoiDungRepository.findById(notificationId).ifPresent(tbnd -> {
            if (tbnd.getTaiKhoan().getId().equals(taiKhoanId)) {
                tbnd.setDaDoc(true);
                tbnd.setThoiGianDoc(LocalDateTime.now());
                thongBaoNguoiDungRepository.save(tbnd);
            }
        });
    }

    public void markAllNotificationsRead(Integer taiKhoanId) {
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(taiKhoanId);
        for (ThongBaoNguoiDung tb : list) {
            tb.setDaDoc(true);
            tb.setThoiGianDoc(LocalDateTime.now());
        }
        thongBaoNguoiDungRepository.saveAll(list);
    }
}
