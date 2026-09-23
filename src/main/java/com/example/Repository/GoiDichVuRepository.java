package com.example.Repository;

import com.example.Model.GoiDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GoiDichVuRepository extends JpaRepository<GoiDichVu, Integer> {
    List<GoiDichVu> findByDichVu_IdAndTrangThai(Integer dichVuId, String trangThai);
}
