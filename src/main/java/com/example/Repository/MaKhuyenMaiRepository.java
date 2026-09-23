package com.example.Repository;

import com.example.Model.MaKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaKhuyenMaiRepository extends JpaRepository<MaKhuyenMai, Integer> {
    Optional<MaKhuyenMai> findByCodeKhuyenMai(String codeKhuyenMai);
    Optional<MaKhuyenMai> findByCodeKhuyenMaiIgnoreCase(String codeKhuyenMai);
}