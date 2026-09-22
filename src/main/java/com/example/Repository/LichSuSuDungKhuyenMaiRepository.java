package com.example.Repository;

import com.example.Model.LichSuSuDungKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuSuDungKhuyenMaiRepository extends JpaRepository<LichSuSuDungKhuyenMai, Integer> {
    
}