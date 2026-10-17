package com.example.Repository;

import com.example.Model.LichSuSuDungKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuSuDungKhuyenMaiRepository extends JpaRepository<LichSuSuDungKhuyenMai, Integer> {

    /** Các lần dùng mã khuyến mãi của một đơn (để trả lại lượt khi đơn bị hủy). */
    java.util.List<LichSuSuDungKhuyenMai> findByDonDat_Id(Integer donDatId);
}