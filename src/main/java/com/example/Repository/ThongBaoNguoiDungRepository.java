package com.example.Repository;

import com.example.Model.ThongBaoNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongBaoNguoiDungRepository extends JpaRepository<ThongBaoNguoiDung, Integer> {
    List<ThongBaoNguoiDung> findByTaiKhoan_IdOrderByThongBao_ThoiGianGuiDesc(Integer taiKhoanId);
    List<ThongBaoNguoiDung> findByTaiKhoan_IdAndDaDocFalseOrderByThongBao_ThoiGianGuiDesc(Integer taiKhoanId);

    List<ThongBaoNguoiDung> findByThongBao_Id(Integer thongBaoId);

    void deleteByThongBao_Id(Integer thongBaoId);

    /** Mỗi dòng: [thongBaoId, số người nhận, số người đã đọc]. */
    @org.springframework.data.jpa.repository.Query("SELECT t.thongBao.id, COUNT(t), SUM(CASE WHEN t.daDoc = true THEN 1 ELSE 0 END) "
         + "FROM ThongBaoNguoiDung t GROUP BY t.thongBao.id")
    List<Object[]> thongKeNguoiNhan();

    /**
     * Phát thông báo cho mọi khách hàng đang hoạt động bằng MỘT câu INSERT ... SELECT (nguyên tử, nhanh).
     * NOT EXISTS giúp chạy lại không tạo dòng trùng.
     */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value =
           "INSERT INTO ThongBaoNguoiDung (thongBaoId, taiKhoanId, DaDoc, TrangThai) "
         + "SELECT :thongBaoId, kh.taiKhoanId, FALSE, 'DaGui' FROM KhachHang kh "
         + "WHERE kh.taiKhoanId IS NOT NULL AND kh.TrangThai = 'HoatDong' "
         + "AND NOT EXISTS (SELECT 1 FROM ThongBaoNguoiDung x WHERE x.thongBaoId = :thongBaoId AND x.taiKhoanId = kh.taiKhoanId)")
    int phatChoKhachHang(@org.springframework.data.repository.query.Param("thongBaoId") Integer thongBaoId);

    /** Phát thông báo cho mọi cộng tác viên đang hoạt động. */
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value =
           "INSERT INTO ThongBaoNguoiDung (thongBaoId, taiKhoanId, DaDoc, TrangThai) "
         + "SELECT :thongBaoId, ctv.taiKhoanId, FALSE, 'DaGui' FROM CongTacVien ctv "
         + "WHERE ctv.taiKhoanId IS NOT NULL AND ctv.TrangThai = 'HoatDong' "
         + "AND NOT EXISTS (SELECT 1 FROM ThongBaoNguoiDung x WHERE x.thongBaoId = :thongBaoId AND x.taiKhoanId = ctv.taiKhoanId)")
    int phatChoCongTacVien(@org.springframework.data.repository.query.Param("thongBaoId") Integer thongBaoId);
}
