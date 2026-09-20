package com.example.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Giám đốc module controllers - UC-GD01 to UC-GD05
 */
@Controller
@RequestMapping("/giam-doc")
public class GiamDocController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "giam-doc/dashboard";
    }

    // UC-GD02 – Thống kê & Báo cáo
    @GetMapping("/bao-cao")
    public String baoCao() {
        return "giam-doc/bao-cao";
    }

    // UC-GD01 – Xem xét khiếu nại leo thang
    @GetMapping("/khieu-nai")
    public String khieuNai() {
        return "giam-doc/khieu-nai";
    }

    // UC-GD03 – Xem và phê duyệt thay đổi nhân sự
    @GetMapping("/nhan-vien")
    public String nhanVien() {
        return "giam-doc/nhan-vien";
    }

    // UC-GD04 – Tạo & quản lý thông báo
    @GetMapping("/thong-bao")
    public String thongBao() {
        return "giam-doc/thong-bao";
    }

    // UC-GD05 – Quản lý tài khoản hệ thống
    @GetMapping("/tai-khoan")
    public String taiKhoan() {
        return "giam-doc/tai-khoan";
    }
}
