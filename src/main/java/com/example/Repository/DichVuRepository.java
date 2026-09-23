package com.example.Repository;

import com.example.Model.DichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DichVuRepository extends JpaRepository<DichVu, Integer> {
    List<DichVu> findByTrangThai(String trangThai);
    List<DichVu> findByLoaiDichVu_IdAndTrangThai(Integer loaiDichVuId, String trangThai);
}
