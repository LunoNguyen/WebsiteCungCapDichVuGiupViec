package com.example.Config;

/**
 * Tên các vùng cache lưu trên Redis (key thực tế: "neatify:<tên cache>::<khóa>").
 * Khai báo tập trung để @Cacheable và phần xóa cache luôn dùng chung một tên.
 */
public final class CacheNames {

    /** Danh mục + dịch vụ + giá hiển thị trên website công khai. */
    public static final String DANH_MUC_DICH_VU = "danhMucDichVu";
    /** Kết quả API /v1/services theo từng bộ lọc. */
    public static final String DICH_VU = "dichVu";
    /** Danh sách loại dịch vụ (API /v1/service-types). */
    public static final String LOAI_DICH_VU = "loaiDichVu";
    /** Khu vực phục vụ. */
    public static final String KHU_VUC = "khuVuc";
    /** Chương trình khuyến mãi đang chạy. */
    public static final String KHUYEN_MAI = "khuyenMai";
    /** Đánh giá nổi bật của khách hàng. */
    public static final String DANH_GIA = "danhGia";
    /** Số liệu thống kê, báo cáo. */
    public static final String THONG_KE = "thongKe";

    private CacheNames() {
    }
}
