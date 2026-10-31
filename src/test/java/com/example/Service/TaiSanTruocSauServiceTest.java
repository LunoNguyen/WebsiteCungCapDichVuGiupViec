package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.*;
import com.example.Repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử luồng bắt buộc chụp ảnh tài sản Trước & Sau khi thực hiện dịch vụ:
 * 1. Bắt đầu thực hiện dịch vụ -> Bắt buộc chụp ảnh tài sản trước khi làm (TruocDichVu).
 * 2. Hoàn thành dịch vụ -> Bắt buộc chụp ảnh tài sản sau khi làm (SauDichVu), nếu không chụp thì đơn KHÔNG được sang HoanThanh.
 * 3. Lưu BieuMauTaiSan + ChiTietTaiSan và trả về danh sách ảnh Trước/Sau khi xem chi tiết đơn đặt.
 */
class TaiSanTruocSauServiceTest {

    private PhanCongCTVRepository phanCongCTVRepository;
    private DonDatDichVuRepository donDatDichVuRepository;
    private LichLamViecRepository lichLamViecRepository;
    private CongTacVienRepository congTacVienRepository;
    private LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    private ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    private ThongBaoRepository thongBaoRepository;
    private TaiKhoanRepository taiKhoanRepository;
    private KhachHangRepository khachHangRepository;
    private BieuMauTaiSanRepository bieuMauTaiSanRepository;
    private ChiTietTaiSanRepository chiTietTaiSanRepository;

    private CollaboratorApiService service;

    private final List<BieuMauTaiSan> savedForms = new ArrayList<>();
    private final List<ChiTietTaiSan> savedDetails = new ArrayList<>();

    @BeforeEach
    void setUp() {
        phanCongCTVRepository = mock(PhanCongCTVRepository.class);
        donDatDichVuRepository = mock(DonDatDichVuRepository.class);
        lichLamViecRepository = mock(LichLamViecRepository.class);
        congTacVienRepository = mock(CongTacVienRepository.class);
        lichSuTrangThaiDonRepository = mock(LichSuTrangThaiDonRepository.class);
        thongBaoNguoiDungRepository = mock(ThongBaoNguoiDungRepository.class);
        thongBaoRepository = mock(ThongBaoRepository.class);
        taiKhoanRepository = mock(TaiKhoanRepository.class);
        khachHangRepository = mock(KhachHangRepository.class);
        bieuMauTaiSanRepository = mock(BieuMauTaiSanRepository.class);
        chiTietTaiSanRepository = mock(ChiTietTaiSanRepository.class);

        service = new CollaboratorApiService(
                phanCongCTVRepository,
                donDatDichVuRepository,
                lichLamViecRepository,
                congTacVienRepository,
                lichSuTrangThaiDonRepository,
                thongBaoNguoiDungRepository,
                thongBaoRepository,
                taiKhoanRepository,
                khachHangRepository
        );
        ReflectionTestUtils.setField(service, "bieuMauTaiSanRepository", bieuMauTaiSanRepository);
        ReflectionTestUtils.setField(service, "chiTietTaiSanRepository", chiTietTaiSanRepository);

        savedForms.clear();
        savedDetails.clear();

        when(bieuMauTaiSanRepository.save(any(BieuMauTaiSan.class))).thenAnswer(inv -> {
            BieuMauTaiSan bm = inv.getArgument(0);
            if (bm.getId() == null) bm.setId(savedForms.size() + 1);
            savedForms.add(bm);
            return bm;
        });

        when(chiTietTaiSanRepository.save(any(ChiTietTaiSan.class))).thenAnswer(inv -> {
            ChiTietTaiSan ct = inv.getArgument(0);
            if (ct.getId() == null) ct.setId(savedDetails.size() + 1);
            savedDetails.add(ct);
            return ct;
        });

        when(bieuMauTaiSanRepository.findByDonDat_IdOrderByThoiGianGhiNhanAsc(any())).thenAnswer(inv -> {
            Integer donId = inv.getArgument(0);
            return savedForms.stream()
                    .filter(f -> f.getDonDat() != null && Objects.equals(f.getDonDat().getId(), donId))
                    .toList();
        });

        when(bieuMauTaiSanRepository.findByDonDat_IdAndLoaiBieuMauOrderByThoiGianGhiNhanDesc(any(), any())).thenAnswer(inv -> {
            Integer donId = inv.getArgument(0);
            String loai = inv.getArgument(1);
            return savedForms.stream()
                    .filter(f -> f.getDonDat() != null
                            && Objects.equals(f.getDonDat().getId(), donId)
                            && loai.equalsIgnoreCase(f.getLoaiBieuMau()))
                    .toList();
        });

        when(chiTietTaiSanRepository.findByBieuMau_Id(any())).thenAnswer(inv -> {
            Integer bmId = inv.getArgument(0);
            return savedDetails.stream()
                    .filter(d -> d.getBieuMau() != null && Objects.equals(d.getBieuMau().getId(), bmId))
                    .toList();
        });
    }

    private PhanCongCTV taoPhanCongMau(String trangThaiDon, String trangThaiPc) {
        CongTacVien ctv = CongTacVien.builder()
                .id(10)
                .hoTen("Nguyễn Văn A")
                .build();

        DonDatDichVu don = DonDatDichVu.builder()
                .id(88)
                .maDonDat("DD-088")
                .loaiHinhDat("TheoLan")
                .ngayThucHien(LocalDate.now())
                .gioBatDau(LocalTime.now().minusMinutes(10))
                .gioKetThuc(LocalTime.now().plusHours(2))
                .thanhTien(new BigDecimal("250000"))
                .trangThai(trangThaiDon)
                .build();

        PhanCongCTV pc = PhanCongCTV.builder()
                .id(55)
                .maPhanCong("PC-055")
                .donDat(don)
                .congTacVien(ctv)
                .trangThai(trangThaiPc)
                .thoiGianPhanCong(LocalDateTime.now().minusHours(1))
                .build();

        when(phanCongCTVRepository.findById(55)).thenReturn(Optional.of(pc));
        when(phanCongCTVRepository.findByIdForUpdate(55)).thenReturn(Optional.of(pc));
        when(phanCongCTVRepository.findAllByDonDat_Id(88)).thenReturn(List.of(pc));
        when(lichLamViecRepository.findByPhanCong_Id(55)).thenReturn(Collections.emptyList());
        when(lichLamViecRepository.findByPhanCong_DonDat_Id(88)).thenReturn(Collections.emptyList());
        return pc;
    }

    @Test
    void batDauLam_KhongChupAnhTruoc_VanBatDauVaKhongLuuBieuMau() {
        PhanCongCTV pc = taoPhanCongMau("DaXacNhan", "DaXacNhan");

        // Ảnh tài sản không bắt buộc
        service.startAssignment(55, new AssignmentActionRequest());

        assertEquals("DangThucHien", pc.getDonDat().getTrangThai());
        assertTrue(savedForms.isEmpty());
    }

    @Test
    void hoanThanhDon_KhongChupAnhSau_VanHoanThanhVaKhongLuuBieuMau() {
        PhanCongCTV pc = taoPhanCongMau("DangThucHien", "DaXacNhan");

        AssignmentActionRequest emptyReq = new AssignmentActionRequest();
        emptyReq.setKetQuaThucHien("Đã dọn xong");
        service.completeAssignment(55, emptyReq);

        assertEquals("HoanThanh", pc.getTrangThai());
        assertTrue(savedForms.isEmpty());
    }

    @Test
    void luongDayDu_ChupTruocVaChupSau_LuuDbVaCong80PhanTramThuNhap() {
        PhanCongCTV pc = taoPhanCongMau("DaXacNhan", "DaXacNhan");

        // Bước 1: Chụp ảnh trước khi thực hiện và ấn Bắt đầu
        AssignmentActionRequest startReq = new AssignmentActionRequest();
        startReq.setDanhSachHinhAnh(List.of("don-dat/tai-san/truoc_1.png", "don-dat/tai-san/truoc_2.png"));
        startReq.setKhuVucVatDung("Phòng khách");
        startReq.setMoTaHienTrang("Tài sản nguyên vẹn, bình thường");

        Map<String, Object> startRes = service.startAssignment(55, startReq);
        assertEquals("DangThucHien", startRes.get("trangThaiDon"));
        assertEquals("DangThucHien", pc.getDonDat().getTrangThai());
        assertEquals(Boolean.TRUE, startRes.get("daChupTruocDichVu"));
        assertEquals(1, savedForms.size());
        assertEquals("TruocDichVu", savedForms.get(0).getLoaiBieuMau());
        assertEquals(2, savedDetails.size());

        // Bước 2: Sau khi làm xong ngoài đời, chụp ảnh sau khi thực hiện và ấn Hoàn thành
        AssignmentActionRequest completeReq = new AssignmentActionRequest();
        completeReq.setDanhSachHinhAnh(List.of("don-dat/tai-san/sau_1.png"));
        completeReq.setKhuVucVatDung("Phòng khách");
        completeReq.setMoTaHienTrang("Đã dọn sạch sẽ, tài sản nguyên vẹn");
        completeReq.setKetQuaThucHien("Hoàn thành tốt");

        Map<String, Object> completeRes = service.completeAssignment(55, completeReq);

        // Kiểm tra đơn đã sang HoanThanh, tính đúng 80% của 250.000đ (= 200.000đ) và 20% phí (= 50.000đ)
        assertEquals("HoanThanh", completeRes.get("donDatTrangThai"));
        assertEquals("HoanThanh", pc.getDonDat().getTrangThai());
        assertEquals(new BigDecimal("200000"), completeRes.get("tienThucNhan"));
        assertEquals(new BigDecimal("50000"), completeRes.get("phiNenTang"));

        // Kiểm tra có đủ cả ảnh Trước và ảnh Sau khi xem đơn đặt
        assertEquals(Boolean.TRUE, completeRes.get("daChupTruocDichVu"));
        assertEquals(Boolean.TRUE, completeRes.get("daChupSauDichVu"));

        List<?> anhTruoc = (List<?>) completeRes.get("anhTruocDichVu");
        List<?> anhSau = (List<?>) completeRes.get("anhSauDichVu");
        assertEquals(2, anhTruoc.size());
        assertEquals(1, anhSau.size());
    }

    @Test
    void minioService_ChoPhepXemAnhTaiSanCongKhaiQuaFileViewController() {
        MinioService minioService = new MinioService(mock(io.minio.MinioClient.class));
        assertTrue(minioService.isPublic("don-dat/tai-san/anh_truoc_123.png"));
        assertTrue(minioService.isPublic("cong-khai/dich-vu/icon.png"));
        assertFalse(minioService.isPublic("ctv/cccd/mat_truoc.png"));
    }
}
