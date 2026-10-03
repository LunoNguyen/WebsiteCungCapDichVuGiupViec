package com.example.DTO.view;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Danh mục (loại dịch vụ) kèm các dịch vụ đang hoạt động, dùng cho website công khai. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DanhMucView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String maLoaiDichVu;
    private String tenLoaiDichVu;
    private String moTa;
    /** Đường dẫn ảnh hiển thị được ngay (ảnh từ MinIO hoặc ảnh mặc định). */
    private String hinhAnhUrl;
    private int soDichVu;
    /** Vd: "từ 192.000đ"; rỗng nếu chưa có giá. */
    private String giaTuText;
    @Builder.Default
    private List<DichVuView> dichVuTheoLan = new ArrayList<>();
    @Builder.Default
    private List<DichVuView> dichVuGoiThang = new ArrayList<>();
    /** Vài dịch vụ tiêu biểu để hiện trong menu và thẻ danh mục. */
    @Builder.Default
    private List<String> tieuBieu = new ArrayList<>();
}
