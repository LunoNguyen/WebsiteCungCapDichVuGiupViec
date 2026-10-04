package com.example.DTO.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Vị trí GPS do ứng dụng gửi lên. */
@Data
public class LocationRequest {
    @NotNull(message = "Thiếu vĩ độ")
    @DecimalMin(value = "-90", message = "Vĩ độ không hợp lệ")
    @DecimalMax(value = "90", message = "Vĩ độ không hợp lệ")
    private BigDecimal viDo;

    @NotNull(message = "Thiếu kinh độ")
    @DecimalMin(value = "-180", message = "Kinh độ không hợp lệ")
    @DecimalMax(value = "180", message = "Kinh độ không hợp lệ")
    private BigDecimal kinhDo;
}
