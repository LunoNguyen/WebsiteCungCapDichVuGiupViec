package com.example.Repository;

import com.example.Model.PhanCongCTV;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhanCongCTVRepository extends JpaRepository<PhanCongCTV, Integer> {
    List<PhanCongCTV> findByCongTacVien_IdOrderByThoiGianPhanCongDesc(Integer congTacVienId);
    List<PhanCongCTV> findByCongTacVien_IdAndTrangThaiOrderByThoiGianPhanCongDesc(Integer congTacVienId, String trangThai);
    Optional<PhanCongCTV> findByDonDat_IdAndCongTacVien_Id(Integer donDatId, Integer congTacVienId);
    Optional<PhanCongCTV> findByDonDat_Id(Integer donDatId);
}
