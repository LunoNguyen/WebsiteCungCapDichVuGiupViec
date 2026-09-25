package com.example.Repository;

import com.example.Model.DonDatDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonDatDichVuRepository extends JpaRepository<DonDatDichVu, Integer> {
    List<DonDatDichVu> findByKhachHang_IdOrderByNgayTaoDesc(Integer khachHangId);
    List<DonDatDichVu> findByKhachHang_IdAndTrangThaiOrderByNgayTaoDesc(Integer khachHangId, String trangThai);
    Optional<DonDatDichVu> findByMaDonDat(String maDonDat);

    /**
     * Eager load khachHang và dichVu để tránh LazyInitializationException trong Thymeleaf.
     * Dùng DISTINCT để tránh bản ghi trùng lặp do JOIN.
     */
    @Query("SELECT DISTINCT d FROM DonDatDichVu d "
         + "LEFT JOIN FETCH d.khachHang "
         + "LEFT JOIN FETCH d.dichVu "
         + "LEFT JOIN FETCH d.diaChi "
         + "ORDER BY d.id DESC")
    List<DonDatDichVu> findAllWithDetails();
}
