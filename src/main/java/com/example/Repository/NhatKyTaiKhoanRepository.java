package com.example.Repository;

import com.example.Model.NhatKyTaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NhatKyTaiKhoanRepository extends JpaRepository<NhatKyTaiKhoan, Integer> { }
