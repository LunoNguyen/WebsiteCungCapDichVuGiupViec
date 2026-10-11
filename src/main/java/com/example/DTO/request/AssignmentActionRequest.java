package com.example.DTO.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class AssignmentActionRequest {
    @JsonAlias({"lyDo", "ly_do", "reason"})
    private String lyDoTuChoi;

    @JsonAlias({"ghiChu", "ghi_chu", "note", "ket_qua"})
    private String ketQuaThucHien;
}
