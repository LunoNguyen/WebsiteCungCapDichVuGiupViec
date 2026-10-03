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
 * Phê duyệt hồ sơ ứng viên cộng tác viên: HCNS đặt mật khẩu, hệ thống kích hoạt
 * tài khoản và gửi tin nhắn chứa tên đăng nhập, mật khẩu cho ứng viên.
 * Ứng viên đăng ký trên web không tự đặt mật khẩu.
 */
@Service
public class CapTaiKhoanCtvService {

    public static final int DO_DAI_TOI_THIEU = 6;
    // Bỏ các ký tự dễ đọc nhầm (0/O, 1/l/I) vì ứng viên gõ lại từ tin nhắn
    private static final String KY_TU = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CongTacVienRepository congTacVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;
    private final NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository;
    private final SmsSender smsSender;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CapTaiKhoanCtvService(CongTacVienRepository congTacVienRepository,
                                 TaiKhoanRepository taiKhoanRepository,
                                 NhatKyTaiKhoanRepository nhatKyTaiKhoanRepository,
                                 SmsSender smsSender) {
        this.congTacVienRepository = congTacVienRepository;
        this.taiKhoanRepository = taiKhoanRepository;
        this.nhatKyTaiKhoanRepository = nhatKyTaiKhoanRepository;
        this.smsSender = smsSender;
    }

    public record KetQua(String hoTen, String soDienThoai, String tenDangNhap, String noiDungTin, String maTin) {}

    /** Mật khẩu ngẫu nhiên 8 ký tự, dùng làm gợi ý cho HCNS và làm mật khẩu tạm lúc ứng viên đăng ký. */
    public static String taoMatKhauNgauNhien() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(KY_TU.charAt(RANDOM.nextInt(KY_TU.length())));
        }
        return sb.toString();
    }

    @Transactional
    public KetQua duyetVaCapTaiKhoan(Integer congTacVienId, String matKhau, String diaChiIP, String thietBi) {
        String mk = matKhau != null ? matKhau.trim() : "";
        if (mk.length() < DO_DAI_TOI_THIEU) {
            throw new IllegalArgumentException("Mật khẩu cần ít nhất " + DO_DAI_TOI_THIEU + " ký tự.");
        }

        CongTacVien ctv = congTacVienRepository.findById(congTacVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ cộng tác viên."));
        if (!"ChoDuyet".equals(ctv.getTrangThai())) {
            throw new IllegalArgumentException("Hồ sơ này không còn ở trạng thái chờ duyệt.");
        }

        TaiKhoan tk = ctv.getTaiKhoan();
        if (tk == null) {
            // Trường hợp CTV do HCNS tạo thủ công mà chưa có tài khoản:
            // tự động tạo tài khoản mới và liên kết
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
            // Trường hợp CTV đã có tài khoản (đăng ký qua web): cập nhật mật khẩu mới do HCNS đặt
            tk.setMatKhau(passwordEncoder.encode(mk));
            tk.setTrangThai("HoatDong");
            taiKhoanRepository.save(tk);
        }

        ctv.setTrangThai("HoatDong");
        congTacVienRepository.save(ctv);

        String soDienThoai = ctv.getSoDienThoai() != null ? ctv.getSoDienThoai() : tk.getSoDienThoai();
        String noiDung = "Neatify: Ho so cong tac vien cua ban da duoc duyet. "
                + "Tai khoan: " + tk.getTenDangNhap() + ", mat khau: " + mk + ". "
                + "Vui long dang nhap ung dung Neatify va doi mat khau.";
        String maTin = smsSender.send(soDienThoai, noiDung);

        // Nhật ký không chứa mật khẩu
        NhatKyTaiKhoan log = new NhatKyTaiKhoan();
        log.setTaiKhoan(tk);
        log.setHanhDong("HCNS duyệt hồ sơ CTV, đặt mật khẩu và gửi SMS (" + maTin + ")");
        log.setDiaChiIP(diaChiIP);
        log.setThietBi(thietBi);
        log.setKetQua("ThanhCong");
        nhatKyTaiKhoanRepository.save(log);

        return new KetQua(ctv.getHoTen(), soDienThoai, tk.getTenDangNhap(), noiDung, maTin);
    }
}
