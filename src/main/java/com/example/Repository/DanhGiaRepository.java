package com.example.Repository;

import com.example.Model.DanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DanhGiaRepository extends JpaRepository<DanhGia, Integer> {
    Optional<DanhGia> findByDonDat_Id(Integer donDatId);
    List<DanhGia> findByCongTacVien_Id(Integer congTacVienId);
    List<DanhGia> findByDonDat_DichVu_Id(Integer dichVuId);
}
