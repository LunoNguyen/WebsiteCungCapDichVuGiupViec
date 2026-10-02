package com.example.Repository;

import com.example.Model.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Integer> {
    List<LichLamViec> findByCongTacVien_IdOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId);
    List<LichLamViec> findByCongTacVien_IdAndTrangThaiOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId, String trangThai);
    List<LichLamViec> findByCongTacVien_IdAndNgayLamBetweenOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId, LocalDate from, LocalDate to);
    List<LichLamViec> findByPhanCong_DonDat_Id(Integer donDatId);

    @Query("SELECT DISTINCT l FROM LichLamViec l "
         + "JOIN FETCH l.congTacVien ctv "
         + "LEFT JOIN FETCH l.phanCong pc "
         + "LEFT JOIN FETCH pc.donDat dd "
         + "LEFT JOIN FETCH dd.dichVu dv "
         + "LEFT JOIN FETCH dd.khachHang kh "
         + "LEFT JOIN FETCH dd.diaChi dc "
         + "ORDER BY l.ngayLam ASC, l.gioBatDau ASC")
    List<LichLamViec> findAllWithDetails();

    @Query("SELECT DISTINCT l FROM LichLamViec l "
         + "JOIN FETCH l.congTacVien ctv "
         + "LEFT JOIN FETCH l.phanCong pc "
         + "LEFT JOIN FETCH pc.donDat dd "
         + "LEFT JOIN FETCH dd.dichVu dv "
         + "LEFT JOIN FETCH dd.khachHang kh "
         + "LEFT JOIN FETCH dd.diaChi dc "
         + "WHERE ctv.id = :ctvId "
         + "ORDER BY l.ngayLam ASC, l.gioBatDau ASC")
    List<LichLamViec> findByCongTacVienIdWithDetails(@org.springframework.data.repository.query.Param("ctvId") Integer ctvId);
}
