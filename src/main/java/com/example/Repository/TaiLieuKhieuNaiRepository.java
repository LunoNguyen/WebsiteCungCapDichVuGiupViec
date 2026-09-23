package com.example.Repository;

import com.example.Model.TaiLieuKhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaiLieuKhieuNaiRepository extends JpaRepository<TaiLieuKhieuNai, Integer> {
    List<TaiLieuKhieuNai> findByKhieuNai_Id(Integer khieuNaiId);
}
