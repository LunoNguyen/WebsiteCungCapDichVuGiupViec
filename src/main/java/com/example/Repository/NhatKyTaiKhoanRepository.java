package com.example.Repository;

import com.example.Model.NhatKyTaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface NhatKyTaiKhoanRepository extends JpaRepository<NhatKyTaiKhoan, Integer> {
    List<NhatKyTaiKhoan> findTop5ByOrderByThoiGianDesc();
    void deleteByTaiKhoan(com.example.Model.TaiKhoan taiKhoan);
}
