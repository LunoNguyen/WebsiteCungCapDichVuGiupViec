package com.example.Repository;

import com.example.Model.CongTacVien;
import com.example.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CongTacVienRepository extends JpaRepository<CongTacVien, Integer> {
    Optional<CongTacVien> findByTaiKhoan(TaiKhoan taiKhoan);
    Optional<CongTacVien> findByTaiKhoan_Id(Integer taiKhoanId);
}

