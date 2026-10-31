package com.example.Service;

import com.example.DTO.request.CollaboratorRegisterRequest;
import com.example.Model.CongTacVien;
import com.example.Repository.CongTacVienRepository;
import com.example.Repository.HoSoCTVRepository;
import org.junit.jupiter.api.BeforeEach;
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

/** Web chỉ tải giấy tờ đăng ký CTV lên MinIO sau khi gửi hồ sơ thành công, rồi gắn vào hồ sơ bằng mã một lần. */
@ExtendWith(MockitoExtension.class)
class GuiGiayToCtvTest {

    @Mock CongTacVienRepository congTacVienRepository;
    @Mock HoSoCTVRepository hoSoCTVRepository;
    @Mock MaGuiGiayToStore maGuiGiayToStore;
    @Mock MinioService minioService;
    @InjectMocks CustomerApiService service;

    @BeforeEach
    void ganTruong() {
        // maGuiGiayToStore, minioService là trường @Autowired (không qua constructor)
        ReflectionTestUtils.setField(service, "maGuiGiayToStore", maGuiGiayToStore);
        ReflectionTestUtils.setField(service, "minioService", minioService);
    }

    @Test
    void maHopLeThiLuuGiayToVaXoaMa() {
        when(maGuiGiayToStore.layCtvId("abc")).thenReturn(Optional.of(104));
        when(congTacVienRepository.findById(104)).thenReturn(Optional.of(CongTacVien.builder().id(104).build()));
        when(minioService.toStoredValue("ctv/ho-so/cccd.jpg")).thenReturn("ctv/ho-so/cccd.jpg");
        CollaboratorRegisterRequest.HoSoItem hs = new CollaboratorRegisterRequest.HoSoItem();
        hs.setLoaiTaiLieu("CCCD_Mat_Truoc");
        hs.setDuongDanFile("ctv/ho-so/cccd.jpg");

        Map<String, Object> kq = service.guiGiayToDangKy("abc", List.of(hs), null);

        assertEquals(1, kq.get("soFile"));
        verify(hoSoCTVRepository).save(any());
        verify(maGuiGiayToStore).xoa("abc");
    }

    @Test
    void maHetHanThiKhongLuuGi() {
        when(maGuiGiayToStore.layCtvId("cu")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.guiGiayToDangKy("cu", List.of(), null));
        verify(hoSoCTVRepository, never()).save(any());
        verify(maGuiGiayToStore, never()).xoa(any());
    }
}
