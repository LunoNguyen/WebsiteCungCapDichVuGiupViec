package com.example.DTO.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Đánh giá của khách hàng đã được duyệt hiển thị. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DanhGiaView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenKhachHang;
    private String chuCaiDau;
    private int soSao;
    private String nhanXet;
    private String ngayText;
}
