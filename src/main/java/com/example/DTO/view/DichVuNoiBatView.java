package com.example.DTO.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/** Thẻ "dịch vụ phổ biến" ở trang chủ. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DichVuNoiBatView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String ten;
    private String hinhAnhUrl;
    private String giaTuText;
    private Integer danhMucId;
}
