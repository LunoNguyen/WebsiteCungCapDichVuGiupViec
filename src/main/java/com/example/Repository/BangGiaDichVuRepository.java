package com.example.Repository;

import com.example.Model.BangGiaDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BangGiaDichVuRepository extends JpaRepository<BangGiaDichVu, Integer> {
    List<BangGiaDichVu> findByDichVu_IdAndTrangThai(Integer dichVuId, String trangThai);
    Optional<BangGiaDichVu> findTopByDichVu_IdAndLoaiHinhDatAndTrangThai(Integer dichVuId, String loaiHinhDat, String trangThai);
}
