package com.example.Repository;

import com.example.Model.LichSuTrangThaiDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuTrangThaiDonRepository extends JpaRepository<LichSuTrangThaiDon, Integer> { }
