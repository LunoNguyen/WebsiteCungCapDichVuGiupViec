package com.example.Service;

import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service tinh toan cac chi so thong ke va tong hop du lieu tu CSDL
 * phuc vu hien thi dong tren Dashboard va cac bao cao quan tri.
 */
@Service
public class ThongKeService {

    private final DonDatDichVuRepository donDatDichVuRepository;
    private final KhachHangRepository khachHangRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final NhanVienRepository nhanVienRepository;
    private final DichVuRepository dichVuRepository;
    private final DanhGiaRepository danhGiaRepository;
    private final KhieuNaiRepository khieuNaiRepository;
    private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
    private final MaCouponRepository maCouponRepository;
    private final ThongBaoRepository thongBaoRepository;

    public ThongKeService(
            DonDatDichVuRepository donDatDichVuRepository,
            KhachHangRepository khachHangRepository,
            CongTacVienRepository congTacVienRepository,
            NhanVienRepository nhanVienRepository,
            DichVuRepository dichVuRepository,
            DanhGiaRepository danhGiaRepository,
            KhieuNaiRepository khieuNaiRepository,
            ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository,
            MaCouponRepository maCouponRepository,
            ThongBaoRepository thongBaoRepository) {
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.khachHangRepository = khachHangRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.nhanVienRepository = nhanVienRepository;
        this.dichVuRepository = dichVuRepository;
        this.danhGiaRepository = danhGiaRepository;
        this.khieuNaiRepository = khieuNaiRepository;
        this.chuongTrinhKhuyenMaiRepository = chuongTrinhKhuyenMaiRepository;
        this.maCouponRepository = maCouponRepository;
        this.thongBaoRepository = thongBaoRepository;
    }

    public long getTongDonHang() {
        return donDatDichVuRepository.count();
    }

    public long getTongKhachHang() {
        return khachHangRepository.count();
    }

    public long getTongCongTacVien() {
        return congTacVienRepository.count();
    }

    public long getTongNhanVien() {
        return nhanVienRepository.count();
    }

    public long getTongDichVu() {
        return dichVuRepository.count();
    }

    public long getTongKhieuNai() {
        return khieuNaiRepository.count();
    }

    public long getKhieuNaiLeoThangCount() {
        return khieuNaiRepository.findAll().stream()
                .filter(kn -> "LeoThang".equalsIgnoreCase(kn.getTrangThai()) || "LeoCao".equalsIgnoreCase(kn.getTrangThai()))
                .count();
    }

    public BigDecimal getTongDoanhThu() {
        return donDatDichVuRepository.findAll().stream()
                .filter(d -> "HoanThanh".equalsIgnoreCase(d.getTrangThai()))
                .map(DonDatDichVu::getThanhTien)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getDoanhThuTrieuDong() {
        BigDecimal tong = getTongDoanhThu();
        return tong.divide(new BigDecimal("1000000"), 1, RoundingMode.HALF_UP);
    }

    public double getDiemDanhGiaTrungBinh() {
        List<DanhGia> danhGias = danhGiaRepository.findAll();
        if (danhGias.isEmpty()) return 4.8;
        double sum = danhGias.stream()
                .mapToInt(d -> d.getDiemChatLuong() != null ? d.getDiemChatLuong() : 5)
                .sum();
        return Math.round((sum / danhGias.size()) * 10.0) / 10.0;
    }

    public List<DonDatDichVu> getDonHangGanDay(int limit) {
        List<DonDatDichVu> list = donDatDichVuRepository.findAll();
        list.sort((a, b) -> Integer.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));
        return list.stream().limit(limit).collect(Collectors.toList());
    }

    public List<KhieuNai> getKhieuNaiGanDay(int limit) {
        List<KhieuNai> list = khieuNaiRepository.findAll();
        list.sort((a, b) -> Integer.compare(b.getId() != null ? b.getId() : 0, a.getId() != null ? a.getId() : 0));
        return list.stream().limit(limit).collect(Collectors.toList());
    }

    public Map<String, Long> getPhanBoTrangThaiDon() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("Hoàn thành", 0L);
        map.put("Đang thực hiện", 0L);
        map.put("Đã xác nhận", 0L);
        map.put("Chờ duyệt", 0L);
        map.put("Đã hủy", 0L);

        for (DonDatDichVu d : donDatDichVuRepository.findAll()) {
            String st = d.getTrangThai();
            if ("HoanThanh".equalsIgnoreCase(st)) map.put("Hoàn thành", map.get("Hoàn thành") + 1);
            else if ("DangThucHien".equalsIgnoreCase(st)) map.put("Đang thực hiện", map.get("Đang thực hiện") + 1);
            else if ("DaXacNhan".equalsIgnoreCase(st)) map.put("Đã xác nhận", map.get("Đã xác nhận") + 1);
            else if ("ChoDuyet".equalsIgnoreCase(st)) map.put("Chờ duyệt", map.get("Chờ duyệt") + 1);
            else if ("DaHuy".equalsIgnoreCase(st)) map.put("Đã hủy", map.get("Đã hủy") + 1);
        }
        return map;
    }

    public Map<String, Long> getTopDichVu() {
        Map<String, Long> map = new LinkedHashMap<>();
        for (DonDatDichVu d : donDatDichVuRepository.findAll()) {
            if (d.getDichVu() != null) {
                String ten = d.getDichVu().getTenDichVu();
                map.put(ten, map.getOrDefault(ten, 0L) + 1);
            }
        }
        return map;
    }

    public Map<Integer, Long> getPhanPhoiDanhGia() {
        Map<Integer, Long> map = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) map.put(i, 0L);
        for (DanhGia dg : danhGiaRepository.findAll()) {
            int diem = dg.getDiemChatLuong() != null ? dg.getDiemChatLuong() : 5;
            map.put(diem, map.getOrDefault(diem, 0L) + 1);
        }
        return map;
    }

    public Map<String, Long> getTrangThaiKhieuNai() {
        Map<String, Long> map = new LinkedHashMap<>();
        map.put("Mới", 0L);
        map.put("Đang xử lý", 0L);
        map.put("Leo thang", 0L);
        map.put("Đã giải quyết", 0L);

        for (KhieuNai kn : khieuNaiRepository.findAll()) {
            String st = kn.getTrangThai();
            if ("Moi".equalsIgnoreCase(st)) map.put("Mới", map.get("Mới") + 1);
            else if ("DangXuLy".equalsIgnoreCase(st)) map.put("Đang xử lý", map.get("Đang xử lý") + 1);
            else if ("LeoThang".equalsIgnoreCase(st) || "LeoCao".equalsIgnoreCase(st)) map.put("Leo thang", map.get("Leo thang") + 1);
            else if ("DaGiaiQuyet".equalsIgnoreCase(st)) map.put("Đã giải quyết", map.get("Đã giải quyết") + 1);
        }
        return map;
    }
}
