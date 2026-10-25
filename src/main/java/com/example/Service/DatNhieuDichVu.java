package com.example.Service;

import com.example.Model.ChuongTrinhKhuyenMai;
import com.example.Model.DichVu;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.util.List;

/**
 * Quy tắc đặt nhiều dịch vụ trong một đơn (giỏ hàng): một ngày, một giờ bắt đầu, một địa chỉ,
 * các dịch vụ làm nối tiếp nhau. Chỉ dịch vụ theo lần, tối đa 5 dịch vụ, tổng tối đa 8 giờ, xong trước 20:00.
 * Lớp thuần (không chạm CSDL): web, API và test dùng chung câu báo lỗi.
 */
public final class DatNhieuDichVu {

    public static final int SO_DICH_VU_TOI_DA = 5;
    public static final int TONG_PHUT_TOI_DA = 480;
    /** Dịch vụ chưa khai báo thời lượng: tính 2 giờ như khi tạo đơn một dịch vụ. */
    public static final int PHUT_MAC_DINH = 120;
    public static final LocalTime GIO_KET_THUC_MUON_NHAT = LocalTime.of(20, 0);

    private DatNhieuDichVu() {
    }

    public static int phut(DichVu dv) {
        Integer p = dv.getThoiGianThucHien();
        return p != null && p > 0 ? p : PHUT_MAC_DINH;
    }

    public static int tongPhut(List<DichVu> ds) {
        return ds.stream().mapToInt(DatNhieuDichVu::phut).sum();
    }

    /** null nếu danh sách đặt chung được; ngược lại là lỗi đầu tiên. */
    public static String loiDanhSach(List<DichVu> ds) {
        if (ds == null || ds.isEmpty()) return "Vui lòng chọn ít nhất một dịch vụ.";
        if (ds.size() > SO_DICH_VU_TOI_DA) return "Mỗi lần đặt tối đa " + SO_DICH_VU_TOI_DA + " dịch vụ.";
        for (DichVu dv : ds) {
            if ("GoiThang".equalsIgnoreCase(dv.getLoaiHinhDat())) {
                return "Dịch vụ \"" + dv.getTenDichVu() + "\" là gói tháng, vui lòng đặt riêng.";
            }
        }
        for (DichVu dv : ds) {
            if (!"HoatDong".equalsIgnoreCase(dv.getTrangThai())) {
                return "Dịch vụ \"" + dv.getTenDichVu() + "\" hiện không còn nhận đặt.";
            }
        }
        int tong = tongPhut(ds);
        if (tong > TONG_PHUT_TOI_DA) {
            return "Tổng thời lượng tối đa " + thoiLuong(TONG_PHUT_TOI_DA) + " (đang chọn " + thoiLuong(tong) + ").";
        }
        return null;
    }

    public static LocalTime gioKetThuc(LocalTime batDau, int tongPhut) {
        return batDau.plusMinutes(tongPhut);
    }

    /** null nếu các dịch vụ xong trước (hoặc đúng) 20:00. */
    public static String loiGioKetThuc(LocalTime batDau, int tongPhut) {
        int ketThucPhut = batDau.getHour() * 60 + batDau.getMinute() + tongPhut;
        int muonNhat = GIO_KET_THUC_MUON_NHAT.getHour() * 60 + GIO_KET_THUC_MUON_NHAT.getMinute();
        if (ketThucPhut <= muonNhat) return null;
        return "Các dịch vụ phải xong trước " + gio(muonNhat) + " (dự kiến xong lúc " + gio(ketThucPhut)
                + "). Vui lòng chọn giờ sớm hơn.";
    }

    /** Giá một dịch vụ trong đơn (đã tính theo bảng giá lúc bắt đầu làm). */
    public record DongGia(Integer dichVuId, String tenDichVu, BigDecimal donGia) {
    }

    public static BigDecimal tongGoc(List<DongGia> dong) {
        return dong.stream().map(DongGia::donGia).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Số tiền giảm của chương trình trên tổng tiền đơn (phần trăm có trần, hoặc số tiền cố định), không quá tổng. */
    public static BigDecimal soTienGiam(ChuongTrinhKhuyenMai ct, BigDecimal tong) {
        BigDecimal giam;
        if ("PhanTram".equalsIgnoreCase(ct.getLoaiGiam())) {
            giam = tong.multiply(ct.getGiaTriGiam()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (ct.getSoTienGiamToiDa() != null && giam.compareTo(ct.getSoTienGiamToiDa()) > 0) {
                giam = ct.getSoTienGiamToiDa();
            }
        } else {
            giam = ct.getGiaTriGiam().setScale(0, RoundingMode.HALF_UP);
        }
        return giam.min(tong);
    }

    /** "8 giờ", "8 giờ 30 phút", "45 phút". */
    static String thoiLuong(int phut) {
        int g = phut / 60, p = phut % 60;
        if (g == 0) return p + " phút";
        return p == 0 ? g + " giờ" : g + " giờ " + p + " phút";
    }

    /** Phút trong ngày → "HH:mm" (không quay vòng qua nửa đêm, để câu báo lỗi đúng). */
    private static String gio(int phutTrongNgay) {
        return String.format("%02d:%02d", phutTrongNgay / 60, phutTrongNgay % 60);
    }
}
