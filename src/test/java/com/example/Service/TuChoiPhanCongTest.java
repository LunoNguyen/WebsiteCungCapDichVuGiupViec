package com.example.Service;

import com.example.Model.CongTacVien;
import com.example.Model.DonDatDichVu;
import com.example.Model.LichLamViec;
import com.example.Model.PhanCongCTV;
import com.example.Repository.DonDatDichVuRepository;
import com.example.Repository.LichLamViecRepository;
import com.example.Repository.LichSuTrangThaiDonRepository;
import com.example.Repository.PhanCongCTVRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** CTV từ chối / hủy nhận đơn: các ca chưa làm của phân công đó phải được hủy để không chặn lịch CTV. */
@ExtendWith(MockitoExtension.class)
class TuChoiPhanCongTest {

    @Mock PhanCongCTVRepository phanCongCTVRepository;
    @Mock DonDatDichVuRepository donDatDichVuRepository;
    @Mock LichLamViecRepository lichLamViecRepository;
    @Mock LichSuTrangThaiDonRepository lichSuTrangThaiDonRepository;
    @InjectMocks CollaboratorApiService service;

    @Test
    void tuChoiThiHuyCaSapToiGiuCaDaXong() {
        PhanCongCTV pc = PhanCongCTV.builder().id(155).trangThai("DaXacNhan")
                .congTacVien(CongTacVien.builder().id(154).hoTen("Nguyễn Văn C").build())
                .donDat(DonDatDichVu.builder().id(313).maDonDat("DD-1").trangThai("DaXacNhan").build())
                .build();
        LichLamViec sapToi = LichLamViec.builder().id(1).phanCong(pc).trangThai("SapToi")
                .ngayLam(LocalDate.of(2026, 10, 27)).gioBatDau(LocalTime.of(9, 0)).build();
        LichLamViec daXong = LichLamViec.builder().id(2).phanCong(pc).trangThai("HoanThanh")
                .ngayLam(LocalDate.of(2026, 10, 20)).gioBatDau(LocalTime.of(9, 0)).build();
        when(phanCongCTVRepository.findByIdForUpdate(155)).thenReturn(Optional.of(pc));
        when(lichLamViecRepository.findByPhanCong_Id(155)).thenReturn(List.of(sapToi, daXong));

        service.rejectAssignment(155, null);

        assertEquals("TuChoi", pc.getTrangThai());
        assertEquals("Huy", sapToi.getTrangThai());
        assertEquals("HoanThanh", daXong.getTrangThai());
    }
}
