package com.example.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * CSKH module controllers - UC-CSKH01 to UC-CSKH06
 */
@Controller
@RequestMapping("/cskh")
public class CSKHController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "cskh/dashboard";
    }

    // UC-CSKH01 – Quản lý khách hàng
    @GetMapping("/khach-hang")
    public String khachHang() {
        return "cskh/khach-hang";
    }

    // UC-CSKH02 – Quản lý đơn đặt dịch vụ
    @GetMapping("/don-dat-dich-vu")
    public String donDatDichVu() {
        return "cskh/don-dat-dich-vu";
    }

    // UC-CSKH03 – Quản lý khiếu nại (level 1-2)
    @GetMapping("/khieu-nai")
    public String khieuNai() {
        return "cskh/khieu-nai";
    }

    // UC-CSKH04 – Quản lý đánh giá
    @GetMapping("/danh-gia")
    public String danhGia() {
        return "cskh/danh-gia";
    }

    // UC-CSKH05 – Lịch phân công CTV
    @GetMapping("/lich-phan-cong")
    public String lichPhanCong() {
        return "cskh/lich-phan-cong";
    }

    // UC-CSKH06 – Quản lý thông báo CSKH
    @GetMapping("/thong-bao")
    public String thongBao() {
        return "cskh/thong-bao";
    }
}
