package com.example.Repository;

import com.example.Model.DichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DichVuRepository extends JpaRepository<DichVu, Integer> {
    List<DichVu> findByTrangThai(String trangThai);

    /** Các biến thể của một dịch vụ (cùng mã, VD DV101: 2 giờ, 3 giờ, 4 giờ, gói tháng). */
    List<DichVu> findByMaDichVuAndTrangThai(String maDichVu, String trangThai);
    List<DichVu> findByLoaiDichVu_IdAndTrangThai(Integer loaiDichVuId, String trangThai);
}
