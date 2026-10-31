package com.example.DTO.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/** Giấy tờ đăng ký CTV gửi SAU KHI hồ sơ đã lưu thành công (file vừa tải lên MinIO). */
@Data
public class GuiGiayToCtvRequest {
    @NotBlank(message = "Thiếu mã gửi giấy tờ")
    private String maGuiGiayTo;

    private List<CollaboratorRegisterRequest.HoSoItem> danhSachHoSo;
    private List<CollaboratorRegisterRequest.ChungChiItem> danhSachChungChi;
}
