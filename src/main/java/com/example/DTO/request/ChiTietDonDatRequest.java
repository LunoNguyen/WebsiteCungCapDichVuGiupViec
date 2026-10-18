package com.example.DTO.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class ChiTietDonDatRequest {
    @NotNull(message = "dichVuId không được để trống")
    private Integer dichVuId;
    @NotNull(message = "soLuong không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer soLuong;

    private String ngayThucHienTrongTuan; // vd: "2,4,6", null nếu đặt theo lần
    private String ghiChu;
}