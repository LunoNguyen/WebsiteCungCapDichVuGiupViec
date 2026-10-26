package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.DonDatDichVu;
import com.example.Model.PhanCongCTV;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phân công theo dịch vụ của đơn nhiều dịch vụ: mỗi dòng ChiTietDonDat tối đa một phân công hiệu lực.
 * Phân công không gắn dòng (chiTietDonDat = null) là phân công cả đơn – đơn một dịch vụ giữ nguyên cách cũ.
 * Lớp thuần (không chạm CSDL) để kiểm thử.
 */
public final class PhanCongTheoDichVu {

    private PhanCongTheoDichVu() {
    }

    /** Phân công còn tính: chưa bị từ chối / hủy. */
    public static boolean hieuLuc(PhanCongCTV pc) {
        return !"TuChoi".equalsIgnoreCase(pc.getTrangThai()) && !"DaHuy".equalsIgnoreCase(pc.getTrangThai());
    }

    /** Id các dòng chưa có phân công hiệu lực; đã có phân công cả đơn thì rỗng. */
    public static Set<Integer> chiTietConTrong(List<ChiTietDonDat> cts, List<PhanCongCTV> pcs) {
        Set<Integer> conTrong = new LinkedHashSet<>();
        for (ChiTietDonDat ct : cts) conTrong.add(ct.getId());
        for (PhanCongCTV pc : pcs) {
            if (!hieuLuc(pc)) continue;
            if (pc.getChiTietDonDat() == null) return new LinkedHashSet<>();
            conTrong.remove(pc.getChiTietDonDat().getId());
        }
        return conTrong;
    }

    /** Mọi dòng đã có người và có ít nhất một CTV đã xác nhận. */
    public static boolean duNguoi(List<ChiTietDonDat> cts, List<PhanCongCTV> pcs) {
        boolean daXacNhan = pcs.stream().anyMatch(pc ->
                "DaXacNhan".equalsIgnoreCase(pc.getTrangThai()) || "HoanThanh".equalsIgnoreCase(pc.getTrangThai()));
        return daXacNhan && chiTietConTrong(cts, pcs).isEmpty();
    }

    /**
     * null nếu CTV nhận được các dòng [muonNhan].
     *
     * @param chiTietCuaDon    id mọi dòng của đơn
     * @param conTrong         id các dòng chưa có người ([chiTietConTrong])
     * @param dichVuCtv        id dịch vụ CTV đăng ký kỹ năng (đang hoạt động)
     * @param dichVuCuaChiTiet id dòng → id dịch vụ
     */
    public static String loiNhan(Set<Integer> chiTietCuaDon, Set<Integer> conTrong, Set<Integer> dichVuCtv,
                                 Map<Integer, Integer> dichVuCuaChiTiet, List<Integer> muonNhan) {
        if (muonNhan == null || muonNhan.isEmpty()) return "Vui lòng chọn dịch vụ muốn nhận.";
        for (Integer id : muonNhan) {
            if (!chiTietCuaDon.contains(id)) return "Dịch vụ không thuộc đơn này.";
        }
        for (Integer id : muonNhan) {
            if (!conTrong.contains(id)) return "Dịch vụ bạn chọn vừa có người nhận. Vui lòng tải lại đơn.";
        }
        for (Integer id : muonNhan) {
            if (!dichVuCtv.contains(dichVuCuaChiTiet.get(id))) return "Bạn chưa đăng ký kỹ năng cho dịch vụ đã chọn.";
        }
        return null;
    }

    /** Dòng mặc định khi CTV bấm nhận: mọi dòng còn trống mà CTV có kỹ năng, sắp tăng dần. */
    public static List<Integer> chonMacDinh(Set<Integer> conTrong, Set<Integer> dichVuCtv, Map<Integer, Integer> dichVuCuaChiTiet) {
        List<Integer> ds = new ArrayList<>();
        for (Integer id : conTrong) {
            if (dichVuCtv.contains(dichVuCuaChiTiet.get(id))) ds.add(id);
        }
        ds.sort(null);
        return ds;
    }

    /** Mọi phân công còn hiệu lực của đơn đã hoàn thành (đơn nhiều dịch vụ: mọi CTV đã làm xong phần mình). */
    public static boolean moiPhanCongXong(List<PhanCongCTV> pcs) {
        boolean co = false;
        for (PhanCongCTV pc : pcs) {
            if (!hieuLuc(pc)) continue;
            co = true;
            if (!"HoanThanh".equalsIgnoreCase(pc.getTrangThai())) return false;
        }
        return co;
    }

    /**
     * Tên dịch vụ CTV làm trong đơn: có phân công cả đơn thì mọi dịch vụ của đơn; ngược lại chỉ các dòng
     * CTV đã nhận (đơn nhiều dịch vụ). Dạng "A", "A + B", "A + 2 dịch vụ khác".
     */
    public static String tenDichVuCuaCtv(DonDatDichVu don, List<PhanCongCTV> pcsCuaCtv) {
        List<String> ten = new ArrayList<>();
        for (PhanCongCTV pc : pcsCuaCtv) {
            if (pc.getChiTietDonDat() == null) return DatNhieuDichVu.tenDichVuCuaDon(don, "");
            if (pc.getChiTietDonDat().getDichVu() != null) ten.add(pc.getChiTietDonDat().getDichVu().getTenDichVu());
        }
        return ten.isEmpty() ? DatNhieuDichVu.tenDichVuCuaDon(don, "") : DatNhieuDichVu.tomTatDichVu(ten);
    }

    /**
     * Phần tiền đơn (sau khuyến mãi) ứng với các dịch vụ CTV làm: có phân công cả đơn thì cả đơn; ngược lại
     * chia thanhTien của đơn theo tỉ lệ giá các dòng CTV nhận (giá các dòng đều 0 thì chia đều theo số dòng).
     */
    public static BigDecimal tienCuaCtv(DonDatDichVu don, List<PhanCongCTV> pcsCuaCtv) {
        BigDecimal tienDon = don == null || don.getThanhTien() == null ? BigDecimal.ZERO : don.getThanhTien();
        Set<Integer> dong = new LinkedHashSet<>();
        for (PhanCongCTV pc : pcsCuaCtv) {
            if (!hieuLuc(pc)) continue;
            if (pc.getChiTietDonDat() == null) return tienDon;
            dong.add(pc.getChiTietDonDat().getId());
        }
        return tienTheoDong(don, dong);
    }

    /** Phần tiền đơn (sau khuyến mãi) ứng với các dòng chi tiết [dong], chia theo tỉ lệ giá dòng. */
    public static BigDecimal tienTheoDong(DonDatDichVu don, Set<Integer> dong) {
        BigDecimal tienDon = don == null || don.getThanhTien() == null ? BigDecimal.ZERO : don.getThanhTien();
        List<ChiTietDonDat> cts = don == null || don.getChiTietList() == null ? List.of() : don.getChiTietList();
        if (dong.isEmpty() || cts.isEmpty()) return BigDecimal.ZERO;
        BigDecimal tong = BigDecimal.ZERO, cuaCtv = BigDecimal.ZERO;
        int soDongCuaCtv = 0;
        for (ChiTietDonDat ct : cts) {
            BigDecimal gia = ct.getThanhTien() == null ? BigDecimal.ZERO : ct.getThanhTien();
            tong = tong.add(gia);
            if (dong.contains(ct.getId())) { cuaCtv = cuaCtv.add(gia); soDongCuaCtv++; }
        }
        if (soDongCuaCtv == cts.size()) return tienDon;
        if (tong.signum() == 0) {
            return tienDon.multiply(BigDecimal.valueOf(soDongCuaCtv)).divide(BigDecimal.valueOf(cts.size()), 0, RoundingMode.HALF_UP);
        }
        return tienDon.multiply(cuaCtv).divide(tong, 0, RoundingMode.HALF_UP);
    }
}
