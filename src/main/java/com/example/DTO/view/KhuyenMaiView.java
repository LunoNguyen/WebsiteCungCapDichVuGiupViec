package com.example.DTO.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Chương trình khuyến mãi đang chạy, hiển thị trên website công khai. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhuyenMaiView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenChuongTrinh;
    private String moTa;
    /** Vd: "Giảm 20%" hoặc "Giảm 50.000đ". */
    private String mucGiamText;
    /** Vd: "Tối đa 100.000đ · Đơn từ 300.000đ"; có thể rỗng. */
    private String dieuKienText;
    /** Vd: "đến hết 31/12/2026". */
    private String hanText;
    /** Mã nhập khi đặt dịch vụ; null nếu chương trình không cần mã. */
    private String code;
}
