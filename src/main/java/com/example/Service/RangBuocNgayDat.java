package com.example.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ràng buộc ngày giờ khi đặt dịch vụ (dùng chung cho website và API ứng dụng):
 * - không chọn ngày quá khứ, giờ bắt đầu sau hiện tại ít nhất 1 giờ;
 * - đặt theo lần: ngày làm trong vòng 1 tháng tới;
 * - gói tháng (kéo dài 1 tháng): ngày bắt đầu gói trong vòng 3 tháng tới.
 */
public final class RangBuocNgayDat {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RangBuocNgayDat() {
    }

    /** Ngày muộn nhất được chọn (với gói tháng: ngày bắt đầu gói). */
    public static LocalDate ngayToiDa(String loaiHinhDat, LocalDate homNay) {
        return "GoiThang".equalsIgnoreCase(loaiHinhDat) ? homNay.plusMonths(3) : homNay.plusMonths(1);
    }

    /** null nếu ngày giờ hợp lệ; ngược lại là thông báo lỗi cho khách. */
    public static String loiNgayDat(String loaiHinhDat, LocalDate ngay, LocalTime gio, LocalDateTime bayGio) {
        if (ngay == null || gio == null) return "Vui lòng chọn ngày và giờ bắt đầu.";
        if (ngay.isBefore(bayGio.toLocalDate())) return "Ngày thực hiện không được ở quá khứ.";
        LocalDate toiDa = ngayToiDa(loaiHinhDat, bayGio.toLocalDate());
        if (ngay.isAfter(toiDa)) {
            return "GoiThang".equalsIgnoreCase(loaiHinhDat)
                    ? "Gói tháng chỉ được bắt đầu trong vòng 3 tháng tới (đến " + toiDa.format(NGAY) + ")."
                    : "Đặt theo lần chỉ được chọn ngày trong vòng 1 tháng tới (đến " + toiDa.format(NGAY) + ").";
        }
        if (LocalDateTime.of(ngay, gio).isBefore(bayGio.plusHours(1))) {
            return "Giờ bắt đầu cần sau thời điểm hiện tại ít nhất 1 giờ.";
        }
        return null;
    }
}
