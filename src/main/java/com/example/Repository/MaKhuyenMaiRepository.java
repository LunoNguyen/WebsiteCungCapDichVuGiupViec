package com.example.Repository;

import com.example.Model.MaKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MaKhuyenMaiRepository extends JpaRepository<MaKhuyenMai, Integer> {
    
}