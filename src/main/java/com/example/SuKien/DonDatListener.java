package com.example.SuKien;

import com.example.Model.DonDatDichVu;
import com.example.Model.PhanCongCTV;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entity listener cho DonDatDichVu và PhanCongCTV: so trạng thái / thông tin trước và sau khi lưu
 * rồi phát sự kiện (SuKienDon). Mọi luồng (khách, CTV, CSKH, tác vụ tự hủy) đều đi qua đây nên
 * thông báo luôn được gửi mà không phải sửa từng nơi đổi trạng thái.
 * Lưu ý: câu UPDATE JPQL trực tiếp không đi qua listener.
 */
public class DonDatListener {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm");

    // ---------------- Đơn đặt ----------------

    @PostLoad
    public void taiDon(Object o) {
        if (o instanceof DonDatDichVu d) d.chupTrangThai();
        if (o instanceof PhanCongCTV pc) pc.chupTrangThai();
    }

    @PostPersist
    public void taoMoi(Object o) {
        if (o instanceof DonDatDichVu d) {
            d.chupTrangThai();
            BoPhatSuKien.phat(new SuKienDon.DonMoi(d.getId()));
        } else if (o instanceof PhanCongCTV pc) {
            pc.chupTrangThai();
            BoPhatSuKien.phat(new SuKienDon.PhanCongMoi(pc.getId(), pc.getTrangThai()));
        }
    }

    @PostUpdate
    public void capNhat(Object o) {
        if (o instanceof DonDatDichVu d) {
            DonDatDichVu.AnhChup cu = d.getAnhChup();
            if (cu != null) {
                if (cu.trangThai() != null && !Objects.equals(cu.trangThai(), d.getTrangThai())) {
                    BoPhatSuKien.phat(new SuKienDon.DoiTrangThai(d.getId(), cu.trangThai(), d.getTrangThai()));
                }
                List<String> thayDoi = new ArrayList<>();
                if (cu.ngay() != null && d.getNgayThucHien() != null && !cu.ngay().equals(d.getNgayThucHien())) {
                    thayDoi.add("ngày " + cu.ngay().format(NGAY) + " → " + d.getNgayThucHien().format(NGAY));
                }
                if (cu.gioBatDau() != null && d.getGioBatDau() != null && !cu.gioBatDau().equals(d.getGioBatDau())) {
                    thayDoi.add("giờ bắt đầu " + cu.gioBatDau().format(GIO) + " → " + d.getGioBatDau().format(GIO));
                }
                if (!Objects.equals(cu.gioKetThuc(), d.getGioKetThuc()) && d.getGioKetThuc() != null && cu.gioKetThuc() != null) {
                    thayDoi.add("giờ kết thúc " + cu.gioKetThuc().format(GIO) + " → " + d.getGioKetThuc().format(GIO));
                }
                Integer diaChiMoi = d.getDiaChi() != null ? d.getDiaChi().getId() : null;
                if (cu.diaChiId() != null && diaChiMoi != null && !cu.diaChiId().equals(diaChiMoi)) {
                    thayDoi.add("địa chỉ thực hiện");
                }
                if (!thayDoi.isEmpty() && !"DaHuy".equals(d.getTrangThai())) {
                    BoPhatSuKien.phat(new SuKienDon.DoiThongTin(d.getId(), String.join(", ", thayDoi)));
                }
            }
            d.chupTrangThai();
        } else if (o instanceof PhanCongCTV pc) {
            String cu = pc.getTrangThaiGoc();
            if (cu != null && !Objects.equals(cu, pc.getTrangThai())) {
                BoPhatSuKien.phat(new SuKienDon.PhanCongDoiTrangThai(pc.getId(), cu, pc.getTrangThai()));
            }
            pc.chupTrangThai();
        }
    }
}
