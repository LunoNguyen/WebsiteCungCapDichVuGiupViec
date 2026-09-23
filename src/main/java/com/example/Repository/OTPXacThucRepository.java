package com.example.Repository;

import com.example.Model.OTPXacThuc;
import com.example.Model.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPXacThucRepository extends JpaRepository<OTPXacThuc, Integer> {
    Optional<OTPXacThuc> findTopByTaiKhoanAndMucDichAndDaSuDungFalseOrderByThoiGianTaoDesc(TaiKhoan taiKhoan, String mucDich);
    Optional<OTPXacThuc> findTopByTaiKhoan_IdAndMucDichAndDaSuDungFalseOrderByThoiGianTaoDesc(Integer taiKhoanId, String mucDich);
}
