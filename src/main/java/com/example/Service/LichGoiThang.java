package com.example.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lịch buổi làm của gói tháng: khách chọn các thứ trong tuần (vd "3,5,7,CN") và ngày bắt đầu,
 * hệ thống lấy lần lượt các ngày khớp thứ, trong vòng 1 tháng kể từ ngày bắt đầu, cho đủ số buổi của gói.
 *
 * Không lưu lịch vào CSDL: chỉ lưu ngày bắt đầu (DonDatDichVu.NgayThucHien) và các thứ
 * (ChiTietDonDat.NgayThucHienTrongTuan); lịch tính lại được bằng cùng một thuật toán.
 * App Flutter dùng đúng thuật toán này (lib/services/lich_goi_thang.dart).
 *
 * Mã thứ: "2".."7" là Thứ 2..Thứ 7, "CN" là Chủ nhật.
 */
public final class LichGoiThang {

    private static final Pattern SO_BUOI_TRONG_TEN = Pattern.compile("(\\d+)\\s*(buổi|ngày|lần)\\s*/\\s*tháng", Pattern.CASE_INSENSITIVE);

    private LichGoiThang() {
    }

    /** Số buổi của gói: lấy cột SoBuoi, chưa có thì đọc từ tên ("Gói ... 13 buổi/tháng"). */
    public static int soBuoi(Integer soBuoiCot, String tenDichVu) {
        if (soBuoiCot != null && soBuoiCot > 0) return soBuoiCot;
        if (tenDichVu != null) {
            Matcher m = SO_BUOI_TRONG_TEN.matcher(tenDichVu);
            if (m.find()) return Integer.parseInt(m.group(1));
        }
        return 4;
    }

    /** "3, 5,cn" → {TUESDAY, THURSDAY, SUNDAY}. Mã lạ bị bỏ qua. */
    public static Set<DayOfWeek> docThu(String chuoi) {
        Set<DayOfWeek> thu = EnumSet.noneOf(DayOfWeek.class);
        if (chuoi == null) return thu;
        for (String ma : chuoi.split(",")) {
            String m = ma.trim().toUpperCase();
            if (m.isEmpty()) continue;
            if (m.equals("CN") || m.equals("8") || m.equals("1")) {
                thu.add(DayOfWeek.SUNDAY);
            } else if (m.matches("[2-7]")) {
                thu.add(DayOfWeek.of(Integer.parseInt(m) - 1));
            }
        }
        return thu;
    }

    /** {TUESDAY, SUNDAY} → "3,CN" (thứ tự Thứ 2 → Chủ nhật). */
    public static String vietThu(Set<DayOfWeek> thu) {
        List<String> ma = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (thu.contains(d)) ma.add(d == DayOfWeek.SUNDAY ? "CN" : String.valueOf(d.getValue() + 1));
        }
        return String.join(",", ma);
    }

    /** "3,5,7,CN" → "T3, T5, T7, CN". */
    public static String nhanThu(String chuoi) {
        List<String> nhan = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            if (docThu(chuoi).contains(d)) nhan.add(d == DayOfWeek.SUNDAY ? "CN" : "T" + (d.getValue() + 1));
        }
        return String.join(", ", nhan);
    }

    /** Các ngày khớp thứ đã chọn, từ ngày bắt đầu tới trước cùng ngày tháng sau, tối đa soBuoi ngày. */
    public static List<LocalDate> tinhLich(LocalDate batDau, Set<DayOfWeek> thu, int soBuoi) {
        List<LocalDate> ngays = new ArrayList<>();
        if (batDau == null || thu == null || thu.isEmpty()) return ngays;
        LocalDate ketThuc = batDau.plusMonths(1);
        for (LocalDate d = batDau; d.isBefore(ketThuc) && ngays.size() < soBuoi; d = d.plusDays(1)) {
            if (thu.contains(d.getDayOfWeek())) ngays.add(d);
        }
        return ngays;
    }

    /** Như tinhLich nhưng báo lỗi khi các thứ đã chọn không đủ số buổi trong 1 tháng. */
    public static List<LocalDate> tinhLichDu(LocalDate batDau, String chuoiThu, int soBuoi) {
        Set<DayOfWeek> thu = docThu(chuoiThu);
        if (thu.isEmpty()) {
            throw new IllegalArgumentException("Gói tháng cần chọn các thứ trong tuần sẽ làm.");
        }
        List<LocalDate> ngays = tinhLich(batDau, thu, soBuoi);
        if (ngays.size() < soBuoi) {
            int canThem = (int) Math.ceil((soBuoi - ngays.size()) / 4.0);
            throw new IllegalArgumentException("Các thứ đã chọn chỉ có " + ngays.size() + " buổi trong 1 tháng, gói cần "
                    + soBuoi + " buổi. Hãy chọn thêm ít nhất " + canThem + " thứ.");
        }
        return ngays;
    }
}
