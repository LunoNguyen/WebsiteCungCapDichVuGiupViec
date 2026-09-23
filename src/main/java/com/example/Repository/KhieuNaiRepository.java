package com.example.Repository;

import com.example.Model.KhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhieuNaiRepository extends JpaRepository<KhieuNai, Integer> {
    List<KhieuNai> findByKhachHang_IdOrderByNgayGuiDesc(Integer khachHangId);
    Optional<KhieuNai> findByDonDat_Id(Integer donDatId);
}
