package com.example.DTO.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AssignmentActionRequest {
    @JsonAlias({"lyDo", "ly_do", "reason"})
    private String lyDoTuChoi;

    @JsonAlias({"ghiChu", "ghi_chu", "note", "ket_qua"})
    private String ketQuaThucHien;

    /** Danh sách đường dẫn / objectName ảnh tài sản đã upload lên MinIO (don-dat/tai-san/...). */
    @JsonAlias({"hinhAnhList", "hinh_anh_list", "images", "anhTaiSan"})
    private List<String> danhSachHinhAnh;

    /** Khu vực / vật dụng tại nơi thực hiện (VD: "Toàn bộ khu vực làm việc", "Phòng khách", "Phòng bếp"). */
    @JsonAlias({"khuVuc", "khu_vuc_vat_dung"})
    private String khuVucVatDung;

    /** Mô tả hiện trạng tài sản trước hoặc sau khi làm việc. */
    @JsonAlias({"moTa", "mo_ta_hien_trang"})
    private String moTaHienTrang;

    /** Ghi chú đặc biệt cho biểu mẫu tài sản (BieuMauTaiSan.GhiChuDacBiet). */
    @JsonAlias({"ghiChuDacBiet", "ghi_chu_tai_san"})
    private String ghiChuTaiSan;

    /** Danh sách chi tiết từng khu vực/ảnh (tùy chọn nếu gửi nhiều khu vực khác nhau). */
    private List<Map<String, String>> chiTietTaiSanList;
}
