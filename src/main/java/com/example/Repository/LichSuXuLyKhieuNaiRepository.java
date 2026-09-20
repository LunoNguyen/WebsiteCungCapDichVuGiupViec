package com.example.Repository;

import com.example.Model.LichSuXuLyKhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuXuLyKhieuNaiRepository extends JpaRepository<LichSuXuLyKhieuNai, Integer> { }
