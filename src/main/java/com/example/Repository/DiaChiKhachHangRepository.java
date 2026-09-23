package com.example.Repository;

import com.example.Model.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Integer> {
    List<DiaChiKhachHang> findByKhachHang_IdAndTrangThai(Integer khachHangId, String trangThai);
    List<DiaChiKhachHang> findByKhachHang_Id(Integer khachHangId);
}
