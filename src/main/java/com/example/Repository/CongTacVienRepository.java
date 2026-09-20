package com.example.Repository;

import com.example.Model.CongTacVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CongTacVienRepository extends JpaRepository<CongTacVien, Integer> { }
