package com.example.Repository;

import com.example.Model.LichSuTrangThaiDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LichSuTrangThaiDonRepository extends JpaRepository<LichSuTrangThaiDon, Integer> {
    /** Lần chuyển sang trạng thái này gần nhất của đơn (VD lấy người hủy và lý do hủy). */
    Optional<LichSuTrangThaiDon> findFirstByDonDat_IdAndTrangThaiMoiOrderByThoiGianDescIdDesc(Integer donDatId, String trangThaiMoi);
}
