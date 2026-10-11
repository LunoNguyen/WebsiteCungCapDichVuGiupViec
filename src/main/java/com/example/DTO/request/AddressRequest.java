package com.example.DTO.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Thêm / sửa địa chỉ của khách hàng. Dùng đúng các cột sẵn có của bảng DiaChiKhachHang.
 *
 * Cách mới (web, app): soNhaTenDuong + maTinh + maPhuong (provinces.open-api.vn v2); khu vực tự suy ra
 * từ phường, chỉ gửi khuVucId khi phường nằm trên nhiều khu vực.
 * Cách cũ (app chưa cập nhật): diaChiChiTiet + khuVucId.
 */
@Data
public class AddressRequest {
    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String diaChiChiTiet;

    @Size(max = 200, message = "Số nhà, tên đường tối đa 200 ký tự")
    private String soNhaTenDuong;
    private Integer maTinh;
    private Integer maPhuong;

    private Integer khuVucId;
    private BigDecimal dienTichNha;
    private String luuYDacBiet;
    private Boolean laMacDinh;
}
