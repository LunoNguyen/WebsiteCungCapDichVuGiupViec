package com.example.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Quy tắc độ phức tạp cho mật khẩu MỚI (đăng ký, đổi, đặt lại, nhân viên tạo tài khoản).
 * Đăng nhập không kiểm tra quy tắc này: mật khẩu cũ vẫn dùng được.
 * Web (static/js/quy-tac-mat-khau.js) và app Flutter (lib/core/password_rules.dart) dùng cùng tiêu chí và câu báo lỗi.
 */
public final class QuyTacMatKhau {

    public static final int DO_DAI_TOI_THIEU = 8;

    public static final String LOI_DO_DAI = "Mật khẩu cần ít nhất " + DO_DAI_TOI_THIEU + " ký tự.";
    public static final String LOI_CHU_CAI = "Mật khẩu cần có ít nhất một chữ cái.";
    public static final String LOI_CHU_SO = "Mật khẩu cần có ít nhất một chữ số.";
    public static final String LOI_KHOANG_TRANG = "Mật khẩu không được chứa khoảng trắng.";
    public static final String LOI_SO_DIEN_THOAI = "Mật khẩu không được chứa số điện thoại.";

    private QuyTacMatKhau() {
    }

    /** Danh sách tiêu chí chưa đạt (rỗng khi hợp lệ). soDienThoai null/rỗng thì bỏ qua tiêu chí số điện thoại. */
    public static List<String> kiemTra(String matKhau, String soDienThoai) {
        String mk = matKhau != null ? matKhau : "";
        List<String> loi = new ArrayList<>();
        if (mk.length() < DO_DAI_TOI_THIEU) {
            loi.add(LOI_DO_DAI);
        }
        if (mk.codePoints().noneMatch(Character::isLetter)) {
            loi.add(LOI_CHU_CAI);
        }
        if (mk.chars().noneMatch(c -> c >= '0' && c <= '9')) {
            loi.add(LOI_CHU_SO);
        }
        if (mk.codePoints().anyMatch(Character::isWhitespace)) {
            loi.add(LOI_KHOANG_TRANG);
        }
        if (chuaSoDienThoai(mk, soDienThoai)) {
            loi.add(LOI_SO_DIEN_THOAI);
        }
        return loi;
    }

    public static boolean hopLe(String matKhau, String soDienThoai) {
        return kiemTra(matKhau, soDienThoai).isEmpty();
    }

    /** Ném IllegalArgumentException với lỗi đầu tiên khi mật khẩu chưa đạt quy tắc. */
    public static void batBuoc(String matKhau, String soDienThoai) {
        List<String> loi = kiemTra(matKhau, soDienThoai);
        if (!loi.isEmpty()) {
            throw new IllegalArgumentException(loi.get(0));
        }
    }

    /** Chứa số điện thoại (chỉ lấy chữ số), kể cả dạng bỏ số 0 đầu như 912345678 trong 0912345678. */
    static boolean chuaSoDienThoai(String matKhau, String soDienThoai) {
        if (soDienThoai == null) {
            return false;
        }
        String sdt = soDienThoai.replaceAll("\\D", "");
        if (sdt.startsWith("84") && sdt.length() >= 11) {
            sdt = "0" + sdt.substring(2);
        }
        if (sdt.length() < 6) {
            return false;
        }
        if (matKhau.contains(sdt)) {
            return true;
        }
        return sdt.startsWith("0") && matKhau.contains(sdt.substring(1));
    }
}
