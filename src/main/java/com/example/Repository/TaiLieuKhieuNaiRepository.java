package com.example.Repository;

import com.example.Model.TaiLieuKhieuNai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaiLieuKhieuNaiRepository extends JpaRepository<TaiLieuKhieuNai, Integer> { }
