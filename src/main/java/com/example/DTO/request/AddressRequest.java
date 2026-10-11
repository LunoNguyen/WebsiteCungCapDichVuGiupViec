package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/** Thêm / sửa địa chỉ của khách hàng (app). Dùng đúng các cột sẵn có của bảng DiaChiKhachHang. */
@Data
public class AddressRequest {
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String diaChiChiTiet;

    private Integer khuVucId;
    private BigDecimal dienTichNha;
    private String luuYDacBiet;
    private Boolean laMacDinh;
}
