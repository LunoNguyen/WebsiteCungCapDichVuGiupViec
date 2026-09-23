package com.example.Repository;

import com.example.Model.DonDatDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonDatDichVuRepository extends JpaRepository<DonDatDichVu, Integer> {
    List<DonDatDichVu> findByKhachHang_IdOrderByNgayTaoDesc(Integer khachHangId);
    List<DonDatDichVu> findByKhachHang_IdAndTrangThaiOrderByNgayTaoDesc(Integer khachHangId, String trangThai);
    Optional<DonDatDichVu> findByMaDonDat(String maDonDat);
}
