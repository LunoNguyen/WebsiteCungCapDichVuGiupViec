package com.example.Repository;

import com.example.Model.ThongBaoNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongBaoNguoiDungRepository extends JpaRepository<ThongBaoNguoiDung, Integer> {
    List<ThongBaoNguoiDung> findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(Integer taiKhoanId);
    List<ThongBaoNguoiDung> findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(Integer taiKhoanId);
}
