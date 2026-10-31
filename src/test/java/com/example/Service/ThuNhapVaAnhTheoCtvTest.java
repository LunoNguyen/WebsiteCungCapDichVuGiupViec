package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.ChiTietDonDat;
import com.example.Model.CongTacVien;
import com.example.Model.DonDatDichVu;
import com.example.Model.LichLamViec;
import com.example.Model.PhanCongCTV;
import com.example.Repository.BieuMauTaiSanRepository;
import com.example.Repository.LichLamViecRepository;
import com.example.Repository.PhanCongCTVRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Đơn nhiều dịch vụ: CTV chỉ nhận tiền công phần dịch vụ mình làm; ảnh tài sản trước / sau không bắt buộc. */
@ExtendWith(MockitoExtension.class)
class ThuNhapVaAnhTheoCtvTest {

    @Mock PhanCongCTVRepository phanCongCTVRepository;
    @Mock BieuMauTaiSanRepository bieuMauTaiSanRepository;
    @Mock LichLamViecRepository lichLamViecRepository;
    // Repo phụ để hoàn thành chạy hết luồng (lịch sử đơn, thông báo, ví)
    @Mock com.example.Repository.DonDatDichVuRepository donDatDichVuRepository;
    @Mock com.example.Repository.CongTacVienRepository congTacVienRepository;
    @Mock com.example.Repository.LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    @Mock com.example.Repository.ThongBaoNguoiDungRepository thongBaoNguoiDungRepository;
    @Mock com.example.Repository.ThongBaoRepository thongBaoRepository;
    @Mock com.example.Repository.TaiKhoanRepository taiKhoanRepository;
    @Mock com.example.Repository.KhachHangRepository khachHangRepository;
    @InjectMocks CollaboratorApiService service;

    @BeforeEach
    void ganRepoAnh() {
        // bieuMauTaiSanRepository là trường @Autowired (không qua constructor)
        ReflectionTestUtils.setField(service, "bieuMauTaiSanRepository", bieuMauTaiSanRepository);
    }

    private final CongTacVien ctvB = CongTacVien.builder().id(154).hoTen("CTV B").build();

    private DonDatDichVu don() {
        DonDatDichVu d = DonDatDichVu.builder().id(364).maDonDat("DD-1").trangThai("DangThucHien")
                .thanhTien(BigDecimal.valueOf(837000)).build();
        d.getChiTietList().add(ChiTietDonDat.builder().id(314).thanhTien(BigDecimal.valueOf(270000)).build());
        d.getChiTietList().add(ChiTietDonDat.builder().id(315).thanhTien(BigDecimal.valueOf(660000)).build());
        return d;
    }

    @Test
    void viChiCongPhanDichVuCuaCtv() {
        DonDatDichVu d = don();
        PhanCongCTV pc = PhanCongCTV.builder().id(1).trangThai("HoanThanh").congTacVien(ctvB).donDat(d)
                .chiTietDonDat(d.getChiTietList().get(0)).build();
        when(phanCongCTVRepository.findByCongTacVien_IdOrderByThoiGianPhanCongDesc(154)).thenReturn(List.of(pc));

        // 80% của 243.000đ (phần dịch vụ 270.000đ sau khuyến mãi), không phải 80% của cả đơn 837.000đ
        assertEquals(0, BigDecimal.valueOf(194400).compareTo(service.tinhSoDuViCtv(154)));
    }

    @Test
    void hoanThanhKhongCoAnhVanDuocKhongLuuBieuMau() {
        DonDatDichVu d = don();
        PhanCongCTV pc = PhanCongCTV.builder().id(2).trangThai("DangThucHien").congTacVien(ctvB).donDat(d)
                .chiTietDonDat(d.getChiTietList().get(1)).build();
        when(phanCongCTVRepository.findByIdForUpdate(2)).thenReturn(Optional.of(pc));
        // Không có lịch làm việc: vẫn hoàn thành, ảnh tài sản không bắt buộc
        Map<String, Object> kq = service.completeAssignment(2, new AssignmentActionRequest());

        assertEquals("HoanThanh", pc.getTrangThai());
        assertNotNull(kq);
        verify(bieuMauTaiSanRepository, never()).save(any());
    }

    @Test
    void bamHoanThanhLanNuaThiBaoDaHoanThanh() {
        DonDatDichVu d = don();
        PhanCongCTV pc = PhanCongCTV.builder().id(3).trangThai("HoanThanh").congTacVien(ctvB).donDat(d)
                .chiTietDonDat(d.getChiTietList().get(1)).build();
        when(phanCongCTVRepository.findByIdForUpdate(3)).thenReturn(Optional.of(pc));
        when(lichLamViecRepository.findByPhanCong_Id(3)).thenReturn(List.of(
                LichLamViec.builder().id(1).trangThai("HoanThanh").congTacVien(ctvB).build()));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.completeAssignment(3, new AssignmentActionRequest()));
        assertTrue(e.getMessage().contains("đã hoàn thành"));
        verify(phanCongCTVRepository, never()).save(any());
        verify(bieuMauTaiSanRepository, never()).save(any());
    }
}
