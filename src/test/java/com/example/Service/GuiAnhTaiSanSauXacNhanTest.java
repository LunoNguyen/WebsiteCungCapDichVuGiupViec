package com.example.Service;

import com.example.DTO.request.AssignmentActionRequest;
import com.example.Model.CongTacVien;
import com.example.Model.DonDatDichVu;
import com.example.Model.LichLamViec;
import com.example.Model.PhanCongCTV;
import com.example.Repository.BieuMauTaiSanRepository;
import com.example.Repository.ChiTietTaiSanRepository;
import com.example.Repository.LichLamViecRepository;
import com.example.Repository.PhanCongCTVRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** App chỉ tải ảnh tài sản lên MinIO sau khi bắt đầu / hoàn thành thành công, rồi gắn object key vào phân công. */
@ExtendWith(MockitoExtension.class)
class GuiAnhTaiSanSauXacNhanTest {

    @Mock PhanCongCTVRepository phanCongCTVRepository;
    @Mock BieuMauTaiSanRepository bieuMauTaiSanRepository;
    @Mock ChiTietTaiSanRepository chiTietTaiSanRepository;
    @Mock LichLamViecRepository lichLamViecRepository;

    @InjectMocks CollaboratorApiService service;

    private CollaboratorApiService ctvService() {
        // bieuMau/chiTietTaiSan là trường @Autowired (không qua constructor)
        ReflectionTestUtils.setField(service, "bieuMauTaiSanRepository", bieuMauTaiSanRepository);
        ReflectionTestUtils.setField(service, "chiTietTaiSanRepository", chiTietTaiSanRepository);
        return service;
    }

    private PhanCongCTV phanCong(String trangThai) {
        DonDatDichVu don = DonDatDichVu.builder().id(364).maDonDat("DD-1").build();
        return PhanCongCTV.builder().id(7).trangThai(trangThai).donDat(don)
                .congTacVien(CongTacVien.builder().id(104).build()).build();
    }

    private AssignmentActionRequest coAnh() {
        AssignmentActionRequest r = new AssignmentActionRequest();
        r.setDanhSachHinhAnh(List.of("don-dat/tai-san/a.jpg"));
        return r;
    }

    @Test
    void anhTruocGanVaoPhanCongDaBatDau() {
        when(phanCongCTVRepository.findById(7)).thenReturn(Optional.of(phanCong("DangThucHien")));
        when(bieuMauTaiSanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> kq = ctvService().guiAnhTaiSan(7, "TruocDichVu", coAnh());

        assertEquals("TruocDichVu", kq.get("loaiBieuMau"));
        verify(bieuMauTaiSanRepository).save(any());
    }

    @Test
    void phanCongVanDaXacNhanNhungCaDangLamThiNhanAnh() {
        // Bắt đầu ca chỉ đổi trạng thái đơn và buổi làm, phân công vẫn DaXacNhan
        when(phanCongCTVRepository.findById(7)).thenReturn(Optional.of(phanCong("DaXacNhan")));
        when(lichLamViecRepository.findByPhanCong_Id(7)).thenReturn(List.of(
                LichLamViec.builder().id(1).trangThai("DangThucHien").build()));
        when(bieuMauTaiSanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ctvService().guiAnhTaiSan(7, "TruocDichVu", coAnh());

        verify(bieuMauTaiSanRepository).save(any());
    }

    @Test
    void chuaBatDauThiKhongNhanAnh() {
        when(phanCongCTVRepository.findById(7)).thenReturn(Optional.of(phanCong("DaXacNhan")));

        assertThrows(IllegalArgumentException.class, () -> ctvService().guiAnhTaiSan(7, "TruocDichVu", coAnh()));
        verify(bieuMauTaiSanRepository, never()).save(any());
    }

    @Test
    void loaiAnhSaiThiBaoLoi() {
        assertThrows(IllegalArgumentException.class, () -> ctvService().guiAnhTaiSan(7, "Khac", coAnh()));
    }
}
