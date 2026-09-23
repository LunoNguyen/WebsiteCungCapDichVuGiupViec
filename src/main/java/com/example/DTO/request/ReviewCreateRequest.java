package com.example.DTO.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewCreateRequest {
    @NotNull(message = "donDatId không được để trống")
    private Integer donDatId;

    private Integer khachHangId;

    @NotNull(message = "Điểm chất lượng không được để trống")
    @Min(value = 1, message = "Điểm chất lượng tối thiểu là 1")
    @Max(value = 5, message = "Điểm chất lượng tối đa là 5")
    private Integer diemChatLuong;

    @NotNull(message = "Điểm thái độ không được để trống")
    @Min(value = 1, message = "Điểm thái độ tối thiểu là 1")
    @Max(value = 5, message = "Điểm thái độ tối đa là 5")
    private Integer diemThaiDo;

    private String nhanXet;
}
