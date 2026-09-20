package com.example.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Marketing module controllers - UC-MKT01 to UC-MKT04
 */
@Controller
@RequestMapping("/marketing")
public class MarketingController {

    @GetMapping("/dashboard")
    public String dashboard() {
        return "marketing/dashboard";
    }

    // UC-MKT01 – Quản lý khuyến mãi
    @GetMapping("/khuyen-mai")
    public String khuyenMai() {
        return "marketing/khuyen-mai";
    }

    // UC-MKT02 – Quản lý thông báo (gửi đến KH/CTV)
    @GetMapping("/thong-bao")
    public String thongBao() {
        return "marketing/thong-bao";
    }

    // UC-MKT03 – Phân tích & báo cáo marketing
    @GetMapping("/phan-tich")
    public String phanTich() {
        return "marketing/phan-tich";
    }

    // UC-MKT04 – Quản lý đánh giá (marketing view)
    @GetMapping("/danh-gia")
    public String danhGia() {
        return "marketing/danh-gia";
    }
}
