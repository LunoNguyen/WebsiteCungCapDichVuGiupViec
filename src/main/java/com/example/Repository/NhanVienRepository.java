package com.example.Repository;

import com.example.Model.NhanVien;
import com.example.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NhanVienRepository extends JpaRepository<NhanVien, Integer> {
    Optional<NhanVien> findByTaiKhoan(TaiKhoan taiKhoan);
    Optional<NhanVien> findByTaiKhoan_Id(Integer taiKhoanId);
    Optional<NhanVien> findByEmailIgnoreCase(String email);
}

