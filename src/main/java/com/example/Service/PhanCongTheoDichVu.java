package com.example.Service;

import com.example.Model.ChiTietDonDat;
import com.example.Model.PhanCongCTV;

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
}
