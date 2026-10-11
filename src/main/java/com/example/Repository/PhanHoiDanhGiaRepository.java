package com.example.Repository;

import com.example.Model.PhanHoiDanhGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhanHoiDanhGiaRepository extends JpaRepository<PhanHoiDanhGia, Integer> {
    /** Trao đổi dưới một đánh giá, cũ trước mới sau. */
    List<PhanHoiDanhGia> findByDanhGia_IdOrderByThoiGianAscIdAsc(Integer danhGiaId);
}
