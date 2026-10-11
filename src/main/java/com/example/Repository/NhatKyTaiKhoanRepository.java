package com.example.Repository;

import com.example.Model.NhatKyTaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface NhatKyTaiKhoanRepository extends JpaRepository<NhatKyTaiKhoan, Integer> {
    List<NhatKyTaiKhoan> findTop5ByOrderByThoiGianDesc();
    void deleteByTaiKhoan(com.example.Model.TaiKhoan taiKhoan);

    boolean existsByTaiKhoan_IdAndHanhDong(Integer taiKhoanId, String hanhDong);

    /** Id các tài khoản đã có một hành động (VD: HCNS đã xem hồ sơ ứng tuyển). */
    @org.springframework.data.jpa.repository.Query(
            "select distinct n.taiKhoan.id from NhatKyTaiKhoan n where n.hanhDong = :hanhDong")
    List<Integer> taiKhoanDaCoHanhDong(@org.springframework.data.repository.query.Param("hanhDong") String hanhDong);

    List<NhatKyTaiKhoan> findByTaiKhoan_IdOrderByThoiGianDesc(Integer taiKhoanId);
}
