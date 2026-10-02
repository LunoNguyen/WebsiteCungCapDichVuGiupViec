package com.example.Repository;

import com.example.Model.DonDatDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonDatDichVuRepository extends JpaRepository<DonDatDichVu, Integer> {
    List<DonDatDichVu> findByKhachHang_IdOrderByNgayTaoDesc(Integer khachHangId);
    List<DonDatDichVu> findByKhachHang_IdAndTrangThaiOrderByNgayTaoDesc(Integer khachHangId, String trangThai);
    Optional<DonDatDichVu> findByMaDonDat(String maDonDat);

    /**
     * Eager load khachHang, diaChi và danh sách dịch vụ (chiTietList -> dichVu)
     * để tránh LazyInitializationException trong Thymeleaf.
     * DISTINCT để tránh bản ghi trùng do JOIN collection.
     */
    @Query("SELECT DISTINCT d FROM DonDatDichVu d "
         + "LEFT JOIN FETCH d.khachHang "
         + "LEFT JOIN FETCH d.diaChi "
         + "LEFT JOIN FETCH d.chiTietList ct "
         + "LEFT JOIN FETCH ct.dichVu "
         + "ORDER BY d.id DESC")
    List<DonDatDichVu> findAllWithDetails();

    /** Lấy 1 đơn kèm đầy đủ dịch vụ (dùng cho trang chi tiết) */
    @Query("SELECT DISTINCT d FROM DonDatDichVu d "
         + "LEFT JOIN FETCH d.khachHang "
         + "LEFT JOIN FETCH d.diaChi "
         + "LEFT JOIN FETCH d.chiTietList ct "
         + "LEFT JOIN FETCH ct.dichVu "
         + "WHERE d.id = :id")
    Optional<DonDatDichVu> findByIdWithDetails(@Param("id") Integer id);
}