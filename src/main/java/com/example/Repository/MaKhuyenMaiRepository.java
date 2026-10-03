package com.example.Repository;

import com.example.Model.MaKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaKhuyenMaiRepository extends JpaRepository<MaKhuyenMai, Integer> {
    Optional<MaKhuyenMai> findByCodeKhuyenMai(String codeKhuyenMai);
    Optional<MaKhuyenMai> findByCodeKhuyenMaiIgnoreCase(String codeKhuyenMai);

    /**
     * Tăng số lượt đã dùng bằng MỘT câu UPDATE có điều kiện, nên hai khách dùng cùng lúc lượt cuối cùng
     * thì chỉ một người thành công (không bị "mất lượt" hay vượt quá SoLuotToiDa).
     *
     * @return 1 nếu ghi nhận được lượt dùng, 0 nếu mã đã hết lượt hoặc không còn hoạt động
     */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE MaKhuyenMai m SET m.soLuotDaDung = m.soLuotDaDung + 1, "
         + "m.trangThai = CASE WHEN m.soLuotDaDung + 1 >= m.soLuotToiDa THEN 'HetLuot' ELSE m.trangThai END "
         + "WHERE m.id = :id AND m.trangThai = 'HoatDong' AND m.soLuotDaDung < m.soLuotToiDa")
    int ghiNhanLuotDung(@org.springframework.data.repository.query.Param("id") Integer id);
}
