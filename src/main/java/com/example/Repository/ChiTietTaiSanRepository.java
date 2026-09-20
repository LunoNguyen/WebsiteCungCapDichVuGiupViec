package com.example.Repository;

import com.example.Model.ChiTietTaiSan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietTaiSanRepository extends JpaRepository<ChiTietTaiSan, Integer> { }
