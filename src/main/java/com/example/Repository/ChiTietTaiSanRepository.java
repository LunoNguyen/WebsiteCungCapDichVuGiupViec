package com.example.Repository;

import com.example.Model.ChiTietTaiSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietTaiSanRepository extends JpaRepository<ChiTietTaiSan, Integer> {
    List<ChiTietTaiSan> findByBieuMau_Id(Integer bieuMauId);

    List<ChiTietTaiSan> findByBieuMau_DonDat_Id(Integer donDatId);
}
