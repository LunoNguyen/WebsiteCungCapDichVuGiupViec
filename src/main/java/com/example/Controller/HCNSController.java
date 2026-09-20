package com.example.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * HCNS-Kế toán module controllers - UC-HCNS01 to UC-HCNS05
 */
@Controller
@RequestMapping("/hcns")
public class HCNSController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "hcns/dashboard";
    }

    // UC-HCNS01 – Quản lý nhân viên
    @GetMapping("/nhan-vien")
    public String nhanVien() {
        return "hcns/nhan-vien";
    }

    // UC-HCNS02 – Quản lý CTV
    @GetMapping("/cong-tac-vien")
    public String congTacVien() {
        return "hcns/cong-tac-vien";
    }

    // UC-HCNS03 – Quản lý danh mục dịch vụ
    @GetMapping("/danh-muc-dich-vu")
    public String danhMucDichVu() {
        return "hcns/danh-muc-dich-vu";
    }

    // UC-HCNS04 – Quản lý dịch vụ và bảng giá
    @GetMapping("/dich-vu-bang-gia")
    public String dichVuBangGia() {
        return "hcns/dich-vu-bang-gia";
    }

    // UC-HCNS05 – Quản lý tài khoản
    @GetMapping("/tai-khoan")
    public String taiKhoan() {
        return "hcns/tai-khoan";
    }
}
