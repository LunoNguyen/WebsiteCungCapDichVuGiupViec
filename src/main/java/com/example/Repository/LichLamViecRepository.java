package com.example.Repository;

import com.example.Model.LichLamViec;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LichLamViecRepository extends JpaRepository<LichLamViec, Integer> {
    List<LichLamViec> findByCongTacVien_IdOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId);
    List<LichLamViec> findByCongTacVien_IdAndTrangThaiOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId, String trangThai);
    List<LichLamViec> findByCongTacVien_IdAndNgayLamBetweenOrderByNgayLamAscGioBatDauAsc(Integer congTacVienId, LocalDate from, LocalDate to);
    List<LichLamViec> findByPhanCong_DonDat_Id(Integer donDatId);
}
