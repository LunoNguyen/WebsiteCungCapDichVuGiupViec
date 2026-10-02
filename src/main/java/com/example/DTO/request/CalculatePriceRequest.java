package com.example.DTO.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CalculatePriceRequest {
    // Danh sách dịch vụ cần tính giá
    @Valid
    private List<ChiTietDonDatRequest> chiTiet;

    // Lối tắt cho 1 dịch vụ (client cũ)
    private Integer dichVuId;
    private Integer bangGiaId;

    private String loaiHinhDat; // TheoLan | GoiThang
    private String codeKhuyenMai;

    @JsonIgnore
    public List<ChiTietDonDatRequest> getDanhSachDichVu() {
        if (chiTiet != null && !chiTiet.isEmpty()) return chiTiet;
        List<ChiTietDonDatRequest> ds = new ArrayList<>();
        if (dichVuId != null) {
            ChiTietDonDatRequest ct = new ChiTietDonDatRequest();
            ct.setDichVuId(dichVuId);
            ct.setSoLuong(1);
            ds.add(ct);
        }
        return ds;
    }
}