package com.example.Service;

import com.example.Model.CongTacVien;
import com.example.Model.NhatKyTaiKhoan;
import com.example.Model.TaiKhoan;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.NhatKyTaiKhoanRepository;
import com.example.Repository.TaiKhoanRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/**
 * Phê duyệt hồ sơ ứng viên cộng tác viên: kích hoạt hồ sơ CTV và tài khoản đăng nhập,
 * rồi nhắn tin báo kết quả cho ứng viên.
 * Ứng viên tự đặt mật khẩu khi đăng ký trên web/app nên mặc định giữ nguyên mật khẩu đó.
 * HCNS chỉ cần nhập mật khẩu khi muốn đặt lại, hoặc khi hồ sơ do HCNS tạo tay chưa có tài khoản
 * (bỏ trống thì hệ thống tự sinh và gửi qua tin nhắn).
 */
@Service
public class CapTaiKhoanCtvService {

    // Bỏ các ký tự dễ đọc nhầm (0/O, 1/l/I) vì ứng viên gõ lại từ tin nhắn
    private static final String KY_TU = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CongTacVienRepository congTacVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository;
    private final SmsSender smsSender;
    private final UngVienCtvService ungVienCtvService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CapTaiKhoanCtvService(CongTacVienRepository congTacVienRepository,
                                 TaiKhoanRepository taiKhoanRepository,
                                 NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository,
                                 SmsSender smsSender,
                                 UngVienCtvService ungVienCtvService) {
        this.congTacVienRepository = congTacVienRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.nhatKyTaiKhoanRepository = nhatKyTaiKhoanRepository;
        this.smsSender = smsSender;
        this.ungVienCtvService = ungVienCtvService;
    }

    /** daGuiTin = false khi nhà cung cấp SMS lỗi: hồ sơ vẫn được duyệt, HCNS cần báo ứng viên bằng kênh khác. */
    public record KetQua(String hoTen, String soDienThoai, String tenDangNhap, String noiDungTin, String maTin, boolean daGuiTin) {}

    /** Mật khẩu ngẫu nhiên 8 ký tự, dùng làm gợi ý cho HCNS và làm mật khẩu tạm lúc ứng viên đăng ký. */
    /** Mật khẩu ngẫu nhiên đạt QuyTacMatKhau (luôn có chữ cái và chữ số). */
    public static String taoMatKhauNgauNhien() {
        String mk;
        do {
            StringBuilder sb = new StringBuilder(QuyTacMatKhau.DO_DAI_TOI_THIEU);
            for (int i = 0; i < QuyTacMatKhau.DO_DAI_TOI_THIEU; i++) {
                sb.append(KY_TU.charAt(RANDOM.nextInt(KY_TU.length())));
            }
            mk = sb.toString();
        } while (!QuyTacMatKhau.hopLe(mk, null));
        return mk;
    }

    @Transactional
    public KetQua duyetVaCapTaiKhoan(Integer congTacVienId, String matKhau, String diaChiIP, String thietBi) {
        String mk = matKhau != null ? matKhau.trim() : "";

        CongTacVien ctv = congTacVienRepository.findById(congTacVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ cộng tác viên."));
        if (!mk.isEmpty()) {
            QuyTacMatKhau.batBuoc(mk, ctv.getSoDienThoai());
        }
        if (!"ChoDuyet".equals(ctv.getTrangThai())) {
            throw new IllegalArgumentException("Hồ sơ này không còn ở trạng thái chờ duyệt.");
        }
        // Ứng viên đăng ký qua web/app phải được mời phỏng vấn trước khi duyệt
        if (ctv.getTaiKhoan() != null && !ungVienCtvService.daGuiLichPhongVan(ctv)) {
            throw new IllegalArgumentException("Cần gửi lịch phỏng vấn cho ứng viên " + ctv.getHoTen()
                    + " trước khi phê duyệt.");
        }

        TaiKhoan tk = ctv.getTaiKhoan();
        if (tk == null) {
            // CTV do HCNS tạo thủ công mà chưa có tài khoản: tạo tài khoản mới và liên kết
            if (mk.isEmpty()) {
                mk = taoMatKhauNgauNhien();
            }
            String suffix = com.example.Service.MaSinh.duoi();
            tk = new TaiKhoan();
            tk.setMaTaiKhoan("TK-CTV-" + suffix);
            tk.setTenDangNhap(ctv.getSoDienThoai());
            tk.setEmail(ctv.getSoDienThoai() + "@ctv.local");
            tk.setSoDienThoai(ctv.getSoDienThoai());
            tk.setLoaiTaiKhoan("CongTacVien");
            tk.setMatKhau(passwordEncoder.encode(mk));
            tk.setTrangThai("HoatDong");
            tk = taiKhoanRepository.save(tk);
            ctv.setTaiKhoan(tk);
        } else {
            // CTV đã có tài khoản (đăng ký qua web/app với mật khẩu tự đặt): kích hoạt tài khoản,
            // chỉ đổi mật khẩu khi HCNS chủ động đặt lại
            if (!mk.isEmpty()) {
                tk.setMatKhau(passwordEncoder.encode(mk));
            }
            tk.setTrangThai("HoatDong");
            tk.setNgayCapNhat(java.time.LocalDateTime.now());
            taiKhoanRepository.save(tk);
        }
        boolean coGuiMatKhau = !mk.isEmpty();

        ctv.setTrangThai("HoatDong");
        congTacVienRepository.save(ctv);

        String soDienThoai = ctv.getSoDienThoai() != null ? ctv.getSoDienThoai() : tk.getSoDienThoai();
        String noiDung = coGuiMatKhau
                ? "bTaskee: Ho so cong tac vien cua ban da duoc duyet. "
                        + "Tai khoan: " + tk.getTenDangNhap() + ", mat khau: " + mk + ". "
                        + "Vui long dang nhap ung dung bTaskee va doi mat khau."
                : "bTaskee: Ho so cong tac vien cua ban da duoc duyet va tai khoan da duoc kich hoat. "
                        + "Dang nhap ung dung bTaskee bang tai khoan " + tk.getTenDangNhap()
                        + " va mat khau ban da dat khi dang ky.";
        // Lỗi SMS không được làm hỏng việc duyệt: tài khoản vẫn kích hoạt, HCNS được báo để liên hệ ứng viên
        String maTin;
        boolean daGuiTin;
        try {
            maTin = smsSender.send(soDienThoai, noiDung);
            daGuiTin = true;
        } catch (SmsSendException e) {
            maTin = "loi: " + e.getMessage();
            daGuiTin = false;
        }

        // Nhật ký không chứa mật khẩu
        NhatKyTaiKhoan log = new NhatKyTaiKhoan();
        log.setTaiKhoan(tk);
        // Cột HanhDong chỉ 100 ký tự: không ghi mã tin / lỗi SMS chi tiết (đã có trong log ứng dụng)
        log.setHanhDong("HCNS duyệt CTV, kích hoạt tài khoản"
                + (coGuiMatKhau ? ", đặt lại mật khẩu" : "")
                + (daGuiTin ? ", đã gửi SMS" : ", gửi SMS lỗi"));
        log.setDiaChiIP(diaChiIP);
        log.setThietBi(thietBi);
        log.setKetQua("ThanhCong");
        nhatKyTaiKhoanRepository.save(log);

        return new KetQua(ctv.getHoTen(), soDienThoai, tk.getTenDangNhap(), noiDung, maTin, daGuiTin);
    }
}
