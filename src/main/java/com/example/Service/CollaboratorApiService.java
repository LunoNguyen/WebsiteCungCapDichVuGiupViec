package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CollaboratorApiService {

    @org.springframework.beans.factory.annotation.Autowired
    private PhanCongService phanCongService;

    private final PhanCongCTVRepository phanCongCTVRepository;
    private final DonDatDichVuRepository donDatDichVuRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private ViTriCtvStore viTriCtvStore;

    public CollaboratorApiService(
            PhanCongCTVRepository phanCongCTVRepository,
            DonDatDichVuRepository donDatDichVuRepository,
            LichLamViecRepository lichLamViecRepository,
            CongTacVienRepository congTacVienRepository,
            LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository,
            ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
            TaiKhoanRepository taiKhoanRepository) {
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.lichSuTrangThaiDonRepository = lichSuTrangThaiDonRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }

    // ==========================================
    // UC-CTV01: QUẢN LÝ ĐƠN ĐƯỢC PHÂN CÔNG
    // ==========================================
    public List<Map<String, Object>> getAssignments(Integer congTacVienId, String trangThai) {
        List<PhanCongCTV> list;
        if (trangThai != null && !trangThai.isBlank()) {
            list = phanCongCTVRepository.findByCongTacVien_IdAndTrangThaiOrderByThoiGianPhanCongDesc(congTacVienId, trangThai);
        } else {
            list = phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(congTacVienId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PhanCongCTV pc : list) {
            DonDatDichVu don = pc.getDonDat();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("phanCongId", pc.getId());
            item.put("maPhanCong", pc.getMaPhanCong());
            item.put("trangThaiPhanCong", pc.getTrangThai());
            item.put("thoiGianPhanCong", pc.getThoiGianPhanCong());
            item.put("donDatId", don.getId());
            item.put("maDonDat", don.getMaDonDat());
            String tenDichVu = (don.getChiTietList() != null && !don.getChiTietList().isEmpty())
                    ? don.getChiTietList().get(0).getDichVu().getTenDichVu() : "";
            item.put("tenDichVu", tenDichVu);
            item.put("ngayThucHien", don.getNgayThucHien());
            item.put("gioBatDau", don.getGioBatDau());
            item.put("gioKetThuc", don.getGioKetThuc());
            item.put("diaChi", don.getDiaChi() != null ? don.getDiaChi().getDiaChiChiTiet() : "");
            item.put("khachHangTen", don.getKhachHang().getHoTen());
            item.put("khachHangPhone", don.getKhachHang().getSoDienThoai());
            item.put("yeuCauDacBiet", don.getYeuCauDacBiet());
            item.put("thanhTien", don.getThanhTien());
            result.add(item);
        }
        return result;
    }

    /** CTV gửi vị trí hiện tại (GPS) từ ứng dụng. Lưu trên Redis, không thêm cột vào CSDL. */
    @Transactional(readOnly = true)
    public Map<String, Object> updateLocation(Integer congTacVienId, java.math.BigDecimal viDo, java.math.BigDecimal kinhDo) {
        if (!congTacVienRepository.existsById(congTacVienId)) {
            throw new IllegalArgumentException("Không tìm thấy cộng tác viên.");
        }
        ViTriCtvStore.ViTri vt = viTriCtvStore.update(congTacVienId, viDo, kinhDo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("congTacVienId", congTacVienId);
        result.put("viDo", vt.viDo());
        result.put("kinhDo", vt.kinhDo());
        result.put("thoiGianCapNhatViTri", vt.thoiGian());
        return result;
    }

    public Map<String, Object> getAssignmentDetail(Integer phanCongId) {
        PhanCongCTV pc = phanCongCTVRepository.findById(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        DonDatDichVu don = pc.getDonDat();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("phanCongId", pc.getId());
        result.put("maPhanCong", pc.getMaPhanCong());
        result.put("trangThaiPhanCong", pc.getTrangThai());
        result.put("thoiGianPhanCong", pc.getThoiGianPhanCong());
        result.put("thoiGianXacNhan", pc.getThoiGianXacNhan());
        result.put("lyDoTuChoi", pc.getLyDoTuChoi());

        result.put("donDatId", don.getId());
        result.put("maDonDat", don.getMaDonDat());
        String tenDichVu = (don.getChiTietList() != null && !don.getChiTietList().isEmpty())
                ? don.getChiTietList().get(0).getDichVu().getTenDichVu() : "";
        result.put("tenDichVu", tenDichVu);
        result.put("loaiHinhDat", don.getLoaiHinhDat());
        result.put("ngayThucHien", don.getNgayThucHien());
        result.put("gioBatDau", don.getGioBatDau());
        result.put("gioKetThuc", don.getGioKetThuc());
        result.put("yeuCauDacBiet", don.getYeuCauDacBiet());
        result.put("ghiChu", don.getGhiChu());
        result.put("thanhTien", don.getThanhTien());

        Map<String, Object> khMap = new LinkedHashMap<>();
        khMap.put("hoTen", don.getKhachHang().getHoTen());
        khMap.put("soDienThoai", don.getKhachHang().getSoDienThoai());
        khMap.put("diaChi", don.getDiaChi() != null ? don.getDiaChi().getDiaChiChiTiet() : "");
        result.put("khachHang", khMap);

        return result;
    }

    public Map<String, Object> acceptAssignment(Integer phanCongId) {
        // Khóa phân công: CTV bấm hai lần hoặc CSKH đổi phân công cùng lúc sẽ được xử lý lần lượt
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        DonDatDichVu don = pc.getDonDat();

        // CTV tự nhận đơn cũng phải theo quy tắc xếp lịch: không trùng giờ, cách việc khác ít nhất 1 tiếng,
        // không quá tải trong ngày. Khóa CTV để hai lần nhận đơn cùng lúc được kiểm tra lần lượt.
        if (!"DaXacNhan".equalsIgnoreCase(pc.getTrangThai())) {
            congTacVienRepository.findByIdForUpdate(pc.getCongTacVien().getId());
            phanCongService.kiemTraLichHoacBaoLoi(pc.getCongTacVien().getId(), don.getNgayThucHien(),
                    don.getGioBatDau(), don.getGioKetThuc(),
                    don.getDiaChi() != null ? don.getDiaChi().getKhuVuc() : null, pc.getId(), false);
        }

        pc.setTrangThai("DaXacNhan"); 
        pc.setThoiGianXacNhan(LocalDateTime.now());
        phanCongCTVRepository.save(pc);

        String oldStatus = don.getTrangThai();
        don.setTrangThai("DaXacNhan");
        donDatDichVuRepository.save(don);

        // Lưu lịch sử trạng thái đơn
        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu(oldStatus)
                .trangThaiMoi("DaXacNhan")
                .nguoiThucHien("CongTacVien: " + pc.getCongTacVien().getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("Cộng tác viên xác nhận nhận đơn qua Mobile App")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Tự động tạo hoặc cập nhật Lịch làm việc cho CTV
        List<LichLamViec> existing = lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId());
        if (existing.isEmpty()) {
            LichLamViec llv = LichLamViec.builder()
                    .maLichLamViec(com.example.Service.MaSinh.tao("LLV-"))
                    .phanCong(pc)
                    .congTacVien(pc.getCongTacVien())
                    .ngayLam(don.getNgayThucHien())
                    .gioBatDau(don.getGioBatDau())
                    .gioKetThuc(don.getGioKetThuc())
                    .trangThai("SapToi")
                    .build();
            lichLamViecRepository.save(llv);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiPhanCong", pc.getTrangThai());
        res.put("donDatTrangThai", don.getTrangThai());
        res.put("message", "Đã xác nhận nhận đơn thành công. Lịch làm việc đã được cập nhật vào thời gian biểu.");
        return res;
    }

    public Map<String, Object> rejectAssignment(Integer phanCongId, AssignmentActionRequest req) {
        // Khóa phân công: CTV bấm hai lần hoặc CSKH đổi phân công cùng lúc sẽ được xử lý lần lượt
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        pc.setTrangThai("TuChoi");
        pc.setLyDoTuChoi(req.getLyDoTuChoi() != null ? req.getLyDoTuChoi() : "Bận lịch cá nhân");
        phanCongCTVRepository.save(pc);

        DonDatDichVu don = pc.getDonDat();
        don.setTrangThai("ChoDuyet"); // Trả lại về trạng thái chờ để HCNS phân công người khác
        donDatDichVuRepository.save(don);

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("DaPhanCong")
                .trangThaiMoi("ChoDuyet")
                .nguoiThucHien("CongTacVien: " + pc.getCongTacVien().getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV từ chối đơn. Lý do: " + pc.getLyDoTuChoi())
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiPhanCong", "TuChoi");
        res.put("message", "Đã từ chối đơn. Hệ thống sẽ thông báo phòng HCNS để phân công lại.");
        return res;
    }

    public Map<String, Object> completeAssignment(Integer phanCongId, AssignmentActionRequest req) {
        // Khóa phân công: CTV bấm hai lần hoặc CSKH đổi phân công cùng lúc sẽ được xử lý lần lượt
        PhanCongCTV pc = phanCongCTVRepository.findByIdForUpdate(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        pc.setTrangThai("HoanThanh");
        phanCongCTVRepository.save(pc);

        DonDatDichVu don = pc.getDonDat();
        don.setTrangThai("HoanThanh");
        donDatDichVuRepository.save(don);

        // Cập nhật kết quả trên Lịch làm việc
        List<LichLamViec> sches = lichLamViecRepository.findByPhanCong_DonDat_Id(don.getId());
        for (LichLamViec llv : sches) {
            llv.setTrangThai("HoanThanh");
            if (req.getKetQuaThucHien() != null) {
                llv.setKetQuaThucHien(req.getKetQuaThucHien());
            }
            lichLamViecRepository.save(llv);
        }

        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("DangThucHien")
                .trangThaiMoi("HoanThanh")
                .nguoiThucHien("CongTacVien: " + pc.getCongTacVien().getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV hoàn thành ca làm việc. Kết quả: " + (req.getKetQuaThucHien() != null ? req.getKetQuaThucHien() : "Hoàn thành tốt"))
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("donDatTrangThai", "HoanThanh");
        res.put("message", "Đã cập nhật trạng thái hoàn thành dịch vụ. Khách hàng có thể tiến hành đánh giá.");
        return res;
    }

    // ==========================================
    // UC-CTV02: NHẬN THÔNG BÁO CỦA CỘNG TÁC VIÊN
    // ==========================================
    public List<Map<String, Object>> getCollaboratorNotifications(Integer taiKhoanId) {
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(taiKhoanId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (ThongBaoNguoiDung tbnd : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tbnd.getId());
            m.put("thongBaoId", tbnd.getThongBao().getId());
            m.put("tieuDe", tbnd.getThongBao().getTieuDe());
            m.put("noiDung", tbnd.getThongBao().getNoiDung());
            m.put("thoiGianGui", tbnd.getThongBao().getThoiGianGui());
            m.put("daDoc", tbnd.getDaDoc());
            m.put("thoiGianDoc", tbnd.getThoiGianDoc());
            result.add(m);
        }
        return result;
    }

    public void markNotificationRead(Integer notificationId, Integer taiKhoanId) {
        thongBaoNguoiDungRepository.findById(notificationId).ifPresent(tbnd -> {
            if (tbnd.getTaiKhoan().getId().equals(taiKhoanId)) {
                tbnd.setDaDoc(true);
                tbnd.setThoiGianDoc(LocalDateTime.now());
                thongBaoNguoiDungRepository.save(tbnd);
            }
        });
    }

    public void markAllNotificationsRead(Integer taiKhoanId) {
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(taiKhoanId);
        for (ThongBaoNguoiDung tb : list) {
            tb.setDaDoc(true);
            tb.setThoiGianDoc(LocalDateTime.now());
        }
        thongBaoNguoiDungRepository.saveAll(list);
    }

    // ==========================================
    // UC-CTV03: XEM THỜI GIAN BIỂU LÀM VIỆC
    // ==========================================
    public List<Map<String, Object>> getSchedules(Integer congTacVienId, LocalDate fromDate, LocalDate toDate, String trangThai) {
        List<LichLamViec> list;
        if (fromDate != null && toDate != null) {
            list = lichLamViecRepository.findByCongTacVien_IdAndNgayLamBetweenOrderByNgayLamAscGioBatDauAsc(congTacVienId, fromDate, toDate);
        } else if (trangThai != null && !trangThai.isBlank()) {
            list = lichLamViecRepository.findByCongTacVien_IdAndTrangThaiOrderByNgayLamAscGioBatDauAsc(congTacVienId, trangThai);
        } else {
            list = lichLamViecRepository.findByCongTacVien_IdOrderByNgayLamAscGioBatDauAsc(congTacVienId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LichLamViec llv : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", llv.getId());
            item.put("maLichLamViec", llv.getMaLichLamViec());
            item.put("ngayLam", llv.getNgayLam());
            item.put("gioBatDau", llv.getGioBatDau());
            item.put("gioKetThuc", llv.getGioKetThuc());
            item.put("trangThai", llv.getTrangThai());
            item.put("ketQuaThucHien", llv.getKetQuaThucHien());

            if (llv.getPhanCong() != null && llv.getPhanCong().getDonDat() != null) {
                DonDatDichVu d = llv.getPhanCong().getDonDat();
                item.put("donDatId", d.getId());
                item.put("maDonDat", d.getMaDonDat());
                String tenDv = (d.getChiTietList() != null && !d.getChiTietList().isEmpty())
                        ? d.getChiTietList().get(0).getDichVu().getTenDichVu() : "";
                item.put("tenDichVu", tenDv);
                item.put("diaChi", d.getDiaChi() != null ? d.getDiaChi().getDiaChiChiTiet() : "");
                item.put("khachHangTen", d.getKhachHang().getHoTen());
                item.put("khachHangPhone", d.getKhachHang().getSoDienThoai());
            }
            result.add(item);
        }
        return result;
    }

    public Map<String, Object> getScheduleDetail(Integer lichLamViecId) {
        LichLamViec llv = lichLamViecRepository.findById(lichLamViecId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ca làm việc ID: " + lichLamViecId));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", llv.getId());
        result.put("maLichLamViec", llv.getMaLichLamViec());
        result.put("ngayLam", llv.getNgayLam());
        result.put("gioBatDau", llv.getGioBatDau());
        result.put("gioKetThuc", llv.getGioKetThuc());
        result.put("trangThai", llv.getTrangThai());
        result.put("ketQuaThucHien", llv.getKetQuaThucHien());

        if (llv.getPhanCong() != null && llv.getPhanCong().getDonDat() != null) {
            DonDatDichVu d = llv.getPhanCong().getDonDat();
            result.put("donDatId", d.getId());
            result.put("maDonDat", d.getMaDonDat());
            String tenDv = (d.getChiTietList() != null && !d.getChiTietList().isEmpty())
                    ? d.getChiTietList().get(0).getDichVu().getTenDichVu() : "";
            result.put("tenDichVu", tenDv);
            result.put("diaChi", d.getDiaChi() != null ? d.getDiaChi().getDiaChiChiTiet() : "");
            result.put("yeuCauDacBiet", d.getYeuCauDacBiet());
            result.put("khachHangTen", d.getKhachHang().getHoTen());
            result.put("khachHangPhone", d.getKhachHang().getSoDienThoai());
        }
        return result;
    }
    // ==========================================
    // UC-CTV04: QUẢN LÝ HỒ SƠ & TRẠNG THÁI (MỚI THÊM)
    // ==========================================

    /**
     * Lấy thông tin hồ sơ của Cộng tác viên
     */
    public Map<String, Object> getCollaboratorProfile(Integer congTacVienId) {
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên ID: " + congTacVienId));
        return toProfileMap(ctv);
    }

    /**
     * Dùng khi CTV đăng nhập trên app: nếu tài khoản đã có hồ sơ thì trả về hồ sơ đó,
     * nếu chưa có thì tạo một hồ sơ rỗng (chỉ có thông tin tối thiểu) để CTV bổ sung sau.
     */
    public Map<String, Object> ensureCollaboratorProfile(Integer taiKhoanId) {
        TaiKhoan taiKhoan = taiKhoanRepository.findById(taiKhoanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản ID: " + taiKhoanId));
        if (!"CongTacVien".equalsIgnoreCase(taiKhoan.getLoaiTaiKhoan())) {
            throw new IllegalArgumentException("Tài khoản này không phải tài khoản Cộng tác viên.");
        }

        Optional<CongTacVien> existing = congTacVienRepository.findByTaiKhoan_Id(taiKhoanId);
        if (existing.isPresent()) {
            Map<String, Object> result = toProfileMap(existing.get());
            result.put("daTaoMoi", false);
            return result;
        }

        CongTacVien ctv = CongTacVien.builder()
                .maCongTacVien("CTV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .taiKhoan(taiKhoan)
                .hoTen("")
                .noiCuTru("")
                .soDienThoai(taiKhoan.getSoDienThoai() != null ? taiKhoan.getSoDienThoai() : "")
                .diemDanhGia(java.math.BigDecimal.ZERO)
                .capDo("Moi")
                .trangThai("ChoDuyet")
                .ngayDangKy(LocalDate.now())
                .build();
        ctv = congTacVienRepository.save(ctv);

        Map<String, Object> result = toProfileMap(ctv);
        result.put("daTaoMoi", true);
        return result;
    }

    /** Trả hồ sơ dạng Map phẳng (tránh serialize proxy LAZY của TaiKhoan) */
    private Map<String, Object> toProfileMap(CongTacVien ctv) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ctv.getId());
        m.put("maCongTacVien", ctv.getMaCongTacVien());
        m.put("taiKhoanId", ctv.getTaiKhoan() != null ? ctv.getTaiKhoan().getId() : null);
        m.put("hoTen", ctv.getHoTen());
        m.put("ngaySinh", ctv.getNgaySinh() != null ? ctv.getNgaySinh().toString() : null);
        m.put("gioiTinh", ctv.getGioiTinh());
        m.put("noiCuTru", ctv.getNoiCuTru());
        m.put("soDienThoai", ctv.getSoDienThoai());
        m.put("diemDanhGia", ctv.getDiemDanhGia());
        m.put("capDo", ctv.getCapDo());
        m.put("trangThai", ctv.getTrangThai());
        m.put("ngayDangKy", ctv.getNgayDangKy() != null ? ctv.getNgayDangKy().toString() : null);
        return m;
    }

    /**
     * Cập nhật trạng thái (Sẵn sàng / Tạm dừng) của CTV
     */
    public Map<String, Object> updateCollaboratorStatus(Integer congTacVienId, String trangThaiMoi) {
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cộng tác viên ID: " + congTacVienId));
        
        ctv.setTrangThai(trangThaiMoi);
        congTacVienRepository.save(ctv);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("congTacVienId", ctv.getId());
        result.put("trangThaiMoi", ctv.getTrangThai());
        result.put("message", "Đã cập nhật trạng thái hoạt động thành công");
        return result;
    }
}
