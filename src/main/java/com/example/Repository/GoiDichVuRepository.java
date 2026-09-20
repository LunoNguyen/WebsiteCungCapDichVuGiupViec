package com.example.Repository;

import com.example.Model.GoiDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoiDichVuRepository extends JpaRepository<GoiDichVu, Integer> { }
