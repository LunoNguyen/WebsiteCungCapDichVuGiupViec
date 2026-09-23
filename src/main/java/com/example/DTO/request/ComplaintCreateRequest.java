package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ComplaintCreateRequest {
    @NotNull(message = "donDatId không được để trống")
    private Integer donDatId;

    private Integer khachHangId;

    @NotBlank(message = "Loại vấn đề không được để trống")
    private String loaiVanDe;

    @NotBlank(message = "Nội dung khiếu nại không được để trống")
    private String noiDung;

    // Danh sách đường dẫn file/ảnh bằng chứng (lưu trên MinIO)
    private List<String> danhSachFileDinhKem;
}
