package com.example.Repository;

import com.example.Model.BienLai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BienLaiRepository extends JpaRepository<BienLai, Integer> {
    Optional<BienLai> findByHoaDon_Id(Integer hoaDonId);
}