package com.example.Service;

import com.example.Model.CongTacVien;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.DonDatDichVuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/** CTV bấm "Bỏ qua" đơn trong pool: máy chủ ghi nhận để đơn không hiện lại với CTV đó. */
@ExtendWith(MockitoExtension.class)
class BoQuaDonTest {

    @Mock CongTacVienRepository congTacVienRepository;
    @Mock DonDatDichVuRepository donDatDichVuRepository;
    @Mock BoQuaDonStore boQuaDonStore;
    @InjectMocks CollaboratorApiService service;

    @BeforeEach
    void ganStore() {
        // boQuaDonStore là trường @Autowired (không qua constructor)
        ReflectionTestUtils.setField(service, "boQuaDonStore", boQuaDonStore);
    }

    @Test
    void boQuaLuuTheoIdCtv() {
        when(congTacVienRepository.findById(104)).thenReturn(Optional.of(CongTacVien.builder().id(104).build()));
        when(donDatDichVuRepository.existsById(516)).thenReturn(true);

        service.rejectOrderFromPool(104, 516, "Xa quá");

        verify(boQuaDonStore).boQua(104, 516, "Xa quá");
    }

    @Test
    void appGuiIdTaiKhoanThiQuyVeIdCtv() {
        when(congTacVienRepository.findById(157)).thenReturn(Optional.empty());
        when(congTacVienRepository.findByTaiKhoan_Id(157)).thenReturn(Optional.of(CongTacVien.builder().id(104).build()));
        when(donDatDichVuRepository.existsById(516)).thenReturn(true);

        service.rejectOrderFromPool(157, 516, "");

        verify(boQuaDonStore).boQua(104, 516, "");
    }

    @Test
    void donKhongTonTaiThiBaoLoiVaKhongLuu() {
        when(congTacVienRepository.findById(104)).thenReturn(Optional.of(CongTacVien.builder().id(104).build()));
        when(donDatDichVuRepository.existsById(9999)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.rejectOrderFromPool(104, 9999, "x"));
        verify(boQuaDonStore, never()).boQua(anyInt(), anyInt(), any());
    }
}
