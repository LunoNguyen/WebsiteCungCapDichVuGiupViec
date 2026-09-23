package com.example.Repository;

import com.example.Model.HoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoaDonRepository extends JpaRepository<HoaDon, Integer> {
    Optional<HoaDon> findByDonDat_Id(Integer donDatId);
    Optional<HoaDon> findByMaHoaDon(String maHoaDon);
}
