package com.example.DTO;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

/** DTO cho DonDatDichVu. Phản ánh schema mới: 1 đơn chứa nhiều dịch vụ qua ChiTietDonDat. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DonDatDichVuDTO {
    private Integer id;
    private String maDonDat;
    private Integer khachHangId;
    private Integer diaChiId;
    private Integer nhanVienTiepNhanId;

    // Đổi tên trường từ couponId sang khuyenMaiId
    private Integer khuyenMaiId;

    /** Danh sách dịch vụ trong đơn (thay thế cho dichVuId / bangGiaId / goiDichVuId cũ) */
    private List<ChiTietDonDatDTO> chiTietList;

    private String loaiHinhDat;
    private LocalDate ngayThucHien;
    private LocalTime gioBatDau;
    private LocalTime gioKetThuc;
    private String yeuCauDacBiet;
    private BigDecimal chiPhiGoc;
    private BigDecimal soTienGiam;
    private BigDecimal thanhTien;
    private String trangThai;
    private LocalDateTime ngayTao;
    private String ghiChu;
}