package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.BieuMauTaiSan;
import com.example.Model.ChiTietDonDat;
import com.example.Model.CongTacVien;
import com.example.Model.DonDatDichVu;
import com.example.Model.PhanCongCTV;
import com.example.Repository.BieuMauTaiSanRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** Đơn nhiều dịch vụ: CTV chỉ nhận tiền công phần dịch vụ mình làm, và phải tự chụp ảnh trước / sau. */
@ExtendWith(MockitoExtension.class)
class ThuNhapVaAnhTheoCtvTest {

    @Mock PhanCongCTVRepository phanCongCTVRepository;
    @Mock BieuMauTaiSanRepository bieuMauTaiSanRepository;
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
    void ctvChuaTuChupAnhSauThiChuaHoanThanhDuDongNghiepDaChup() {
        DonDatDichVu d = don();
        PhanCongCTV pc = PhanCongCTV.builder().id(2).trangThai("DangThucHien").congTacVien(ctvB).donDat(d)
                .chiTietDonDat(d.getChiTietList().get(1)).build();
        when(phanCongCTVRepository.findByIdForUpdate(2)).thenReturn(Optional.of(pc));
        // CTV A (dịch vụ còn lại) đã gửi ảnh sau dịch vụ của đơn
        lenient().when(bieuMauTaiSanRepository.findByDonDat_IdAndLoaiBieuMauOrderByThoiGianGhiNhanDesc(364, "SauDichVu"))
                .thenReturn(List.of(BieuMauTaiSan.builder().id(9).loaiBieuMau("SauDichVu").build()));
        when(bieuMauTaiSanRepository.existsByDonDat_IdAndCongTacVien_IdAndLoaiBieuMau(364, 154, "SauDichVu")).thenReturn(false);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.completeAssignment(2, new AssignmentActionRequest()));
        assertTrue(e.getMessage().contains("ảnh"));
    }
}
