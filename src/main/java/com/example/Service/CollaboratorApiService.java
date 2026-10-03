package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.*;
import com.example.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional
public class CollaboratorApiService {

    private final PhanCongCTVRepository phanCongCTVRepository;
    private final DonDatDichVuRepository donDatDichVuRepository;
    private final LichLamViecRepository lichLamViecRepository;
    private final CongTacVienRepository congTacVienRepository;
    private final LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    private final ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public CollaboratorApiService(
            PhanCongCTVRepository phanCongCTVRepository,
            DonDatDichVuRepository donDatDichVuRepository,
            LichLamViecRepository lichLamViecRepository,
            CongTacVienRepository congTacVienRepository,
            LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository,
            ThongBaoNguoiDungRepository thongBaoNguoiDungRepository,
            ThongBaoRepository thongBaoRepository,
            TaiKhoanRepository taiKhoanRepository) {
        this.phanCongCTVRepository = phanCongCTVRepository;
        this.donDatDichVuRepository = donDatDichVuRepository;
        this.lichLamViecRepository = lichLamViecRepository;
        this.congTacVienRepository = congTacVienRepository;
        this.lichSuTrangThaiDonRepository = lichSuTrangThaiDonRepository;
        this.thongBaoNguoiDungRepository = thongBaoNguoiDungRepository;
        this.thongBaoRepository = thongBaoRepository;
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
        PhanCongCTV pc = phanCongCTVRepository.findById(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        pc.setTrangThai("DaXacNhan"); 
        pc.setThoiGianXacNhan(LocalDateTime.now());
        phanCongCTVRepository.save(pc);

        DonDatDichVu don = pc.getDonDat();
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
                    .maLichLamViec("LLV-" + (System.currentTimeMillis() % 1000000))
                    .phanCong(pc)
                    .congTacVien(pc.getCongTacVien())
                    .ngayLam(don.getNgayThucHien())
                    .gioBatDau(don.getGioBatDau())
                    .gioKetThuc(don.getGioKetThuc())
                    .trangThai("SapToi")
                    .build();
            lichLamViecRepository.save(llv);
        }

        // Tự động gửi thông báo nhắc lịch / xác nhận ca làm cho CTV
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String tieuDe;
            String noiDung;
            String tenCTV = pc.getCongTacVien().getHoTen();
            String prefixCTV = "CTV " + tenCTV;
            String tenKhach = (don.getKhachHang() != null && don.getKhachHang().getHoTen() != null)
                    ? don.getKhachHang().getHoTen() : "Khách hàng";
            String diaChiStr = (don.getDiaChi() != null && don.getDiaChi().getDiaChiChiTiet() != null)
                    ? don.getDiaChi().getDiaChiChiTiet() : "";
            String suffixKH = " của KH " + tenKhach + (diaChiStr.isEmpty() ? "" : " (Đ/c: " + diaChiStr + ")");

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate ngayThucHien = don.getNgayThucHien();
            LocalTime gioBatDau = don.getGioBatDau();
            String gioStr = gioBatDau != null ? gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm")) : "giờ quy định";
            String ngayStr = ngayThucHien != null ? ngayThucHien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
            String thu = getThuTrongTuan(ngayThucHien);

            if (ngayThucHien != null && !ngayThucHien.isAfter(today)) {
                tieuDe = prefixCTV + " – Nhắc lịch ca làm gấp: " + don.getMaDonDat();
                noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " hôm nay (" + thu + ", " + ngayStr + "). Vui lòng nhớ có mặt trước " + gioStr + " hôm nay!";
            } else if (ngayThucHien != null && ngayThucHien.isEqual(tomorrow)) {
                tieuDe = prefixCTV + " – Nhắc lịch ca làm ngày mai: " + don.getMaDonDat();
                noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào ngày mai (" + thu + ", " + ngayStr + "). Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
            } else {
                tieuDe = prefixCTV + " – Nhắc lịch ca làm: " + don.getMaDonDat();
                noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào " + thu + ", ngày " + ngayStr + ". Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
            }

            ThongBao tbNhacLich = ThongBao.builder()
                    .maThongBao("TB-NL-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .tieuDe(tieuDe)
                    .noiDung(noiDung)
                    .nguoiGui("Hệ thống CSKH")
                    .nhomNhan("CongTacVien")
                    .thoiGianGui(LocalDateTime.now())
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(tbNhacLich);

            ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                    .thongBao(tbNhacLich)
                    .taiKhoan(pc.getCongTacVien().getTaiKhoan())
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build();
            thongBaoNguoiDungRepository.save(tbnd);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiPhanCong", pc.getTrangThai());
        res.put("donDatTrangThai", don.getTrangThai());
        res.put("message", "Đã xác nhận nhận đơn thành công. Lịch làm việc đã được cập nhật vào thời gian biểu.");
        return res;
    }

    public Map<String, Object> rejectAssignment(Integer phanCongId, AssignmentActionRequest req) {
        PhanCongCTV pc = phanCongCTVRepository.findById(phanCongId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phân công ID: " + phanCongId));

        pc.setTrangThai("TuChoi");
        String rawLyDo = (req != null) ? req.getLyDoTuChoi() : null;
        String cleanLyDo = (rawLyDo != null) ? rawLyDo.trim().replaceAll("\\s+", " ") : "";
        String lyDo = !cleanLyDo.isEmpty() ? cleanLyDo : "Bận lịch cá nhân";
        pc.setLyDoTuChoi(lyDo);
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
                .ghiChu("CTV từ chối đơn. Lý do: " + lyDo)
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Tạo thông báo xác nhận đã từ chối đơn cho CTV
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String tenCTV = pc.getCongTacVien().getHoTen();
            String prefixCTV = "CTV " + tenCTV;
            ThongBao tbTuChoi = ThongBao.builder()
                    .maThongBao("TB-TC-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                    .tieuDe(prefixCTV + " đã từ chối đơn: " + don.getMaDonDat())
                    .noiDung(prefixCTV + " đã từ chối nhận đơn " + don.getMaDonDat() + ". Lý do: \"" + lyDo + "\". Hệ thống đã chuyển lại đơn cho phòng CSKH để điều phối nhân sự khác.")
                    .nguoiGui("Hệ thống CSKH")
                    .nhomNhan("CongTacVien")
                    .thoiGianGui(LocalDateTime.now())
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(tbTuChoi);

            ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                    .thongBao(tbTuChoi)
                    .taiKhoan(pc.getCongTacVien().getTaiKhoan())
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build();
            thongBaoNguoiDungRepository.save(tbnd);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("trangThaiPhanCong", "TuChoi");
        res.put("message", "Đã từ chối đơn. Hệ thống sẽ thông báo phòng HCNS để phân công lại.");
        return res;
    }

    public Map<String, Object> completeAssignment(Integer phanCongId, AssignmentActionRequest req) {
        PhanCongCTV pc = phanCongCTVRepository.findById(phanCongId)
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
            if (req != null && req.getKetQuaThucHien() != null) {
                String cleanKetQua = req.getKetQuaThucHien().trim().replaceAll("\\s+", " ");
                llv.setKetQuaThucHien(cleanKetQua.isEmpty() ? "Hoàn thành công việc" : cleanKetQua);
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

        // Tạo thông báo hoàn thành đơn cho CTV
        if (pc.getCongTacVien() != null && pc.getCongTacVien().getTaiKhoan() != null) {
            String tenCTV = pc.getCongTacVien().getHoTen();
            String prefixCTV = "CTV " + tenCTV;
            String tienStr = don.getThanhTien() != null ? String.format("%,d", don.getThanhTien().longValue()) + "đ" : "";
            ThongBao tbComplete = ThongBao.builder()
                    .maThongBao("TB-" + System.currentTimeMillis())
                    .tieuDe(prefixCTV + " – Đơn đã hoàn thành: " + don.getMaDonDat())
                    .noiDung(prefixCTV + " đã hoàn thành đơn " + don.getMaDonDat()
                            + (tienStr.isEmpty() ? "" : ". Tiền công nhận được: " + tienStr)
                            + ". Chúc mừng bạn đã hoàn thành ca làm việc!")
                    .nguoiGui("Hệ thống")
                    .nhomNhan("CongTacVien")
                    .thoiGianGui(LocalDateTime.now())
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(tbComplete);

            ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                    .thongBao(tbComplete)
                    .taiKhoan(pc.getCongTacVien().getTaiKhoan())
                    .daDoc(false)
                    .trangThai("DaGui")
                    .build();
            thongBaoNguoiDungRepository.save(tbnd);
        }

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
        // Resolve TaiKhoan linh hoạt (hỗ trợ truyền taiKhoanId hoặc congTacVienId)
        TaiKhoan tk = taiKhoanRepository.findById(taiKhoanId).orElse(null);
        CongTacVien ctv = null;
        if (tk == null) {
            ctv = congTacVienRepository.findById(taiKhoanId).orElse(null);
            if (ctv != null) {
                tk = ctv.getTaiKhoan();
            }
        } else {
            ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
            if (ctv == null) {
                ctv = congTacVienRepository.findById(tk.getId()).orElse(null);
            }
        }
        if (tk == null) {
            return Collections.emptyList();
        }
        Integer actualTaiKhoanId = tk.getId();

        // 1. Tự động đồng bộ các thông báo chung từ Ban Giám Đốc (nhomNhan = CongTacVien hoặc TatCa)
        List<ThongBao> broadcastList = thongBaoRepository.findAll().stream()
                .filter(tb -> "DaGui".equalsIgnoreCase(tb.getTrangThai()) &&
                        ("CongTacVien".equalsIgnoreCase(tb.getNhomNhan()) || "TatCa".equalsIgnoreCase(tb.getNhomNhan())))
                .toList();

        List<ThongBaoNguoiDung> existingList = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(actualTaiKhoanId);
        Set<Integer> existingTbIds = new HashSet<>();
        for (ThongBaoNguoiDung item : existingList) {
            if (item.getThongBao() != null) {
                existingTbIds.add(item.getThongBao().getId());
            }
        }

        List<ThongBaoNguoiDung> newlyCreated = new ArrayList<>();
        for (ThongBao tb : broadcastList) {
            if (!existingTbIds.contains(tb.getId())) {
                ThongBaoNguoiDung nb = ThongBaoNguoiDung.builder()
                        .thongBao(tb)
                        .taiKhoan(tk)
                        .daDoc(false)
                        .trangThai("DaGui")
                        .build();
                newlyCreated.add(nb);
                existingTbIds.add(tb.getId());
            }
        }

        // 2. Tự động đồng bộ thông báo cho các đơn phân công thực tế của CTV (Hoàn thành, Đơn mới)
        if (ctv != null) {
            List<PhanCongCTV> pcList = phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(ctv.getId());
            Set<String> notifiedKeys = new HashSet<>();
            for (ThongBaoNguoiDung item : existingList) {
                if (item.getThongBao() != null) {
                    String fullText = ((item.getThongBao().getTieuDe() != null ? item.getThongBao().getTieuDe() : "") + " "
                            + (item.getThongBao().getNoiDung() != null ? item.getThongBao().getNoiDung() : "")).toLowerCase();
                    for (PhanCongCTV pc : pcList) {
                        if (pc.getDonDat() != null && pc.getDonDat().getMaDonDat() != null) {
                            String code = pc.getDonDat().getMaDonDat().toLowerCase();
                            if (fullText.contains(code)) {
                                if (fullText.contains("hoàn thành")) {
                                    notifiedKeys.add(pc.getDonDat().getMaDonDat() + "_HOAN_THANH");
                                } else {
                                    notifiedKeys.add(pc.getDonDat().getMaDonDat() + "_DON_MOI");
                                }
                            }
                        }
                    }
                }
            }

            for (PhanCongCTV pc : pcList) {
                if (pc.getDonDat() == null || pc.getDonDat().getMaDonDat() == null) continue;
                String maDon = pc.getDonDat().getMaDonDat();
                if ("HoanThanh".equalsIgnoreCase(pc.getTrangThai())) {
                    if (!notifiedKeys.contains(maDon + "_HOAN_THANH")) {
                        String tienStr = pc.getDonDat().getThanhTien() != null
                                ? String.format("%,d", pc.getDonDat().getThanhTien().longValue()) + "đ"
                                : "";
                        LocalDateTime tg = pc.getThoiGianXacNhan() != null ? pc.getThoiGianXacNhan()
                                : (pc.getThoiGianPhanCong() != null ? pc.getThoiGianPhanCong() : LocalDateTime.now());
                        ThongBao tbComplete = ThongBao.builder()
                                .maThongBao("TB-HT-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                                .tieuDe("Đơn " + maDon + " đã hoàn thành")
                                .noiDung("Bạn nhận được " + tienStr + " từ đơn " + maDon + ". Chúc mừng bạn đã hoàn thành xuất sắc ca làm việc!")
                                .nguoiGui("Hệ thống")
                                .nhomNhan("CongTacVien")
                                .thoiGianGui(tg)
                                .trangThai("DaGui")
                                .build();
                        thongBaoRepository.save(tbComplete);

                        ThongBaoNguoiDung nb = ThongBaoNguoiDung.builder()
                                .thongBao(tbComplete)
                                .taiKhoan(tk)
                                .daDoc(false)
                                .trangThai("DaGui")
                                .build();
                        newlyCreated.add(nb);
                        notifiedKeys.add(maDon + "_HOAN_THANH");
                    }
                } else if ("DaPhanCong".equalsIgnoreCase(pc.getTrangThai()) || "ChoXacNhan".equalsIgnoreCase(pc.getTrangThai())) {
                    if (!notifiedKeys.contains(maDon + "_DON_MOI")) {
                        LocalDateTime tg = pc.getThoiGianPhanCong() != null ? pc.getThoiGianPhanCong() : LocalDateTime.now();
                        ThongBao tbAssign = ThongBao.builder()
                                .maThongBao("TB-PC-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                                .tieuDe("Bạn có đơn mới: " + maDon)
                                .noiDung("Bạn vừa được phân công đơn mới " + maDon + ". Vui lòng kiểm tra chi tiết và xác nhận nhận đơn!")
                                .nguoiGui("Hệ thống CSKH")
                                .nhomNhan("CongTacVien")
                                .thoiGianGui(tg)
                                .trangThai("DaGui")
                                .build();
                        thongBaoRepository.save(tbAssign);

                        ThongBaoNguoiDung nb = ThongBaoNguoiDung.builder()
                                .thongBao(tbAssign)
                                .taiKhoan(tk)
                                .daDoc(false)
                                .trangThai("DaGui")
                                .build();
                        newlyCreated.add(nb);
                        notifiedKeys.add(maDon + "_DON_MOI");
                    }
                }
            }
        }

        // 3. Nếu CTV chưa có thông báo cá nhân nào từ trước, tạo các thông báo thực tế theo trạng thái tài khoản
        if (existingList.isEmpty() && newlyCreated.isEmpty()) {
            ThongBao welcomeTb = ThongBao.builder()
                    .maThongBao("TB-WELCOME-" + actualTaiKhoanId)
                    .tieuDe("Chào mừng đến Neatify")
                    .noiDung("Chào mừng bạn gia nhập đội ngũ Cộng tác viên Neatify! Hoàn thiện hồ sơ để bắt đầu nhận được nhiều đơn hơn.")
                    .nguoiGui("Ban Giám đốc")
                    .nhomNhan("CongTacVien")
                    .thoiGianGui(LocalDateTime.now().minusDays(2))
                    .trangThai("DaGui")
                    .build();
            thongBaoRepository.save(welcomeTb);
            newlyCreated.add(ThongBaoNguoiDung.builder().thongBao(welcomeTb).taiKhoan(tk).daDoc(false).trangThai("DaGui").build());
        }

        if (!newlyCreated.isEmpty()) {
            thongBaoNguoiDungRepository.saveAll(newlyCreated);
            existingList = thongBaoNguoiDungRepository.findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(actualTaiKhoanId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (ThongBaoNguoiDung tbnd : existingList) {
            if (tbnd.getThongBao() == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", tbnd.getId());
            m.put("thongBaoId", tbnd.getThongBao().getId());
            m.put("tieuDe", tbnd.getThongBao().getTieuDe());
            m.put("noiDung", tbnd.getThongBao().getNoiDung());
            m.put("nguoiGui", tbnd.getThongBao().getNguoiGui());
            m.put("thoiGianGui", tbnd.getThongBao().getThoiGianGui());
            m.put("daDoc", tbnd.getDaDoc());
            m.put("thoiGianDoc", tbnd.getThoiGianDoc());
            m.put("loai", resolveLoaiThongBao(tbnd.getThongBao().getTieuDe(), tbnd.getThongBao().getNhomNhan()));
            result.add(m);
        }
        return result;
    }

    private String resolveLoaiThongBao(String tieuDe, String nhomNhan) {
        if (tieuDe == null) return "heThong";
        String lower = tieuDe.toLowerCase();
        if (lower.contains("nhắc lịch") || lower.contains("giờ làm") || lower.contains("ca làm") || lower.contains("sắp đến")) {
            return "nhacLich";
        }
        if (lower.contains("hoàn thành") || lower.contains("thanh toán") || lower.contains("tiền")) {
            return "hoanThanh";
        }
        if (lower.contains("đơn mới") || lower.contains("phân công") || lower.contains("nhận đơn") || lower.contains("giao đơn")) {
            return "donMoi";
        }
        if (lower.contains("từ chối") || lower.contains("hủy")) {
            return "huy";
        }
        return "heThong";
    }

    private String getThuTrongTuan(LocalDate date) {
        if (date == null) return "";
        switch (date.getDayOfWeek()) {
            case MONDAY: return "Thứ Hai";
            case TUESDAY: return "Thứ Ba";
            case WEDNESDAY: return "Thứ Tư";
            case THURSDAY: return "Thứ Năm";
            case FRIDAY: return "Thứ Sáu";
            case SATURDAY: return "Thứ Bảy";
            case SUNDAY: return "Chủ Nhật";
            default: return "";
        }
    }

    public void markNotificationRead(Integer notificationId, Integer taiKhoanId) {
        thongBaoNguoiDungRepository.findById(notificationId).ifPresent(tbnd -> {
            tbnd.setDaDoc(true);
            tbnd.setThoiGianDoc(LocalDateTime.now());
            thongBaoNguoiDungRepository.save(tbnd);
        });
    }

    public void markAllNotificationsRead(Integer taiKhoanId) {
        // Resolve TaiKhoan nếu truyền ctvId
        Integer actualId = taiKhoanId;
        TaiKhoan tk = taiKhoanRepository.findById(taiKhoanId).orElse(null);
        if (tk == null) {
            CongTacVien ctv = congTacVienRepository.findById(taiKhoanId).orElse(null);
            if (ctv != null && ctv.getTaiKhoan() != null) {
                actualId = ctv.getTaiKhoan().getId();
            }
        }
        List<ThongBaoNguoiDung> list = thongBaoNguoiDungRepository.findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(actualId);
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

    // ==========================================
    // UC-CTV-POOL: NHẬN ĐƠN TỪ POOL CHUNG
    // ==========================================

    /**
     * Lấy danh sách đơn đang tìm CTV (pool chung).
     */
    public List<Map<String, Object>> getAvailableOrders(Integer congTacVienId) {
        List<DonDatDichVu> donList = donDatDichVuRepository.findByTrangThaiOrderByNgayTaoDesc("ChoDuyet");

        List<Map<String, Object>> result = new ArrayList<>();
        for (DonDatDichVu don : donList) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("donDatId", don.getId());
            item.put("maDonDat", don.getMaDonDat());
            String tenDichVu = (don.getChiTietList() != null && !don.getChiTietList().isEmpty())
                    ? don.getChiTietList().get(0).getDichVu().getTenDichVu() : "Dịch vụ";
            item.put("tenDichVu", tenDichVu);
            item.put("ngayThucHien", don.getNgayThucHien());
            item.put("gioBatDau", don.getGioBatDau());
            item.put("gioKetThuc", don.getGioKetThuc());
            item.put("diaChi", don.getDiaChi() != null ? don.getDiaChi().getDiaChiChiTiet() : "");
            item.put("yeuCauDacBiet", don.getYeuCauDacBiet());
            item.put("thanhTien", don.getThanhTien());
            item.put("ngayTao", don.getNgayTao());
            // Không expose thông tin khách hàng cho CTV khi đơn chưa được nhận
            item.put("khachHangTen", "Khách hàng");
            item.put("khachHangPhone", "");
            result.add(item);
        }
        return result;
    }

    /**
     * CTV nhận đơn từ pool (atomic - first-come-first-served).
     * Dùng database-level lock để đảm bảo chỉ 1 CTV thắng khi nhiều người ấn cùng lúc.
     */
    @Transactional
    public Map<String, Object> acceptOrderFromPool(Integer congTacVienId, Integer donDatId) {
        // Resolve congTacVienId
        CongTacVien ctv = congTacVienRepository.findById(congTacVienId).orElse(null);
        if (ctv == null) {
            TaiKhoan tk = taiKhoanRepository.findById(congTacVienId).orElse(null);
            if (tk != null) ctv = congTacVienRepository.findByTaiKhoan_Id(tk.getId()).orElse(null);
        }
        if (ctv == null) throw new IllegalArgumentException("Không tìm thấy CTV ID: " + congTacVienId);
        if (ctv.getTaiKhoan() == null) throw new IllegalStateException("CTV chưa có tài khoản");

        // !! ATOMIC UPDATE - chỉ 1 request thắng !!
        int rowsAffected = donDatDichVuRepository.atomicAcceptOrder(donDatId);
        if (rowsAffected == 0) {
            throw new IllegalStateException("Đơn này vừa có người nhận rồi! Vui lòng chọn đơn khác.");
        }

        DonDatDichVu don = donDatDichVuRepository.findById(donDatId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn ID: " + donDatId));

        // Tạo 1 bản ghi PhanCongCTV duy nhất khi nhận việc
        PhanCongCTV pc = PhanCongCTV.builder()
                .maPhanCong("PC-" + (System.currentTimeMillis() % 1000000))
                .donDat(don)
                .congTacVien(ctv)
                .trangThai("DaXacNhan")
                .thoiGianPhanCong(LocalDateTime.now())
                .thoiGianXacNhan(LocalDateTime.now())
                .build();
        phanCongCTVRepository.save(pc);

        // Tạo Lịch làm việc
        LichLamViec llv = LichLamViec.builder()
                .maLichLamViec("LLV-" + (System.currentTimeMillis() % 1000000))
                .phanCong(pc)
                .congTacVien(ctv)
                .ngayLam(don.getNgayThucHien())
                .gioBatDau(don.getGioBatDau())
                .gioKetThuc(don.getGioKetThuc())
                .trangThai("SapToi")
                .build();
        lichLamViecRepository.save(llv);

        // Lưu lịch sử
        LichSuTrangThaiDon ls = LichSuTrangThaiDon.builder()
                .donDat(don)
                .trangThaiCu("ChoDuyet")
                .trangThaiMoi("DaXacNhan")
                .nguoiThucHien("CongTacVien: " + ctv.getHoTen())
                .thoiGian(LocalDateTime.now())
                .ghiChu("CTV tự nhận đơn từ pool qua Mobile App")
                .build();
        lichSuTrangThaiDonRepository.save(ls);

        // Thông báo xác nhận cho CTV
        LocalDate ngayThucHien = don.getNgayThucHien();
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalTime gioBatDau = don.getGioBatDau();
        String gioStr = gioBatDau != null ? gioBatDau.format(DateTimeFormatter.ofPattern("HH:mm")) : "giờ quy định";
        String ngayStr = ngayThucHien != null ? ngayThucHien.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        String thu = getThuTrongTuan(ngayThucHien);

        String tieuDe;
        String noiDung;
        String tenCTV = ctv.getHoTen();
        String prefixCTV = "CTV " + tenCTV;
        String tenKhach = (don.getKhachHang() != null && don.getKhachHang().getHoTen() != null)
                ? don.getKhachHang().getHoTen() : "Khách hàng";
        String diaChiStr = (don.getDiaChi() != null && don.getDiaChi().getDiaChiChiTiet() != null)
                ? don.getDiaChi().getDiaChiChiTiet() : "";
        String suffixKH = " của KH " + tenKhach + (diaChiStr.isEmpty() ? "" : " (Đ/c: " + diaChiStr + ")");

        if (ngayThucHien != null && !ngayThucHien.isAfter(today)) {
            tieuDe = prefixCTV + " – Nhắc lịch ca làm gấp: " + don.getMaDonDat();
            noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " hôm nay (" + thu + ", " + ngayStr + "). Vui lòng nhớ có mặt trước " + gioStr + " hôm nay!";
        } else if (ngayThucHien != null && ngayThucHien.isEqual(tomorrow)) {
            tieuDe = prefixCTV + " – Nhắc lịch ca làm ngày mai: " + don.getMaDonDat();
            noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào ngày mai (" + thu + ", " + ngayStr + "). Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
        } else {
            tieuDe = prefixCTV + " – Nhắc lịch ca làm: " + don.getMaDonDat();
            noiDung = prefixCTV + " đã nhận đơn" + suffixKH + " vào " + thu + ", ngày " + ngayStr + ". Nhớ có mặt trước thời gian làm việc (" + gioStr + ") nhé!";
        }

        ThongBao tbNhacLich = ThongBao.builder()
                .maThongBao("TB-NL-" + pc.getId() + "-" + (System.currentTimeMillis() % 100000))
                .tieuDe(tieuDe)
                .noiDung(noiDung)
                .nguoiGui("Hệ thống CSKH")
                .nhomNhan("CongTacVien")
                .thoiGianGui(LocalDateTime.now())
                .trangThai("DaGui")
                .build();
        thongBaoRepository.save(tbNhacLich);

        ThongBaoNguoiDung tbnd = ThongBaoNguoiDung.builder()
                .thongBao(tbNhacLich)
                .taiKhoan(ctv.getTaiKhoan())
                .daDoc(false)
                .trangThai("DaGui")
                .build();
        thongBaoNguoiDungRepository.save(tbnd);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("phanCongId", pc.getId());
        res.put("maDonDat", don.getMaDonDat());
        res.put("trangThai", "DaXacNhan");
        res.put("message", "Đã nhận đơn thành công!");
        return res;
    }

    /**
     * CTV bỏ qua / từ chối đơn từ pool trên ứng dụng.
     * Không chèn bản ghi vào DB. Mobile app tự ẩn đơn trên giao diện local.
     */
    public Map<String, Object> rejectOrderFromPool(Integer congTacVienId, Integer donDatId, String lyDo) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("donDatId", donDatId);
        res.put("message", "Đã bỏ qua đơn hàng");
        return res;
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

    /**
     * Tạo đơn test DangTimCTV (Pool) + Thông báo Broadcast gửi TẤT CẢ CTV
     */
    public Map<String, Object> createTestPoolOrder(String maDon, String tenDichVu, Integer thanhTien) {
        String finalMaDon = (maDon != null && !maDon.isBlank()) ? maDon : "DON-POOL-" + (System.currentTimeMillis() % 100000);
        
        // Lấy đơn mẫu để lấy khachHang & diaChi
        DonDatDichVu sampleOrder = donDatDichVuRepository.findAll().stream().findFirst().orElse(null);
        if (sampleOrder == null) throw new IllegalStateException("Chưa có đơn mẫu nào trong DB");

        java.math.BigDecimal tien = thanhTien != null ? java.math.BigDecimal.valueOf(thanhTien) : java.math.BigDecimal.valueOf(320000);
        DonDatDichVu don = DonDatDichVu.builder()
                .maDonDat(finalMaDon)
                .khachHang(sampleOrder.getKhachHang())
                .diaChi(sampleOrder.getDiaChi())
                .trangThai("ChoDuyet")
                .ngayTao(LocalDateTime.now())
                .ngayThucHien(LocalDate.now().plusDays(1))
                .gioBatDau(LocalTime.of(9, 0))
                .gioKetThuc(LocalTime.of(11, 0))
                .loaiHinhDat(sampleOrder.getLoaiHinhDat() != null ? sampleOrder.getLoaiHinhDat() : "TheoLan")
                .chiPhiGoc(tien)
                .soTienGiam(java.math.BigDecimal.ZERO)
                .thanhTien(tien)
                .yeuCauDacBiet("Căn hộ 2 phòng ngủ - Test Pool đơn mới")
                .build();
        don = donDatDichVuRepository.save(don);

        String sampleKhach = (sampleOrder.getKhachHang() != null && sampleOrder.getKhachHang().getHoTen() != null)
                ? sampleOrder.getKhachHang().getHoTen() : "Nguyễn Thị Thu Lan";
        String sampleDiaChi = (sampleOrder.getDiaChi() != null && sampleOrder.getDiaChi().getDiaChiChiTiet() != null)
                ? sampleOrder.getDiaChi().getDiaChiChiTiet() : "123 Nguyễn Trãi, Q.1";

        // Tạo thông báo Broadcast tới TẤT CẢ CTV (nhomNhan = CongTacVien)
        ThongBao tbBroadcast = ThongBao.builder()
                .maThongBao("TB-POOL-" + don.getId() + "-" + (System.currentTimeMillis() % 100000))
                .tieuDe("Bạn có đơn mới: " + finalMaDon)
                .noiDung("Có đơn mới " + (tenDichVu != null ? tenDichVu : "Dọn dẹp nhà cửa") + " của KH " + sampleKhach + " vào ngày mai lúc 09:00 (Địa chỉ: " + sampleDiaChi + "). Vui lòng vào ứng dụng bấm Nhận việc!")
                .nguoiGui("Hệ thống CSKH")
                .nhomNhan("CongTacVien")
                .thoiGianGui(LocalDateTime.now())
                .trangThai("DaGui")
                .build();
        thongBaoRepository.save(tbBroadcast);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("donDatId", don.getId());
        res.put("maDonDat", don.getMaDonDat());
        res.put("trangThai", don.getTrangThai());
        res.put("thongBaoId", tbBroadcast.getId());
        res.put("message", "Đã tạo đơn pool test thành công và phát thông báo tới tất cả CTV!");
        return res;
    }
}

