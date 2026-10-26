package com.example.Repository;

import com.example.Model.BieuMauTaiSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BieuMauTaiSanRepository extends JpaRepository<BieuMauTaiSan, Integer> {
    List<BieuMauTaiSan> findByDonDat_IdOrderByThoiGianGhiNhanAsc(Integer donDatId);

    List<BieuMauTaiSan> findByDonDat_IdAndLoaiBieuMauOrderByThoiGianGhiNhanDesc(Integer donDatId, String loaiBieuMau);

    /** CTV này đã tự chụp ảnh tài sản (TruocDichVu / SauDichVu) cho đơn chưa – đơn nhiều dịch vụ mỗi CTV chụp riêng. */
    boolean existsByDonDat_IdAndCongTacVien_IdAndLoaiBieuMau(Integer donDatId, Integer congTacVienId, String loaiBieuMau);
}
